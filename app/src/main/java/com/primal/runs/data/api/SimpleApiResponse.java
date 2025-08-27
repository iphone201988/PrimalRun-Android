package com.primal.runs.data.api;

import com.google.gson.annotations.SerializedName;

public class SimpleApiResponse {
    @SerializedName("method")
    protected String method;
    @SerializedName("message")
    protected String message;
    @SerializedName("status")
    protected int status;
    @SerializedName("success")
    protected String success;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String isSuccess() {
        return method;
    }



    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SimpleApiResponse{" +
                "method=" + method +
                ", message=" + message +
                ", success=" + success +
                ",status=" + status + '\'' +

                '}';
    }
}