package com.heking.android.zcamera.camera;

/**
 * 拍摄回掉
 */
public interface CaptureCallback {
    int SUCCESS = 1;
    int FAILURE = 2;

    void onResult(int code,String path);
}
