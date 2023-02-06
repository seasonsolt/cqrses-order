package com.thin.cqrsesorder.bean.request;

import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.UUID;

@Data
@ToString
public class BaseOrderRequest extends BaseRequest {

    @Nullable
    protected String orderId;

    @Override
    @Nullable
    public Integer getHashInt() {
        return StringUtils.isNotBlank(orderId) ? Integer.valueOf(orderId.charAt(orderId.length() - 1)) : null;
    }

    @Override
    public String getRequestId() {
        return UUID.randomUUID().toString();
    }
}
