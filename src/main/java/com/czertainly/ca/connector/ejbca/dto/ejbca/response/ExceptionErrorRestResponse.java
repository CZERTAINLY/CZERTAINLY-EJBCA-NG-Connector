package com.czertainly.ca.connector.ejbca.dto.ejbca.response;

public class ExceptionErrorRestResponse {

    private int errorCode;
    private String errorMessage;

    public ExceptionErrorRestResponse() {}

    private ExceptionErrorRestResponse(final int errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
