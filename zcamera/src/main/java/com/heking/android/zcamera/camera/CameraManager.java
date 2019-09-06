package com.heking.android.zcamera.camera;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;

import com.heking.android.zcamera.camera.Utils.CameraUtil;
import com.heking.android.zcamera.camera.Utils.ZoomHelper;
import com.heking.android.zcamera.exceptions.CameraError;
import com.heking.android.zcamera.exceptions.ErrorInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 相机管理类
 */
public class CameraManager {
    private static final String TAG = "CameraManager";

    //拍照保存图片
    public static final int MSG_WHAT_CAPTURE_IMAGE = 100;

    /**
     * 相机
     */
    private Camera mCamera;
    /**
     * 相机的方向
     */
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    /**
     * 相机id
     */
    private int mCameraId;
    /**
     * 相机参数,记住一定用统一个Parameters 防止交叉设置相机参数时,漏掉参数
     */
    private Camera.Parameters mParameters;
    /**
     * 拍照后是否可以继续预览(连拍)
     */
    private boolean continuePreView=false;

    /**
     * 自拍镜像
     */
    private boolean mirror=true;

    //需要的尺寸,可能会被改变,当和预览尺寸的比例不一样时
    //请把预览和保存图片的宽高比设置一样
    //相机一般为4:3/16:9
    private int picWidth = 1080;
    private int picHeight = 1920;
    private int preWidth = 1080;
    private int preHeight = 1920;

    /**
     * 闪光灯模式
     */
    private String mFlashModel=Camera.Parameters.FLASH_MODE_OFF;

    /**
     * 手机逆时针旋转的角度 默认竖屏0度
     */
    private int mPhoneRotation=0;

    /**
     * 正在拍照
     */
    private boolean capturing=false;

    /**
     * 预览视图
     */
    private SurfaceView mSurfaceView;
    /**
     * 预览回掉,data[] 注意编码问题getPreviewFormat() setPreviewFormat
     */
    private Camera.PreviewCallback previewCallback;
    /**
     * 预览视图的编码
     */
    private int mPreviewFormat=ImageFormat.NV21;

    private SurfaceHolder mHolder;

    /**
     * 拍照回掉
     */
    private CaptureCallback mCaptureCallback;

    /**
     * 变焦工具
     */
    private ZoomHelper mZoomHelper;

    /**
     * 初始化方法 默认打开后置摄像头
     *
     * @throws CameraError
     */
    public void init() throws CameraError {
        if (mSurfaceView == null) {
            throw new NullPointerException("SurfaceView ==null");
        }

        mHolder = mSurfaceView.getHolder();
        mHolder.setKeepScreenOn(true);
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated:");
                startOrientationListener(mSurfaceView.getContext());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                //在surfaceCreated方法后,至少调用一次
                Log.d(TAG, "surfaceChanged:surfaceView视图的宽高 width=" + width + ",height=" + height);
                //需求和显示的宽高比不一致
                computeSize(width, height);
                if (mCamera == null) {
                    try {
                        Log.d(TAG, "surfaceChanged:mCameraFacing="+mCameraFacing);
                        openCamera(mCameraFacing);
                    } catch (CameraError cameraError) {
                        cameraError.printStackTrace();
                    }
                } else {
                    stopPreview();
                }
               startPreview(); //开始预览
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed:");
                stopOrientationListener();
                releaseCamera();
            }
        });
    }

    /**
     * 重新计算预览和图片尺寸
     *
     * @param width
     * @param height
     */
    private void computeSize(int width, int height) {
        if ((double) preWidth / preHeight != (double) width / height) {
            //分别计算出宽高的倍数
            double preRatioW = (double)preWidth / width;
            double preRatioH = (double)preHeight / height;
            double preRadio = preRatioW > preRatioH ? preRatioW : preRatioH;
            preWidth = (int) (width * preRadio);
            preHeight = (int) (height * preRadio);
            Log.d(TAG, "surfaceChanged:重设preWidth=" + preWidth + ",preHeight=" + preHeight);

        }
       /* if ((double) picWidth / picHeight != (double) width / height) {
            double picRatioW = (double)picWidth / width;
            double picRatioH =(double) picHeight / height;
            double picRadio = picRatioW > picRatioH ? picRatioW : picRatioH;
            picWidth = (int) (width * picRadio);
            picHeight = (int) (height * picRadio);
            Log.d(TAG, "surfaceChanged:重设picWidth=" + picWidth + ",picHeight=" + picHeight);
        }*/
    }

    /**
     * 打开相机
     * @param cameraFacing {@link Camera.CameraInfo#CAMERA_FACING_BACK,Camera.CameraInfo#CAMERA_FACING_BACK}
     * @throws CameraError
     */
    private void openCamera(int cameraFacing) throws CameraError {
        mCameraId = CameraUtil.getCameraIdByFacing(cameraFacing);
        if (mCameraId != -1) {
            mCamera = Camera.open(mCameraId);
            //预览回掉
            if (previewCallback != null) {
                mCamera.setPreviewCallback(previewCallback);
            }
            //配置参数
            initParameters();
            try {
                //预览窗口
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            throw CameraError.build(ErrorInfo.ERROR_NO_CAMERA);
        }
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
            Log.d(TAG, "startPreview:开始预览");

        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            Log.d(TAG, "stopPreview:停止预览");

        }
    }


    /**
     * 释放相机
     */
    public void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewDisplay(null);
                mCamera.release();
                mCamera = null;
                mParameters=null;
                if(mZoomHelper!=null){
                    mZoomHelper.release();
                }

                Log.d(TAG, "releaseCamera:释放相机");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 拍照
     */
    public void captureImage() {
        captureImage(null);
    }

    /**
     * 拍照
     *
     * @param path
     */
    public void captureImage(String path) {
        if(capturing){
            Log.e(TAG, "正在拍照请稍后...");
            return;
        }
        capturing=true;
        if (TextUtils.isEmpty(path)) {
            path = cacheImageKeepPath();
        }
        final String filePath = path;
        //shutter 快门回掉
        //raw 原始（未压缩）图像数据的回调
        //postview
        //jpeg jpeg回掉
        try {
            mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Log.d(TAG, "onPictureTaken:data.length="+data.length);
                    if(mCameraFacing==Camera.CameraInfo.CAMERA_FACING_FRONT && mirror){
                        Log.d(TAG, "onPictureTaken:自拍镜像");
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Log.d(TAG, "onPictureTaken:bitmap.size="+bitmap.getByteCount());
                        Matrix matrix=new Matrix();
                        matrix.postScale(-1, 1); // 镜像水平翻转
                        Bitmap convertBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        Log.d(TAG, "onPictureTaken:convertBmp.size="+convertBmp.getByteCount());

                        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
                        convertBmp.compress(Bitmap.CompressFormat.JPEG,80 , byteArrayOutputStream);
                        data=byteArrayOutputStream.toByteArray();
                        Log.d(TAG, "onPictureTaken:data.length="+data.length);
                    }


                    if (data != null && data.length > 0) {
                        keepFile(filePath, data);
                    } else if (mCaptureCallback != null) {
                        mCaptureCallback.onResult(CaptureCallback.FAILURE, null);
                    }
                    if(continuePreView){
                        startPreview();
                    }
                    capturing=false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            capturing=false;
            if (mCaptureCallback != null) {
                mCaptureCallback.onResult(CaptureCallback.FAILURE, null);
            }
        }
    }


    /**
     * 保存图片的缓存路径
     *
     * @return
     */
    private String cacheImageKeepPath() {
        File cacheDir = mSurfaceView.getContext().getExternalCacheDir();
        File file = new File(cacheDir, "image/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file.getAbsolutePath();
    }

    /**
     * 主线程的Handler
     */
    private Handler mainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_CAPTURE_IMAGE:
                    Bundle data = msg.getData();
                    boolean success = data.getBoolean("success");
                    String path = data.getString("path", null);
                    if (mCaptureCallback != null) {
                        mCaptureCallback.onResult(success ? CaptureCallback.SUCCESS : CaptureCallback.FAILURE, path);
                    }
                    break;
            }

            return true;
        }
    });

    /**
     * 保存文件
     *
     * @param path
     * @param data
     */
    private void keepFile(final String path, final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(path);
                    fos.write(data, 0, data.length);
                    fos.flush();
                    success = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Message message = mainHandler.obtainMessage();
                    message.what = MSG_WHAT_CAPTURE_IMAGE;
                    Bundle bundle = message.getData();
                    bundle.putBoolean("success", success);
                    bundle.putString("path", path);
                    message.setData(bundle);
                    mainHandler.sendMessage(message);
                    try {
                        if (fos != null) {
                            fos.close();
                            fos = null;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }


    /**
     * 切换前后摄像头
     */
    public void changeCamera() {
        releaseCamera();
        if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        try {
            Log.d(TAG, "changeCamera:mCameraFacing="+mCameraFacing);
            openCamera(mCameraFacing);
            startPreview(); //开始预览
        } catch (CameraError cameraError) {
            cameraError.printStackTrace();
        }
    }

    /**
     * 是否横屏
     *
     * @return
     */
    private boolean isLandscape() {
        //1 竖;2横屏,
        int orientation = mSurfaceView.getContext().getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 屏幕旋转监听
     */
    private OrientationEventListener mOrientationEventListener;

    /**
     * 注册屏幕旋转监听
     * @param context
     */
    private void startOrientationListener(Context context){
        if(mOrientationEventListener==null){
            mOrientationEventListener=new OrientationEventListener(context) {
                @Override
                public void onOrientationChanged(int orientation) {
                    //计算手机当前方向的角度值
                    int phoneDegree = 0;
                    if (((orientation >= 0) && (orientation <= 45))|| (orientation > 315) &&(orientation<=360)) {
                        phoneDegree = 0;
                    } else if ((orientation > 45) && (orientation <= 135)) {
                        phoneDegree = 90;
                    } else if ((orientation > 135) && (orientation <= 225)) {
                        phoneDegree = 180;
                    } else if ((orientation > 225) && (orientation <= 315)) {
                        phoneDegree = 270;
                    }
                    //手机方向改变了计算图片旋转角度
                    if(mPhoneRotation!=phoneDegree){
                        mPhoneRotation=phoneDegree;
                        setPicRotation(mPhoneRotation);
                    }


                }
            };
        }
        //启动方向感应器
        mOrientationEventListener.enable();
    }

    /**
     * 销毁屏幕旋转监听
     */
    private void stopOrientationListener(){
        if(mOrientationEventListener!=null){
            mOrientationEventListener.disable();
        }
    }


    /**
     * 获取窗口旋转角度
     *
     * @return
     */
    private int getWindowRotation() {
        WindowManager windowManager = (WindowManager) mSurfaceView.getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        //手机旋转的角度
       int  windowRotation  = 0;
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
     * 设置预览方向
     * @param rotation
     */
    private void setPreRotation(int rotation){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        int preRotation=0;
        if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            preRotation = (cameraInfo.orientation - rotation + 360) % 360;
        } else {
            preRotation = (cameraInfo.orientation + rotation) % 360;
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

        if(mCamera!=null){
            mCamera.setDisplayOrientation(preRotation);
            Log.d(TAG, "setPreRotation:rotation="+rotation);
            Log.d(TAG, "setPreRotation:设置预览方向:preRotation="+preRotation);
        }

    }

    /**
     * 设置保存的图片的方向
     * @param phoneRotation
     */
    private void setPicRotation(int phoneRotation){
        Camera.Parameters parameters = _getParameters();
        if(parameters==null)return;

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        int picRotation = 0;
        if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            picRotation = (cameraInfo.orientation + phoneRotation) % 360;
        } else {
            picRotation = (cameraInfo.orientation - phoneRotation + 360) % 360;

        }
        parameters.setRotation(picRotation);
        mCamera.setParameters(parameters);
        Log.d(TAG, "setPicRotation:phoneRotation=" + phoneRotation);
        Log.d(TAG, "setPicRotation 设置图片方向:picRotation=" + picRotation);
    }


    /**
     * //配置相机参数
     *
     */
    private void initParameters() {
        Camera.Parameters parameters = _getParameters();
        if(parameters==null) return;

        boolean exChangeWH = !isLandscape();
        //设置预览图片的格式
        if(supportPreviewFormat(mPreviewFormat)){
            parameters.setPreviewFormat(mPreviewFormat);
        }else{
            parameters.setPreviewFormat(ImageFormat.NV21);
        }

        Camera.Size preSize = CameraUtil.getBestSize(preWidth, preHeight, parameters.getSupportedPreviewSizes(), exChangeWH);
        Log.d(TAG, "initParameters:preSize=[" + preSize.width + "," + preSize.height + "]");
        parameters.setPreviewSize(preSize.width, preSize.height);

        //设置保存图片的格式和尺寸
        parameters.setPictureFormat(ImageFormat.JPEG);
        Camera.Size picSize = CameraUtil.getBestSize(picWidth, picHeight, parameters.getSupportedPictureSizes(), exChangeWH);
        Log.d(TAG, "initParameters:picSize=[" + picSize.width + "," + picSize.height + "]");
        parameters.setPictureSize(picSize.width, picSize.height);

        //预览方向
        setPreRotation( getWindowRotation());
        //拍照方向
        setPicRotation(mPhoneRotation);

        //设置对焦模式
        if (supportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (supportFocus(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        /*if(mCameraFacing==Camera.CameraInfo.CAMERA_FACING_FRONT){
            //off（关闭）,flip-v（竖直翻转）,flip-h（水平翻转）,flip-vh（竖直+水平翻转）
            parameters.set("preview-flip", "flip-h");
            parameters.set("flip-mode-values", "0ff");
            Log.d(TAG, "initParameters:镜像");
        }*/
//        String flatten = parameters.flatten();
//        Log.d(TAG, "initParameters:flatten="+flatten);

        if(mZoomHelper==null){
            mZoomHelper=ZoomHelper.build(mCamera, parameters);
        }else{
            mZoomHelper.setParameters(parameters);
            mZoomHelper.setCamera(mCamera);
        }
        mZoomHelper.init();
        _setFlashMode(mFlashModel);

        mCamera.setParameters(parameters);

    }

    /**
     * 是否支持预览编码
     * @param previewFormat {@link ImageFormat#JPEG ...}
     * @return
     */
    private boolean supportPreviewFormat(int previewFormat){
        Camera.Parameters parameters = _getParameters();
        if(parameters!=null){
            List<Integer> formats = parameters.getSupportedPreviewFormats();
            if(formats!=null && formats.size()>0){
                for(Integer format:formats){
                    if(format ==previewFormat){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 设置预览图片的格式
     * @param previewFormat
     * @return
     */
    public CameraManager setPreviewFormat(int previewFormat) {
        this.mPreviewFormat = previewFormat;
        return this;
    }


    /**
     * 设置闪光灯模式
     * @param flashMode
     */
    public void setFlashMode(String flashMode){
        mFlashModel=flashMode;
        _setFlashMode(mFlashModel);
    }

    /**
     * 设置闪光灯模式
     * @param flashMode
     */
    private void _setFlashMode(String flashMode){
        Camera.Parameters parameters = _getParameters();
        if(parameters!=null && mCamera!=null && supportFlashMode(flashMode)){
            parameters.setFlashMode(flashMode);
            mCamera.setParameters(parameters);
        }
    }

    /**
     * 是否支持指定的闪光灯模式
     * @param flashMode
     * @return
     */
    private boolean supportFlashMode(String flashMode){
        Camera.Parameters parameters = _getParameters();
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
     * 获取Camera.Parameters 要使用Camera.Parameters请务必用此方法获取
     * @return Camera.Parameters
     */
    private Camera.Parameters _getParameters(){
        if(mParameters==null && mCamera!=null){
            mParameters=mCamera.getParameters();
        }

        return mParameters;
    }



    /**
     * 是否支持对焦模式
     *
     * @param focusMode
     * @return
     */
    private boolean supportFocus(String focusMode) {
        Camera.Parameters parameters = _getParameters();
        if(parameters==null){
            return false;
        }
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if(supportedFocusModes==null){
            return false;
        }
        return supportedFocusModes.contains(focusMode);
    }

    /**
     * 设置预览窗口
     * @param surfaceView
     * @return
     */
    public CameraManager setSurfaceView(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
        return this;
    }

    /**
     * 设置预览回掉
     * @param previewCallback
     * @return
     */
    public CameraManager setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
        return this;
    }

    /**
     * 设置预览尺寸
     * @param preWidth
     * @param preHeight
     * @return
     */
    public CameraManager setPreSize(int preWidth, int preHeight) {
        Log.d(TAG, "setPreSize:preWidth="+preWidth+",preHeight="+preHeight);
        if(preWidth>0 && preHeight>0 ){
            this.preWidth = preWidth;
            this.preHeight = preHeight;
        }

        return this;
    }

    /**
     * 设置图片尺寸
     * @param picWidth
     * @param picHeight
     * @return
     */
    public CameraManager setPicSize(int picWidth, int picHeight) {
        Log.d(TAG, "setPicSize:picWidth="+picWidth+",picHeight="+picHeight);
        if(picWidth>0 && picHeight>0 ){
            this.picWidth = picWidth;
            this.picHeight = picHeight;
        }
        return this;

    }

    /**
     * 设置使用哪个摄像头
     * @param cameraFacing {@link Camera.CameraInfo#CAMERA_FACING_BACK,Camera.CameraInfo#CAMERA_FACING_FRONT}
     * @return
     */
    public CameraManager setCameraFacing(int cameraFacing) {
        mCameraFacing = cameraFacing;
        return this;
    }

    /**
     * 获取当前摄像头的方向
     * @return
     */
    public int getCameraFacing() {
        return mCameraFacing;
    }

    /**
     * 设置拍照回掉
     * @param captureCallback
     */
    public CameraManager setCaptureCallback(CaptureCallback captureCallback) {
        this.mCaptureCallback = captureCallback;
        return this;
    }

    public ZoomHelper getZoomHelper(){
        return mZoomHelper;
    }

    /**
     * 拍照后是否继续预览
     * @return
     */
    public boolean isContinuePreView() {
        return continuePreView;
    }

    /**
     * 设置拍照后是否继续预览
     * @param continuePreView
     */
    public CameraManager setContinuePreView(boolean continuePreView) {
        this.continuePreView = continuePreView;
        return this;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public boolean isMirror() {
        return mirror;
    }

    public CameraManager setMirror(boolean mirror) {
        this.mirror = mirror;
        return this;
    }
}
