package com.heking.android.zcamera;

import android.graphics.Camera;

/**
 * @Author HK-LJJ
 * @Date 2019/10/22
 * @Description TODO
 */
public interface CameraEventCallback {
    /**
     * 找不到相机
     */
    int ERROR_CAMERA_NO_FOUND=1001;
    /**
     * 打开相机失败
     */
    int ERROR_CAMERA_OPEN_FAIL=1002;

    void onOpenCameraError(int errorCode,String msg);
    void onOpenCameraSuccess(Camera camera);

}
