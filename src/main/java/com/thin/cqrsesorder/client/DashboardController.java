package com.thin.cqrsesorder.client;

import com.alibaba.fastjson.JSONObject;
import com.bitestream.annotation.BiteCache;
import com.bitestream.api.AgreementApi;
import com.bitestream.api.Db;
import com.bitestream.api.account.ActivitiesApi;
import com.bitestream.api.drive.MetaCurrencyApi;
import com.bitestream.api.product.OpportunityApi;
import com.bitestream.api.product.ProductApi;
import com.bitestream.api.product.ProductSubscriptionApi;
import com.bitestream.api.setting.PermissionApi;
import com.bitestream.constant.ContextConstant;
import com.bitestream.enums.DashboardContributionEnum;
import com.bitestream.model.account.request.GetInvestorsRequest;
import com.bitestream.model.account.request.StatisticsActivityRequest;
import com.bitestream.model.account.request.dto.StatisticsLoginGroup;
import com.bitestream.model.account.response.dto.LoginStatistics;
import com.bitestream.model.account.response.dto.ProductData;
import com.bitestream.model.account.response.vo.StatisticsInvestorsDTO;
import com.bitestream.model.account.response.vo.StatisticsSummaryDTO;
import com.bitestream.model.account.response.vo.StatisticsTasksDTO;
import com.bitestream.model.drive.enums.TaskStatusEnum;
import com.bitestream.model.drive.enums.TasksNodeEnum;
import com.bitestream.model.drive.response.GetTenantCurrencyResponse;
import com.bitestream.model.drive.response.vo.MetaCurrencyWithExchangeRateVO;
import com.bitestream.model.drive.root.Tasks;
import com.bitestream.model.product.enums.OpportunityStageEnum;
import com.bitestream.model.product.enums.ProductStatusEnum;
import com.bitestream.model.product.request.GetProductOnlyRequest;
import com.bitestream.model.product.request.ListProductRequest;
import com.bitestream.model.product.request.QueryProductSubscriptionRequest;
import com.bitestream.model.product.response.*;
import com.bitestream.model.product.root.Opportunity;
import com.bitestream.model.product.root.Product;
import com.bitestream.model.product.root.ProductSubscription;
import com.bitestream.model.root.AssetAgreement;
import com.bitestream.model.root.IFAAgreement;
import com.bitestream.model.root.TenantAgreement;
import com.bitestream.model.setting.enums.GpRoleTypeEnum;
import com.bitestream.model.setting.request.QueryTenantGpAccountRoleRequest;
import com.bitestream.service.account.DashBoardService;
import com.bitestream.util.ContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.parseObject;

@RestController
@RequestMapping("/v2/gp/dashboards")
@Api(value = "DashBoard", tags = {"DashBoard"})
@Log4j2
public class DashboardController {

    @Resource
    private PermissionApi permissionApi;
    @Resourcte
    private AgreementApi agreementApi;
    @Resource
    private ProductApi productApi;
    @Resource
    private ProductSubscriptionApi productSubscriptionApi;
    @Resource
    private MetaCurrencyApi metaCurrencyApi;
    @Resource
    private DashBoardService dashBoardService;
    @Resource
    private ActivitiesApi activitiesApi;
    @Resource
    private OpportunityApi opportunityApi;

    @ApiOperation("Get Dashboard summary / Get Dashboard summary")
    @GetMapping("/summary")
    @BiteCache
    public StatisticsSummaryDTO statisticsSummary(@RequestParam(value = "contribution", required = false) List<String> contribution) {
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID, Long.class);
        GetInvestorsRequest request = GetInvestorsRequest.builder().tenantId(tenantId).build();
        return dashBoardService.statisticsSummary(request, ObjectUtils.isEmpty(contribution) ? Arrays.asList() : contribution);
    }

    @ApiOperation("Get Dashboard task data / Get Dashboard task data")
    @GetMapping("/tasks")
    @BiteCache
    public StatisticsTasksDTO statisticsTasks(){
        return dashBoardService.statisticsTasks();
    }

    @ApiOperation("Get Dashboard investor summary / Get Dashboard investor summary")
    @GetMapping("/investors/summary")
    //@BiteCache
    public StatisticsInvestorsDTO statisticsInvestors(@RequestParam(value = "contribution",required = false) List<String> contribution){
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID,Long.class);
        GetInvestorsRequest request = GetInvestorsRequest.builder().tenantId(tenantId).build();
        return dashBoardService.statisticsInvestors(request,ObjectUtils.isEmpty(contribution)? Arrays.asList():contribution);
    }

    /**
     * @return
     */
    @ApiOperation("Statistics for Investor login times")
    @PostMapping("/investors/logins")
    @BiteCache
    public List<LoginStatistics> queryInvestorsLogins(@RequestBody List<StatisticsLoginGroup> groups) {
        return activitiesApi.queryInvestorsLogins(groups);
    }

    @ApiOperation("Statistics for Investor login times")
    @PostMapping("/investors/activities")
    @BiteCache
    public List<ProductData> queryActivityTimesByProduct(@RequestBody StatisticsActivityRequest request) {
        return activitiesApi.queryActivityTimesByProduct(request);
    }


    @GetMapping("/products")
    @BiteCache
    public QueryDashboardProductsResponse queryDashboardProducts() {
        QueryDashboardProductsResponse response = new QueryDashboardProductsResponse();
        Long gpAccountId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_GP_ACCOUNT_ID, Long.class);
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID, Long.class);

        // Check whether the current login user is an IFA
        Set<GpRoleTypeEnum> gpRoleTypeEnums = new HashSet<>();
        gpRoleTypeEnums.add(GpRoleTypeEnum.WEALTH_MANAGER_IFA_RIA);
        boolean checkSalesRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(gpRoleTypeEnums).build());

        if (checkSalesRole) {
            // IFA perspective
            List<AssetAgreement> assetAgreements = Optional.ofNullable(agreementApi.getAssetAgreements(tenantId, gpAccountId, gpAccountId, null, null).getData()).orElse(new ArrayList<>());
            List<Long> productIds = assetAgreements.stream().map(AssetAgreement::getAssetId).toList();
            if (CollectionUtils.isEmpty(productIds)) {
                return response;
            }
            List<Product> products = Optional.ofNullable(productApi.listProduct(ListProductRequest.builder().tenantId(tenantId).productIds(productIds).productStatus(Arrays.asList(ProductStatusEnum.RELEASED, ProductStatusEnum.REMOVE)).build()).getProductList()).orElseGet(ArrayList::new);
            response.setProducts(products);
        } else {
            // GP, WM perspective
            List<Product> gpProducts = Optional.ofNullable(productApi.listProduct(ListProductRequest.builder().tenantId(tenantId).productStatus(Arrays.asList(ProductStatusEnum.RELEASED, ProductStatusEnum.REMOVE)).build()).getProductList()).orElseGet(ArrayList::new);

            List<AssetAgreement> assetAgreements = Optional.ofNullable(agreementApi.getAssetAgreements(null, tenantId, null, null).getData()).orElse(new ArrayList<>());
            List<Long> productIds = assetAgreements.stream().map(AssetAgreement::getAssetId).toList();
            if (!CollectionUtils.isEmpty(productIds)) {
                List<Product> wmProducts = Optional.ofNullable(productApi.listProduct(ListProductRequest.builder().productIds(productIds).productStatus(Arrays.asList(ProductStatusEnum.RELEASED, ProductStatusEnum.REMOVE)).build()).getProductList()).orElseGet(ArrayList::new);
                gpProducts.addAll(wmProducts);
            }
            response.setProducts(gpProducts);
        }
        return response;
    }

    @GetMapping("/contributions")
    public QueryDashboardContributionsResponse queryDashboardContributions() {
        QueryDashboardContributionsResponse response = QueryDashboardContributionsResponse.builder().build();
        Long gpAccountId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_GP_ACCOUNT_ID, Long.class);
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID, Long.class);

        List<DashboardContributionEnum> contributions = new ArrayList<>();
        contributions.add(DashboardContributionEnum.INTERNAL);

        Set<GpRoleTypeEnum> manageRoles = this.dashBoardManageRoles();
        boolean checkManageRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(manageRoles).build());
        if (checkManageRole) {
            // Determine whether there is ifa agreement
            List<IFAAgreement> ifaAgreements = Optional.ofNullable(agreementApi.getIFAAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            if (!ObjectUtils.isEmpty(ifaAgreements)) {
                contributions.add(DashboardContributionEnum.IFA);
            }
            // Determine whether there is wm agreement
            List<TenantAgreement> tenantAgreements = Optional.ofNullable(agreementApi.getTenantAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            if (!ObjectUtils.isEmpty(tenantAgreements)) {
                contributions.add(DashboardContributionEnum.WM);
            }
        }
        response.setContributions(contributions);
        return response;
    }

    @GetMapping("/subscription-current-stage")
    @BiteCache
    public QueryDashboardSubscriptionCurrentStageResponse queryDashboardsSubscriptionCurrentStage(@RequestParam(value = "productId", required = false) Long productId,
                                                                                                  @RequestParam("contributions") List<DashboardContributionEnum> contributions) {
        QueryDashboardSubscriptionCurrentStageResponse response = QueryDashboardSubscriptionCurrentStageResponse.builder().build();
        Long gpAccountId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_GP_ACCOUNT_ID, Long.class);
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID, Long.class);

        // query tenant rate
        GetTenantCurrencyResponse tenantCurrencyResponse = Optional.ofNullable(metaCurrencyApi.getTenantCurrency()).orElseGet(GetTenantCurrencyResponse::new);
        Map<String, BigDecimal> rateMap = tenantCurrencyResponse.getMetaCurrencyWithExchangeRateVOS().stream().collect(Collectors.toMap(MetaCurrencyWithExchangeRateVO::getCurrencyCode, MetaCurrencyWithExchangeRateVO::getRate));
        response.setRate(rateMap);
        String showCurrency = tenantCurrencyResponse.getBaseCurrency();
        if (!ObjectUtils.isEmpty(productId)) {
            Product product = Optional.ofNullable(productApi.getProductOnly(GetProductOnlyRequest.builder().productId(productId).build()).getProduct()).orElseGet(Product::new);
            showCurrency = product.getBaseCurrency();
        }

        // Check whether the current login user is an IFA
        Set<GpRoleTypeEnum> manageRoles = this.dashBoardManageRoles();
        boolean checkManageRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(manageRoles).build());
        Set<GpRoleTypeEnum> ifaRoles = new HashSet<>();
        ifaRoles.add(GpRoleTypeEnum.WEALTH_MANAGER_IFA_RIA);
        boolean checkIfaRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(ifaRoles).build());

        List<ProductSubscription> internalSubscriptions = new ArrayList<>();

        if (!checkManageRole) {
            if (contributions.contains(DashboardContributionEnum.WM) || contributions.contains(DashboardContributionEnum.IFA)) {
                response.setStatus(QueryDashboardSubscriptionCurrentStageResponse.StatusEnum.PARAMS_ERROR);
                return response;
            }
            if (checkIfaRole) {
                // IFA perspective
                internalSubscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).isDistributedIfa(true).salesId(gpAccountId).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            } else {
                // Normal GP perspective
                internalSubscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).salesId(gpAccountId).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            }
            this.subscriptionCurrencyConversion(internalSubscriptions, rateMap, showCurrency);
            response.setInternal(this.subscriptionCurrentStage(internalSubscriptions));
        } else {
            // GP, WM perspective
            this.subscriptionCurrentStageGpPerspective(response, tenantId, productId, contributions, rateMap, showCurrency);
        }

        response.setCurrency(showCurrency);
        return response;
    }

    private void subscriptionCurrentStageGpPerspective(QueryDashboardSubscriptionCurrentStageResponse response, Long tenantId, Long productId, List<DashboardContributionEnum> contributions, Map<String, BigDecimal> rateMap, String showCurrency) {
        if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.INTERNAL)) {
            List<ProductSubscription> internalSubscriptions = Optional.ofNullable(
                    productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).isDistributedIfa(false).build()).getProductSubscriptionList()
            ).orElseGet(ArrayList::new);
            this.subscriptionCurrencyConversion(internalSubscriptions, rateMap, showCurrency);
            response.setInternal(this.subscriptionCurrentStage(internalSubscriptions));
        }

        if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.IFA)) {
            List<IFAAgreement> ifaAgreements = Optional.ofNullable(agreementApi.getIFAAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            if (!ObjectUtils.isEmpty(ifaAgreements)) {
                List<ProductSubscription> ifaSubscriptions = Optional.ofNullable(
                        productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).isDistributedIfa(true).build()).getProductSubscriptionList()
                ).orElseGet(ArrayList::new);
                this.subscriptionCurrencyConversion(ifaSubscriptions, rateMap, showCurrency);
                response.setIfa(this.subscriptionCurrentStage(ifaSubscriptions));
            }
        }

        if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.WM)) {
            List<TenantAgreement> tenantAgreements = Optional.ofNullable(agreementApi.getTenantAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            if (!ObjectUtils.isEmpty(tenantAgreements)) {
                List<ProductSubscription> wmSubscriptions = Optional.ofNullable(
                        productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().neTenantId(tenantId).productId(productId).productTenantId(tenantId).build()).getProductSubscriptionList()
                ).orElseGet(ArrayList::new);
                this.subscriptionCurrencyConversion(wmSubscriptions, rateMap, showCurrency);
                response.setWm(this.subscriptionCurrentStage(wmSubscriptions));
            }
        }
    }

    @GetMapping("/subscription-risk-rating")
    @BiteCache
    public QueryDashboardSubscriptionRiskRatingResponse queryDashboardsSubscriptionRiskRating(@RequestParam(value = "productId", required = false) Long productId,
                                                                                              @RequestParam("contributions") List<DashboardContributionEnum> contributions) {
        QueryDashboardSubscriptionRiskRatingResponse response = QueryDashboardSubscriptionRiskRatingResponse.builder().build();
        Long gpAccountId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_GP_ACCOUNT_ID, Long.class);
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID, Long.class);

        // Check whether the current login user is an IFA
        Set<GpRoleTypeEnum> manageRoles = this.dashBoardManageRoles();
        boolean checkManageRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(manageRoles).build());
        Set<GpRoleTypeEnum> ifaRoles = new HashSet<>();
        ifaRoles.add(GpRoleTypeEnum.WEALTH_MANAGER_IFA_RIA);
        boolean checkIfaRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(ifaRoles).build());

        List<ProductSubscription> subscriptions = new ArrayList<>();
        List<Long> tenantIds = new ArrayList<>();
        tenantIds.add(tenantId);

        if (contributions.contains(DashboardContributionEnum.WM)) {
            List<TenantAgreement> tenantAgreements = Optional.ofNullable(agreementApi.getTenantAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            tenantIds.addAll(tenantAgreements.stream().map(TenantAgreement::getWmId).toList());
        }

        if (!checkManageRole) {
            if (contributions.contains(DashboardContributionEnum.WM) || contributions.contains(DashboardContributionEnum.IFA)) {
                response.setStatus(QueryDashboardSubscriptionRiskRatingResponse.StatusEnum.PARAMS_ERROR);
                return response;
            }
            if (checkIfaRole) {
                // IFA perspective
                subscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).isDistributedIfa(true).salesId(gpAccountId).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            } else {
                // Normal GP perspective
                subscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).salesId(gpAccountId).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            }
        } else {
            // GP, WM perspective
            if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.INTERNAL)) {
                subscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).isDistributedIfa(false).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            }

            if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.IFA)) {
                subscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().tenantId(tenantId).productId(productId).isDistributedIfa(true).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            }

            if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.WM)) {
                subscriptions.addAll(
                        Optional.ofNullable(
                                productSubscriptionApi.querySubscription(QueryProductSubscriptionRequest.builder().neTenantId(tenantId).productId(productId).productTenantId(tenantId).build()).getProductSubscriptionList()
                        ).orElseGet(ArrayList::new)
                );
            }
        }

        // query tenant rate
        GetTenantCurrencyResponse tenantCurrencyResponse = Optional.ofNullable(metaCurrencyApi.getTenantCurrency()).orElseGet(GetTenantCurrencyResponse::new);
        Map<String, BigDecimal> rateMap = tenantCurrencyResponse.getMetaCurrencyWithExchangeRateVOS().stream().collect(Collectors.toMap(MetaCurrencyWithExchangeRateVO::getCurrencyCode, MetaCurrencyWithExchangeRateVO::getRate));
        response.setRate(rateMap);
        response.setCurrency(tenantCurrencyResponse.getBaseCurrency());

        String showCurrency = tenantCurrencyResponse.getBaseCurrency();
        if (!ObjectUtils.isEmpty(productId)) {
            Product product = Optional.ofNullable(productApi.getProductOnly(GetProductOnlyRequest.builder().productId(productId).build()).getProduct()).orElseGet(Product::new);
            showCurrency = product.getBaseCurrency();
        }

        // query risk rating task
        List<Tasks> tasks = Db.from(Tasks.class).query(it -> it.and(
                it.when(Tasks::getTasksNode).eq(TasksNodeEnum.EXTERNAL_AUDIT),
                it.when(Tasks::getTaskStatus).eq(TaskStatusEnum.DONE),
                it.when(Tasks::getTenantId).in(tenantIds)
        )).list();

        // Filter out subscriptions whose risk rating has done
        this.filterRiskRatingHasDone(response, subscriptions, tasks, rateMap, showCurrency);

        response.setCurrency(showCurrency);

        return response;
    }

    @GetMapping("/pipelines-stage")
    public QueryDashboardPipelineStageResponse queryDashboardPipelineStage(@RequestParam(value = "productId", required = false) Long productId,
                                                                           @RequestParam("contributions") List<DashboardContributionEnum> contributions) {
        QueryDashboardPipelineStageResponse response = QueryDashboardPipelineStageResponse.builder().build();

        Long gpAccountId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_GP_ACCOUNT_ID, Long.class);
        Long tenantId = ContextUtil.getParameter(ContextConstant.BITE_CLAIMS_TENANT_ID, Long.class);

        // query tenant rate
        GetTenantCurrencyResponse tenantCurrencyResponse = Optional.ofNullable(metaCurrencyApi.getTenantCurrency()).orElseGet(GetTenantCurrencyResponse::new);
        Map<String, BigDecimal> rateMap = tenantCurrencyResponse.getMetaCurrencyWithExchangeRateVOS().stream().collect(Collectors.toMap(MetaCurrencyWithExchangeRateVO::getCurrencyCode, MetaCurrencyWithExchangeRateVO::getRate));
        response.setRate(rateMap);
        String showCurrency = tenantCurrencyResponse.getBaseCurrency();
        if (!ObjectUtils.isEmpty(productId)) {
            Product product = Optional.ofNullable(productApi.getProductOnly(GetProductOnlyRequest.builder().productId(productId).build()).getProduct()).orElseGet(Product::new);
            showCurrency = product.getBaseCurrency();
        }

        // Check whether the current login user is an IFA
        Set<GpRoleTypeEnum> manageRoles = this.dashBoardManageRoles();
        boolean checkManageRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(manageRoles).build());
        Set<GpRoleTypeEnum> ifaRoles = new HashSet<>();
        ifaRoles.add(GpRoleTypeEnum.WEALTH_MANAGER_IFA_RIA);
        boolean checkIfaRole = permissionApi.checkGpAccountRole(QueryTenantGpAccountRoleRequest.builder().tenantId(tenantId).gpAccountId(gpAccountId).gpRoleTypeEnums(ifaRoles).build());

        List<Long> productIds = new ArrayList<>();
        if (!ObjectUtils.isEmpty(productId)) {
            productIds.add(productId);
        }
        List<Opportunity> internalOpportunities = new ArrayList<>();

        if (!checkManageRole) {
            if (contributions.contains(DashboardContributionEnum.WM) || contributions.contains(DashboardContributionEnum.IFA)) {
                response.setStatus(QueryDashboardPipelineStageResponse.StatusEnum.PARAMS_ERROR);
                return response;
            }
            if (checkIfaRole) {
                // IFA perspective
                internalOpportunities.addAll(
                        opportunityApi.listOpportunities(gpAccountId, true, tenantId, null, productIds)
                );
            } else {
                // Normal GP perspective
                internalOpportunities.addAll(
                        opportunityApi.listOpportunities(gpAccountId, null, tenantId, null, productIds)
                );
            }
            response.setInternal(this.dashboardPipelineStageResponse(internalOpportunities, rateMap, showCurrency));
        } else {
            // GP, WM perspective
            this.pipelineStageGpPerspective(response, tenantId, productId, productIds, contributions, rateMap, showCurrency);
        }

        response.setCurrency(showCurrency);
        return response;
    }

    private void pipelineStageGpPerspective(QueryDashboardPipelineStageResponse response, Long tenantId, Long productId, List<Long> productIds, List<DashboardContributionEnum> contributions, Map<String, BigDecimal> rateMap, String showCurrency) {
        if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.INTERNAL)) {
            List<Opportunity> internalOpportunities = opportunityApi.listOpportunities(null, false, tenantId, null, productIds);
            response.setInternal(this.dashboardPipelineStageResponse(internalOpportunities, rateMap, showCurrency));
        }

        if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.IFA)) {
            List<IFAAgreement> ifaAgreementList = Optional.ofNullable(agreementApi.getIFAAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            if (!ObjectUtils.isEmpty(ifaAgreementList)) {
                List<Opportunity> ifaOpportunities = opportunityApi.listOpportunities(null, true, tenantId, null, productIds);
                response.setIfa(this.dashboardPipelineStageResponse(ifaOpportunities, rateMap, showCurrency));
            }
        }

        if (contributions.contains(DashboardContributionEnum.ALL) || contributions.contains(DashboardContributionEnum.WM)) {
            List<TenantAgreement> tenantAgreementList = Optional.ofNullable(agreementApi.getTenantAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
            if (!ObjectUtils.isEmpty(tenantAgreementList)) {
                List<Opportunity> wmOpportunities = new ArrayList<>();
                List<AssetAgreement> assetAgreements = Optional.ofNullable(agreementApi.getAssetAgreements(tenantId, null, null, null).getData()).orElseGet(ArrayList::new);
                List<Long> agreementProductIds = assetAgreements.stream().map(AssetAgreement::getAssetId).toList();
                if (!ObjectUtils.isEmpty(productId) && !agreementProductIds.contains(productId)) {
                    response.setWm(this.dashboardPipelineStageResponse(wmOpportunities, rateMap, showCurrency));
                }
                if (ObjectUtils.isEmpty(productId)) {
                    productIds.addAll(agreementProductIds);
                }
                if (!ObjectUtils.isEmpty(productIds)) {
                    wmOpportunities = opportunityApi.listOpportunities(null, null, null, tenantId, productIds);
                }
                response.setWm(this.dashboardPipelineStageResponse(wmOpportunities, rateMap, showCurrency));
            }
        }
    }


    private List<QueryDashboardPipelineStageResponse.PipelineStage> dashboardPipelineStageResponse(List<Opportunity> opportunities, Map<String, BigDecimal> tenantRate, String showCurrent) {
        List<QueryDashboardPipelineStageResponse.PipelineStage> stages = new ArrayList<>();

        QueryDashboardPipelineStageResponse.PipelineStage prospectStage = new QueryDashboardPipelineStageResponse.PipelineStage();
        prospectStage.setStage(OpportunityStageEnum.PROSPECT);
        QueryDashboardPipelineStageResponse.PipelineStage meetingStage = new QueryDashboardPipelineStageResponse.PipelineStage();
        meetingStage.setStage(OpportunityStageEnum.MEETING);
        QueryDashboardPipelineStageResponse.PipelineStage dataRoomStage = new QueryDashboardPipelineStageResponse.PipelineStage();
        dataRoomStage.setStage(OpportunityStageEnum.DATA_ROOM);
        QueryDashboardPipelineStageResponse.PipelineStage softCommitmentStage = new QueryDashboardPipelineStageResponse.PipelineStage();
        softCommitmentStage.setStage(OpportunityStageEnum.SOFT_COMMITMENT);
        QueryDashboardPipelineStageResponse.PipelineStage committedStage = new QueryDashboardPipelineStageResponse.PipelineStage();
        committedStage.setStage(OpportunityStageEnum.COMMITTED);
        QueryDashboardPipelineStageResponse.PipelineStage declineStage = new QueryDashboardPipelineStageResponse.PipelineStage();
        declineStage.setStage(OpportunityStageEnum.DECLINE);

        BigDecimal showRate = tenantRate.get(showCurrent);
        for (Opportunity opportunity : opportunities) {
            BigDecimal rate = tenantRate.get(opportunity.getCurrency().getValue());

            BigDecimal targetSize = BigDecimal.ZERO;
            if (!ObjectUtils.isEmpty(opportunity.getCurrency()) && !ObjectUtils.isEmpty(opportunity.getTargetSize())) {
                targetSize = opportunity.getTargetSize().divide(rate, 5, RoundingMode.HALF_UP).multiply(showRate).setScale(5, RoundingMode.HALF_UP);
            }
            log.info("Dashboard opportunity chart, opportunity id:{}, opportunity stage:{}, opportunity target size:{}", opportunity.getId(), opportunity.getStage().getViewValue(), targetSize);
            switch (opportunity.getStage()) {
                case PROSPECT:
                    prospectStage.setTotalAmount(prospectStage.getTotalAmount().add(targetSize).setScale(5, RoundingMode.HALF_UP));
                    prospectStage.setTotalNumber(prospectStage.getTotalNumber() + 1);
                    break;
                case MEETING:
                    meetingStage.setTotalAmount(meetingStage.getTotalAmount().add(targetSize).setScale(5, RoundingMode.HALF_UP));
                    meetingStage.setTotalNumber(meetingStage.getTotalNumber() + 1);
                    break;
                case DATA_ROOM:
                    dataRoomStage.setTotalAmount(dataRoomStage.getTotalAmount().add(targetSize).setScale(5, RoundingMode.HALF_UP));
                    dataRoomStage.setTotalNumber(dataRoomStage.getTotalNumber() + 1);
                    break;
                case SOFT_COMMITMENT:
                    softCommitmentStage.setTotalAmount(softCommitmentStage.getTotalAmount().add(targetSize).setScale(5, RoundingMode.HALF_UP));
                    softCommitmentStage.setTotalNumber(softCommitmentStage.getTotalNumber() + 1);
                    break;
                case COMMITTED:
                    committedStage.setTotalAmount(committedStage.getTotalAmount().add(targetSize).setScale(5, RoundingMode.HALF_UP));
                    committedStage.setTotalNumber(committedStage.getTotalNumber() + 1);
                    break;
                case DECLINE:
                    declineStage.setTotalAmount(declineStage.getTotalAmount().add(targetSize).setScale(5, RoundingMode.HALF_UP));
                    declineStage.setTotalNumber(declineStage.getTotalNumber() + 1);
                    break;
                default:
                    break;
            }
        }

        stages.add(prospectStage);
        stages.add(meetingStage);
        stages.add(dataRoomStage);
        stages.add(softCommitmentStage);
        stages.add(committedStage);
        stages.add(declineStage);

        return stages;
    }

    private void filterRiskRatingHasDone(QueryDashboardSubscriptionRiskRatingResponse response, List<ProductSubscription> subscriptions, List<Tasks> tasks, Map<String, BigDecimal> tenantRate, String showCurrent) {
        QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating veryHigh = new QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating();
        QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating high = new QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating();
        QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating medium = new QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating();
        QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating low = new QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating();
        QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating veryLow = new QueryDashboardSubscriptionRiskRatingResponse.SubscriptionRiskRating();

        if (ObjectUtils.isEmpty(tasks)) {
            response.setVeryHigh(veryHigh);
            response.setHigh(high);
            response.setMedium(medium);
            response.setLow(low);
            response.setVeryLow(veryLow);
            return;
        }

        // Convert the committed amount of the order into tenant base currency
        Map<Long, BigDecimal> subscriptionsMap = subscriptions.stream().collect(Collectors.toMap(
                ProductSubscription::getId,
                subscription -> {
                    String currencyCode = ObjectUtils.isEmpty(subscription.getSnapshotShareClassCurrency()) ? subscription.getSnapshotProductBaseCurrency() : subscription.getSnapshotShareClassCurrency();
                    BigDecimal rate = tenantRate.get(currencyCode);
                    BigDecimal showRate = tenantRate.get(showCurrent);
                    if (ObjectUtils.isEmpty(subscription.getSubscriptionInvestmentAmountCommitted()) || BigDecimal.ZERO.compareTo(rate) == 0) {
                        return BigDecimal.ZERO;
                    }
                    return subscription.getSubscriptionInvestmentAmountCommitted().divide(rate, 5, RoundingMode.HALF_UP).multiply(showRate).setScale(5, RoundingMode.HALF_UP);
                }
        ));

        // level sort
        for (Tasks task : tasks) {
            if (ObjectUtils.isEmpty(task.getResult()) || ObjectUtils.isEmpty(task.getConditions())) {
                continue;
            }
            JSONObject resultObject = parseObject(task.getResult());
            String taskRatingLevel = resultObject.getString("Risk Rating");
            JSONObject conditionsObject = parseObject(task.getConditions());
            Long subscriptionId = conditionsObject.getLong("subscriptionId");
            if (subscriptionsMap.containsKey(subscriptionId)) {
                BigDecimal committedAmount = subscriptionsMap.get(subscriptionId);
                log.info("Dashboard task risk rating chart, task id: {}, task rating level:{}, subscription id: {}, subscription committed amount:{}", task.getId(), taskRatingLevel, subscriptionId, committedAmount);
                switch (taskRatingLevel) {
                    case "Very High":
                        veryHigh.setTotalCommittedAmount(veryHigh.getTotalCommittedAmount().add(committedAmount).setScale(5, RoundingMode.HALF_UP));
                        veryHigh.setTotalNumber(veryHigh.getTotalNumber() + 1);
                        break;
                    case "High":
                        high.setTotalCommittedAmount(high.getTotalCommittedAmount().add(committedAmount).setScale(5, RoundingMode.HALF_UP));
                        high.setTotalNumber(high.getTotalNumber() + 1);
                        break;
                    case "Medium":
                        medium.setTotalCommittedAmount(medium.getTotalCommittedAmount().add(committedAmount).setScale(5, RoundingMode.HALF_UP));
                        medium.setTotalNumber(medium.getTotalNumber() + 1);
                        break;
                    case "Low":
                        low.setTotalCommittedAmount(low.getTotalCommittedAmount().add(committedAmount).setScale(5, RoundingMode.HALF_UP));
                        low.setTotalNumber(low.getTotalNumber() + 1);
                        break;
                    case "Very Low":
                        veryLow.setTotalCommittedAmount(veryLow.getTotalCommittedAmount().add(committedAmount).setScale(5, RoundingMode.HALF_UP));
                        veryLow.setTotalNumber(veryLow.getTotalNumber() + 1);
                        break;
                    default:
                        log.info("Task rating level: {}", taskRatingLevel);
                        break;
                }
            }
        }

        response.setVeryHigh(veryHigh);
        response.setHigh(high);
        response.setMedium(medium);
        response.setLow(low);
        response.setVeryLow(veryLow);
    }

    // Convert the committed amount of the order into tenant base currency
    private void subscriptionCurrencyConversion(List<ProductSubscription> subscriptions, Map<String, BigDecimal> tenantRate, String showCurrent) {
        subscriptions.forEach(subscription -> {
            String currencyCode = ObjectUtils.isEmpty(subscription.getSnapshotShareClassCurrency()) ? subscription.getSnapshotProductBaseCurrency() : subscription.getSnapshotShareClassCurrency();
            BigDecimal rate = tenantRate.get(currencyCode);
            BigDecimal showRate = tenantRate.get(showCurrent);
            if (null != subscription.getSubscriptionInvestmentAmountCommitted()) {
                subscription.setSubscriptionInvestmentAmountCommitted(subscription.getSubscriptionInvestmentAmountCommitted().divide(rate, 5, RoundingMode.HALF_UP).multiply(showRate).setScale(5, RoundingMode.HALF_UP));
            }
        });
    }

    private QueryDashboardSubscriptionCurrentStageResponse.SubscriptionCurrentStage subscriptionCurrentStage(List<ProductSubscription> subscriptions) {
        QueryDashboardSubscriptionCurrentStageResponse.SubscriptionCurrentStage subscriptionCurrentStage = new QueryDashboardSubscriptionCurrentStageResponse.SubscriptionCurrentStage();
        if (!ObjectUtils.isEmpty(subscriptions)) {

            subscriptions.stream().forEach(subscription -> {
                log.info("Dashboard subscription chart, subscription id:{}, subscription step:{}, subscription committed amount:{}", subscription.getId(), subscription.getSubscriptionStepStatus().getShowValue(), subscription.getSubscriptionInvestmentAmountCommitted());
                switch (subscription.getSubscriptionStepStatus()) {
                    case SIGNING:
                        subscriptionCurrentStage.getSigning().setTotalCommittedAmount(subscriptionCurrentStage.getSigning().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getSigning().setTotalNumber(subscriptionCurrentStage.getSigning().getTotalNumber() + 1);
                        break;
                    case AWAITING_ADDITIONAL_DOCUMENTS:
                        subscriptionCurrentStage.getAwaitingAdditionalDocuments().setTotalCommittedAmount(subscriptionCurrentStage.getAwaitingAdditionalDocuments().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getAwaitingAdditionalDocuments().setTotalNumber(subscriptionCurrentStage.getAwaitingAdditionalDocuments().getTotalNumber() + 1);
                        break;
                    case ASSESSING_RISK:
                        subscriptionCurrentStage.getAssessingRisk().setTotalCommittedAmount(subscriptionCurrentStage.getAssessingRisk().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getAssessingRisk().setTotalNumber(subscriptionCurrentStage.getAssessingRisk().getTotalNumber() + 1);
                        break;
                    case REVIEWING:
                        subscriptionCurrentStage.getReviewing().setTotalCommittedAmount(subscriptionCurrentStage.getReviewing().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getReviewing().setTotalNumber(subscriptionCurrentStage.getReviewing().getTotalNumber() + 1);
                        break;
                    case APPROVED:
                        subscriptionCurrentStage.getApproved().setTotalCommittedAmount(subscriptionCurrentStage.getApproved().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getApproved().setTotalNumber(subscriptionCurrentStage.getApproved().getTotalNumber() + 1);
                        break;
                    case CAPITAL_CALL_OUTSTANDING:
                        subscriptionCurrentStage.getCapitalCallOutstanding().setTotalCommittedAmount(subscriptionCurrentStage.getCapitalCallOutstanding().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getCapitalCallOutstanding().setTotalNumber(subscriptionCurrentStage.getCapitalCallOutstanding().getTotalNumber() + 1);
                        break;
                    case INVESTED:
                        subscriptionCurrentStage.getInvested().setTotalCommittedAmount(subscriptionCurrentStage.getInvested().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getInvested().setTotalNumber(subscriptionCurrentStage.getInvested().getTotalNumber() + 1);
                        break;
                    case FULLY_INVESTED:
                        subscriptionCurrentStage.getFullyInvested().setTotalCommittedAmount(subscriptionCurrentStage.getFullyInvested().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getFullyInvested().setTotalNumber(subscriptionCurrentStage.getFullyInvested().getTotalNumber() + 1);
                        break;
                    case AML_AWAITING_ADDITIONAL_DOCUMENTS:
                        subscriptionCurrentStage.getAmlAwaitingAdditionalDocuments().setTotalCommittedAmount(subscriptionCurrentStage.getAmlAwaitingAdditionalDocuments().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getAmlAwaitingAdditionalDocuments().setTotalNumber(subscriptionCurrentStage.getAmlAwaitingAdditionalDocuments().getTotalNumber() + 1);
                        break;
                    case REJECTED:
                        subscriptionCurrentStage.getRejected().setTotalCommittedAmount(subscriptionCurrentStage.getRejected().getTotalCommittedAmount().add(subscription.getSubscriptionInvestmentAmountCommitted()).setScale(5, RoundingMode.HALF_UP));
                        subscriptionCurrentStage.getRejected().setTotalNumber(subscriptionCurrentStage.getRejected().getTotalNumber() + 1);
                        break;
                    default:
                        break;
                }
            });
        }
        return subscriptionCurrentStage;
    }

    private Set<GpRoleTypeEnum> dashBoardManageRoles() {
        Set<GpRoleTypeEnum> manageRoles = new HashSet<>();
        manageRoles.add(GpRoleTypeEnum.HEAD_OF_INVESTOR_RELATIONS);
        manageRoles.add(GpRoleTypeEnum.ADMIN);
        manageRoles.add(GpRoleTypeEnum.OWNER);
        manageRoles.add(GpRoleTypeEnum.INVESTMENT_DIRECTOR);
        manageRoles.add(GpRoleTypeEnum.OPERATIONAL_SUPPORT);
        manageRoles.add(GpRoleTypeEnum.BIZ_OPS);
        return manageRoles;
    }

}
