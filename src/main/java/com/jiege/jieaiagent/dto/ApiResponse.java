package com.jiege.jieaiagent.dto;

import java.util.Map;

public class ApiResponse {

    private int code;
    private String message;
    private Object data;

    private ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(0, "ok", data);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(0, message, data);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }

    public Map<String, Object> toMap() {
        return Map.of("code", code, "message", message != null ? message : "", "data", data != null ? data : Map.of());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
