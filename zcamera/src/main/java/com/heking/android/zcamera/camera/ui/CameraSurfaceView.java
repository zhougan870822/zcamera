package com.heking.android.zcamera.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.ViewConfiguration;

import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.camera.Utils.ZoomHelper;

public class CameraSurfaceView extends SurfaceView {
    private static final String TAG = "CameraSurfaceView";

    private CameraManager cameraManager;
    private ZoomHelper mZoomHelper;
    private int touchSlop = -1;
    private int doubleTapSlop = -1;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        if (touchSlop == -1) {
            doubleTapSlop = ViewConfiguration.get(getContext()).getScaledDoubleTapSlop();
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }
    }

    private boolean start = false;

    private float startDistance;


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        ZoomHelper zoomHelper = getZoomHelper();
        if (zoomHelper == null || !zoomHelper.isSupportZoom()) {
//            Log.d(TAG, "onTouchEvent: 不支持变焦");
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                int actionIndex = event.getActionIndex();
                if (actionIndex <= 1) {
                    start = false;
                } else {
                    startDistance = distance(event);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (start) {
                    float endDistance = distance(event);
                    float dis=endDistance-startDistance;
                    if(Math.abs(dis)>=touchSlop){
//                        Log.d(TAG, "onTouchEvent:touchSlop="+touchSlop);
                        int zoomIndex=0;
                        if(dis>0 ){
                             zoomIndex = zoomHelper.getZoomIndex()+1;
                        }else{
                             zoomIndex = zoomHelper.getZoomIndex()-1;
                        }
                        if(zoomIndex<zoomHelper.getMinZoomIndex()){
                            zoomIndex=zoomHelper.getMinZoomIndex();
                        }else if(zoomIndex>zoomHelper.getMaxZoomIndex()){
                            zoomIndex=zoomHelper.getMaxZoomIndex();
                        }
//                        Log.d(TAG, "onTouchEvent:zoomIndex="+zoomIndex);
                        zoomHelper.setZoomIndex(zoomIndex);
                        startDistance=endDistance;
                    }



                } else {
                    if (event.getPointerCount() >= 2) {
                        float distance = distance(event);
                        if (distance >= doubleTapSlop) {
//                            Log.d(TAG, "onTouchEvent:doubleTapSlop=" + doubleTapSlop);
                            start = true;
                            startDistance = distance;
                        }
                    }
                }
                break;

        }


        return true;
    }

    private float distance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.sqrt(dx * dx + dy * dy);

    }

    private ZoomHelper getZoomHelper() {
        if (mZoomHelper == null && cameraManager != null) {
            mZoomHelper = cameraManager.getZoomHelper();
        }
        return mZoomHelper;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }
}
