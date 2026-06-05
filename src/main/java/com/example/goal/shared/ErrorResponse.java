package com.example.goal.shared;

public class ErrorResponse {
    public String code;
    public String message;

    public static ErrorResponse of(String code, String message) {
        ErrorResponse response = new ErrorResponse();
        response.code = code;
        response.message = message;
        return response;
    }
}
