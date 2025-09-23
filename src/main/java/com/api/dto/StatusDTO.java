package com.api.dto;

public class StatusDTO {
    private int statusCode;
    private Object message;
    public StatusDTO(int statusCode, Object message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getMessage() {
        return message;
    }
}