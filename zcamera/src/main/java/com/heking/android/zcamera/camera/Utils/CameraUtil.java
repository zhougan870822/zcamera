package com.heking.android.zcamera.camera.Utils;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具类
 */
public class CameraUtil {
    private static final String TAG = "CameraUtil";

    /**
     * 找出与指定宽高相等或最接近的尺寸 一般手机支持4:3,16:9
     * (如果有多个则找出面积最接近的)
     *
     * @param requestWidth  需要的宽度
     * @param requestHeight 需要的高度
     * @param sizeList      支持的尺寸集合
     * @param exchangeWH    是否互换宽高
     * @return
     */
    public static Camera.Size getBestSize(int requestWidth, int requestHeight, List<Camera.Size> sizeList, boolean exchangeWH) {
        Log.d(TAG, "getBestSize:requestWidth=" + requestWidth + ",requestHeight=" + requestHeight);
        //互换宽高
        if (exchangeWH) {
            int temp = requestWidth;
            requestWidth = requestHeight;
            requestHeight = temp;
            Log.d(TAG, "getBestSize:互换宽高");
        }

        //最合适的尺寸
        Camera.Size bestSize = null;
        //目标宽高比
        double targetRatio = (double) requestWidth / (double) requestHeight;
        Log.d(TAG, "getBestSize:targetRatio=" + targetRatio);
        double ratio = 0;
        int tempArea = 0;
        double tempRatio = 0;
        int minArea = Integer.MAX_VALUE;
        double minRatio = Double.MAX_VALUE;
        //宽高比不等的尺寸集合
        List<Camera.Size> cacheSize = new ArrayList<>();
        //遍历支持的尺寸
        for (Camera.Size size : sizeList) {
//            Log.d(TAG, "size:[" + size.width + "," + size.height + "]");
            //宽高相等,直接返回
            if (size.width == requestWidth && size.height == requestHeight) {
//                Log.d(TAG, "找到相同的尺寸size:[" + size.width + "," + size.height + "]");
                return size;
            }
            ratio = (double) size.width / (double) size.height;

            //宽高比相等,找出面积最接近的Size
            if (ratio == targetRatio) {
//                Log.d(TAG, "宽高比相等:bestSize:[" + size.width + "," + size.height + "]");
                tempArea = Math.abs(size.width * size.height - requestWidth * requestHeight);
                if (tempArea < minArea) {
                    bestSize = size;
                    minArea = tempArea;
                }
            } else {
//                Log.d(TAG, "getBestSize:宽高比不等");
                //宽高比不等,找出宽高比最接近的尺寸,可能有多个
                tempRatio = Math.abs(ratio - targetRatio);
                if (tempRatio < minRatio) {
                    cacheSize.clear();
                    minRatio = tempRatio;
                    cacheSize.add(size);
                } else if (tempRatio == minRatio) {
                    cacheSize.add(size);
                }

            }
        }

        if (bestSize != null) {
            return bestSize;
        }

        Log.d(TAG, "getBestSize:cacheSize.size=" + cacheSize.size());
        //遍历找出面积最接近的尺寸
        minArea = Integer.MAX_VALUE;
        for (Camera.Size size : cacheSize) {
//            Log.d(TAG, "宽高比不相等:bestSize:[" + size.width + "," + size.height + "]");
            tempArea = Math.abs(size.width * size.height - requestHeight * requestWidth);
            if (tempArea < minArea) {
                bestSize = size;
                minArea = tempArea;
            }
        }
        if(bestSize!=null){
            Log.d(TAG, "getBestSize:bestSize=[" +bestSize.width+","+bestSize.height+"]" );
        }else{
            Log.e(TAG, "bestSize==null");
        }
        return bestSize;
    }

    /**
     * //根据指定的相机方向(前置/后置)找到对应的相机id
     *
     * @param cameraFacing {@link Camera.CameraInfo#CAMERA_FACING_BACK,Camera.CameraInfo#CAMERA_FACING_BACK}
     * @return cameraId -1表示没有找到
     */
    public static int getCameraIdByFacing(int cameraFacing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int id = 0; id < numberOfCameras; id++) {
            Camera.getCameraInfo(id, info);
            if (info.facing == cameraFacing) {
                return id;
            }
        }
        return -1;
    }


    /**
     * 是否支持给定的预览编码格式
     *
     * @param parameters
     * @param previewFormat eg:{@link android.graphics.ImageFormat#NV16}
     * @return
     */
    public static boolean supportPreviewFormat(Parameters parameters, int previewFormat) {
        if (parameters != null) {
            List<Integer> formats = parameters.getSupportedPreviewFormats();
            if (formats != null && formats.size() > 0) {
                for (Integer format : formats) {
                    if (format == previewFormat) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取窗口旋转角度(逆时针),和手机旋转角度无关
     *
     * @return
     */
    public static int getWindowRotation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        //手机旋转的角度
        int windowRotation = 0;
        switch (rotation) {
            case Surface.ROTATION_0://竖屏
                windowRotation = 0;
                break;
            case Surface.ROTATION_90://逆时针旋转90度(左横屏)
                windowRotation = 90;
                break;
            case Surface.ROTATION_180://逆时针旋转180度(倒竖屏)
                windowRotation = 180;
                break;
            case Surface.ROTATION_270://逆时针旋转270度(右横屏)
                windowRotation = 270;
                break;
        }
        return windowRotation;
    }

    /**
     * 获取预览方向{@link Camera#setDisplayOrientation(int)}
     *
     * @param context
     * @param cameraInfo
     * @param cameraFacing
     * @return
     */
    public static int getPreOrientation(Context context, Camera.CameraInfo cameraInfo, int cameraFacing) {
        int preRotation;
        int windowRotation = getWindowRotation(context);
        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            preRotation = (cameraInfo.orientation - windowRotation + 360) % 360;
        } else {
            preRotation = (cameraInfo.orientation + windowRotation) % 360;
            preRotation = (360 - preRotation) % 360;
        }
     /*  switch (rotation){
           case 0:
               preRotation=90;
               break;
           case 90:
               preRotation=0;
               break;
           case 180:
               preRotation=270;
               break;
           case 270:
               preRotation=180;
               break;
       }*/

        return preRotation;

    }

    /**
     * 获取保存的图片的方向{@link Camera.Parameters#setRotation(int)}
     *
     * @param phoneRotation 手机旋转的角度可以用{@link OrientationEventListener }
     *                      或{@link com.heking.android.zcamera.content.SimpleRotationListener}监听
     */
    public static int getPicRotation(Camera.CameraInfo cameraInfo, int cameraFacing, int phoneRotation) {
        int picRotation = 0;
        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            picRotation = (cameraInfo.orientation + phoneRotation) % 360;
        } else {
            picRotation = (cameraInfo.orientation - phoneRotation + 360) % 360;

        }

        return picRotation;
    }

    /**
     * 是否支持对焦模式
     *
     * @param parameters Parameters
     * @param focusMode eg:{@link  Parameters#FOCUS_MODE_AUTO}
     * @return
     */
    public static boolean supportFocus(Parameters parameters,String focusMode) {
        if(parameters==null){
            return false;
        }
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if(supportedFocusModes==null || supportedFocusModes.size()==0){
            return false;
        }
        return supportedFocusModes.contains(focusMode);
    }

    /**
     * 是否支持指定的闪光灯模式
     * @param flashMode eg:{@link Parameters#FLASH_MODE_AUTO}
     * @return
     */
    public static boolean supportFlashMode(Parameters parameters,String flashMode){
        if(parameters!=null){
            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if(supportedFlashModes==null){
                return false;
            }
            return supportedFlashModes.contains(flashMode);
        }
        return false;
    }


    /**
     * 开始预览
     */
    public static void startPreview(Camera camera) {
        if (camera != null) {
            camera.startPreview();
            Log.d(TAG, "startPreview:开始预览");
        }
    }

    /**
     * 停止预览
     */
    public static void stopPreview(Camera camera) {
        if (camera != null) {
            camera.stopPreview();
            Log.d(TAG, "stopPreview:停止预览");
        }
    }

    /**
     * 释放相机
     */
    public static void releaseCamera(Camera camera) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.setPreviewDisplay(null);
                camera.release();
                camera = null;
                Log.d(TAG, "releaseCamera:释放相机");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}
