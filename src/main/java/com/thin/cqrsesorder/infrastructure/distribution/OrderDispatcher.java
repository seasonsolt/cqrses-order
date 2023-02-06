package com.thin.cqrsesorder.infrastructure.distribution;

import com.thin.cqrsesorder.bean.request.BaseRequest;
import com.thin.cqrsesorder.bean.view.OrderView;
import com.thin.cqrsesorder.infrastructure.Session;
import com.thin.cqrsesorder.infrastructure.exception.NoInstanceException;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * "Dispatcher" layer make sure that specific server handle a particular task, for the lack of "Command
 * Layer(Distributed CommandBus)".
 * The Dispatcher can be treated as a "self load balance" strategy which means there is no need creating
 * external "load balancer". The getHashInt() method of BaseRequest guarantee the randomicity of the service instance.
 *
 */
@Aspect
@Component
@SessionAttributes
@Order(0)
public class OrderDispatcher {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Instance service;

    /**
     * We provide a standAlone switch to support stand-alone mode which can work as usual after a "Split-Brain" or a
     * disconnection from zookeeper.
     */
    @Value("${distribution.instance.stand-alone}")
    boolean standAlone;

    /**
     * Store the response in the session after a successful execution.
     * If the client retry the request(due to a network failure), we send the same response already stored in session.
     */
    @Around("within(com.thin.cqrsesorder.client..*)")
    public Object dispatch(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest servletRequest = (HttpServletRequest) Arrays.stream(joinPoint.getArgs()).filter(t -> t instanceof HttpServletRequest).findFirst().orElse(null);
        BaseRequest request = (BaseRequest) Arrays.stream(joinPoint.getArgs()).filter(t -> t instanceof BaseRequest).findFirst().orElse(null);

        String tickTime = Arrays
                .stream(Optional.ofNullable(servletRequest.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> StringUtils.equals(cookie.getName(), "tickTime"))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        String requestId = request.getRequestId() + tickTime;

        // idempotent process
        if (StringUtils.isNotBlank(requestId)) {
            Session session = new Session(servletRequest.getSession());

            Object orderResponse = session.getResponse(requestId);
            if (null != orderResponse) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) Arrays.stream(joinPoint.getArgs()).filter(t -> t instanceof HttpServletResponse).findFirst().orElse(null);
                String nextTickTime = String.valueOf(((OrderView) orderResponse).getTickTime().getTime());
                httpServletResponse.addCookie(new Cookie("tickTime", nextTickTime));

                return orderResponse;
            }
        }

        if (Objects.isNull(servletRequest) || Objects.isNull(request) || Objects.isNull(request.getHashInt()) || request.isRedirect()) {
            return idempotentProcess(request.getRequestId(), joinPoint);
        } else {
            request.setRedirect(true);
            try {
                String url = getRedirectUrl(servletRequest, request.getHashInt());

                return restTemplate.postForObject(url, request, OrderView.class);
            } catch (NoInstanceException e) {
                if (standAlone) {
                    return idempotentProcess(request.getRequestId(), joinPoint);
                }

                throw new RuntimeException("No instance found while stand-alone mode is off");
            }
        }
    }

    /**
     * Set a session attribute to store responses for the requests.
     */
    private Object idempotentProcess(String requestId, ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest httpServletRequest = (HttpServletRequest) Arrays.stream(joinPoint.getArgs()).filter(t -> t instanceof HttpServletRequest).findFirst().orElse(null);
        HttpServletResponse httpServletResponse = (HttpServletResponse) Arrays.stream(joinPoint.getArgs()).filter(t -> t instanceof HttpServletResponse).findFirst().orElse(null);
        Object orderView = joinPoint.proceed();

//        requestId = requestId + ((OrderView) orderView).getTickTime().getTime();
        httpServletResponse.addCookie(new Cookie("tickTime", String.valueOf(((OrderView) orderView).getTickTime().getTime())));

        Session session = new Session(httpServletRequest.getSession());
        session.addResponse(requestId, orderView);

        return orderView;
    }

    public String getRedirectUrl(HttpServletRequest servletRequest, Integer hashInt) throws NoInstanceException {
        List<String> instances = service.getInstances();

        return servletRequest.getScheme() + "://" + instances.get(hashInt % instances.size()) + servletRequest.getRequestURI();
    }
}
