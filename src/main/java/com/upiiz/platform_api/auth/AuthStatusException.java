package com.upiiz.platform_api.auth;

public class AuthStatusException extends RuntimeException {
    private final String code;
    private final String action;

    public AuthStatusException(String code, String message, String action) {
        super(message);
        this.code = code;
        this.action = action;
    }

    public String getCode() {
        return code;
    }

    public String getAction() {
        return action;
    }
}
