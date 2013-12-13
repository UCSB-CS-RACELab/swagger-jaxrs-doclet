package com.hypnoticocelot.jaxrs.doclet.model;

import com.google.common.base.Objects;

public class ApiResponseMessage {

    private int code;
    
    private String message; // swagger 1.2 name

    private String responseModel;
    
    @SuppressWarnings("unused")
    private ApiResponseMessage() {
    }

    public ApiResponseMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getResponseModel() {
        return responseModel;
    }

    public void setResponseModel(String responseModel) {
        this.responseModel = responseModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiResponseMessage that = (ApiResponseMessage) o;
        return Objects.equal(code, that.code)
                && Objects.equal(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code, message);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("code", code)
                .add("message", message)
                .toString();
    }}
