package com.heking.android.zcamera.exceptions;

public class CameraError extends Exception {
    private int code;
    private String errorMessage;


    public static CameraError build(ErrorInfo errorInfo){
        return new CameraError(errorInfo.getCode(), errorInfo.getError());
    }


    private CameraError(int code, String errorMessage) {
        super(msg(code,errorMessage));
        this.code = code;
        this.errorMessage = errorMessage;
    }

    public int getCode() {
        return code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static String msg(int code, String errorMessage){
        return "["+code+"]"+errorMessage;
    }



}
