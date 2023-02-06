package com.thin.cqrsesorder.bean.request;

import lombok.Data;

@Data
public abstract class BaseRequest {

    private boolean isRedirect;

    public abstract Integer getHashInt();

    public abstract String getRequestId();
}
