package com.heking.android.zcamera.camera.Utils;

import android.hardware.Camera;
import android.util.Log;

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
        Log.d(TAG, "getBestSize:requestWidth="+requestWidth+",requestHeight="+requestHeight);
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
        Log.d(TAG, "getBestSize:targetRatio="+targetRatio);
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
                Log.d(TAG, "找到相同的尺寸size:[" + size.width + "," + size.height + "]");
                return size;
            }
            ratio = (double) size.width / (double) size.height;

            //宽高比相等,找出面积最接近的Size
            if (ratio == targetRatio) {
                Log.d(TAG, "宽高比相等:bestSize:[" + size.width + "," + size.height + "]");
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

        Log.d(TAG, "getBestSize:cacheSize.size="+cacheSize.size());
        //遍历找出面积最接近的尺寸
        minArea = Integer.MAX_VALUE;
        for (Camera.Size size : cacheSize) {
            Log.d(TAG, "宽高比不相等:bestSize:[" + size.width + "," + size.height + "]");
            tempArea = Math.abs(size.width * size.height - requestHeight * requestWidth);
            if (tempArea < minArea) {
                bestSize = size;
                minArea = tempArea;
            }
        }
        Log.d(TAG, "getBestSize:bestSize="+bestSize);
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
}
