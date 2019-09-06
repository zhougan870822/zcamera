package com.heking.android.zcamera.camera.Utils;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * 变焦工具
 */
public class ZoomHelper {
    private static final String TAG = "ZoomHelper";
    private Camera.Parameters parameters;
    private Camera camera;
    private List<Integer> zoomRatios;
    private final float minZoom = 1;
    private float maxZoom = 1;
    private float zoom = minZoom;//变焦的倍数
    private final int  minZoomIndex=0;
    private int maxZoomIndex=0;
    private int zoomIndex=minZoomIndex;

    private boolean supportZoom = false;

    private ZoomHelper() {

    }

    public static ZoomHelper build(Camera camera, Camera.Parameters parameters) {
        ZoomHelper zoomHelper = new ZoomHelper();
        zoomHelper.setCamera(camera);
        zoomHelper.setParameters(parameters);

        return zoomHelper;
    }

    /**
     * 初始化
     */
    public void init() {
        if (parameters != null) {
            supportZoom = parameters.isZoomSupported();
            if (supportZoom) {
                zoomRatios = parameters.getZoomRatios();
                if (zoomRatios != null && zoomRatios.size() > 0) {
                    maxZoom = zoomRatios.get(zoomRatios.size() - 1) / 100f;
                    maxZoomIndex=zoomRatios.size()-1;
                }
            }
        }
    }

    /**
     * 释放资源
     */
    public void release(){
        parameters=null;
        camera=null;
        zoomRatios=null;
        maxZoom=1;
        supportZoom=false;
        zoom=1;
    }

    /**
     * 获取设置zoom的index
     *
     * @param zoom
     * @return
     */
    private int getZoomIndexByZoom(float zoom) {
        if (zoomRatios != null && zoomRatios.size() > 0) {
            int requestZoomRatio = (int) (zoom * 100);
            int size = zoomRatios.size();
            int i = 0;
            for (i = 0; i < size - 1; i++) {
                if (requestZoomRatio >= zoomRatios.get(i) && requestZoomRatio < zoomRatios.get(i + 1)) {
                    return i;
                }

            }
            return i;
        }
        return -1;
    }

    /**
     * 设置焦距
     * @param zoom 变焦倍数
     */
    public void setZoom(float zoom) {
        if(!isSupportZoom()) {
            return;
        }
        if (zoom < minZoom) {
            zoom = minZoom;
        } else if (zoom > maxZoom) {
            zoom = maxZoom;
        }
        if(this.zoom==zoom)return;
        if (parameters != null && camera != null) {
            int zoomIndexByZoom = getZoomIndexByZoom(zoom);
            if (zoomIndexByZoom >= 0) {
                parameters.setZoom(zoomIndexByZoom);
                camera.setParameters(parameters);
                this.zoom = zoom;
                this.zoomIndex = zoomIndexByZoom;
                Log.d(TAG, "setZoom:zoom="+zoom);
            }
        }

    }

    public void setZoomIndex(int zoomIndex){
        if (!isSupportZoom()) {
            return;
        }
        if (zoomIndex < minZoomIndex) {
            zoomIndex = minZoomIndex;
        } else if (zoomIndex > maxZoomIndex) {
            zoomIndex = maxZoomIndex;

        }
        if (zoomIndex == this.zoomIndex) {
            return;
        }

        if (parameters != null && camera != null) {
            parameters.setZoom(zoomIndex);
            camera.setParameters(parameters);
            this.zoom = zoomRatios.get(zoomIndex) / 100f;
            this.zoomIndex = zoomIndex;
            Log.d(TAG, "setZoom:zoom=" + zoom);
        }
    }


    public void setParameters(Camera.Parameters parameters) {
        this.parameters = parameters;
    }


    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public float getMinZoom() {
        return minZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public int getMinZoomIndex() {
        return minZoomIndex;
    }

    public int getMaxZoomIndex() {
        return maxZoomIndex;
    }

    public boolean isSupportZoom() {
        return supportZoom && zoomRatios!=null && zoomRatios.size()>0 && maxZoom>minZoom;
    }

    public float getZoom() {
        return zoom;
    }

    public int getZoomIndex() {
        return zoomIndex;
    }
}
