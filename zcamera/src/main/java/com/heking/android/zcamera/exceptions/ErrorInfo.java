package com.heking.android.zcamera.exceptions;

public enum  ErrorInfo {
    ERROR_NO_CAMERA(101,"没有找到指定的相机"),
    ERROR_NO_SURFACEVIEW(102,"空指针异常 SurfaceView ==null");



    private int code;
    private String error;

    ErrorInfo(int code, String error) {
        this.code = code;
        this.error = error;
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }
}
