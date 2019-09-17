package com.heking.android.zcamera.camera.ui;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.heking.android.zcamera.R;
import com.heking.android.zcamera.camera.Utils.CameraUtil;
import com.heking.android.zcamera.camera.Utils.SpUtil;
import com.zhoug.android.common.utils.FileUtils;
import com.zhoug.android.common.utils.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.security.Policy;
import java.util.ArrayList;
import java.util.List;

/**
 * 拍照
 */
public class CaptureVideoActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private static final String TAG = "CaptureVideoActivity";
    private CameraSurfaceView surfaceView;
    private ImageView ivFlush;
    private ImageView ivChangeCamera;
    private ImageView ivCancel;
    private ImageView ivOk;
    private ImageView ivImage;

    private TextView tvCapture;
    private TextView tvFlash;
    private ViewGroup btnFlush;
    private ViewGroup uiCapture;
    private ViewGroup uiCaptureFinish;

    /**
     * 视频保存路径
     */
    private String keepPath = null;
    /**
     * 闪光灯模式
     */
    private String mFlashMode=Parameters.FLASH_MODE_OFF;

    /**
     * 摄像头方向
     */
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;

    /**
     * 相机
     */
    private Camera mCamera;


    /**
     * 相机id
     */
    private int mCameraId;

    private SurfaceHolder mSurfaceHolder;


    /**
     * 相机参数
     */
    private Parameters mParameters;

    private Camera.CameraInfo mCameraInfo;

    /**
     * 预览尺寸
     */
    private Camera.Size preSize;

    /**
     * 录制尺寸
     */
    private Camera.Size videoSize;

    private int width, height;

    /**
     * 预览方向
     */
    private int mPreRotation = 0;

    /**
     * 图片方向
     */
    private int mPicRotation = 0;

    /**
     * 正在录制
     */
    private boolean recording = false;

    private boolean clickable=true;



    /**
     * 录制视屏
     */
    private MediaRecorder mMediaRecorder;

    /**
     * 录制的最长时间 默认60秒
     */
    private int maxDuration = 60;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zcamera_activity_capture_video);
        initData();
        findViews();

    }


    /**
     * 获取intent中的数据
     */
    private void initData() {

    }

    private void findViews() {
        surfaceView = findViewById(R.id.surfaceView);
        ivFlush = findViewById(R.id.iv_flush);
        ivChangeCamera = findViewById(R.id.iv_change_camera);
        ivCancel = findViewById(R.id.iv_cancel);
        ivOk = findViewById(R.id.iv_ok);
        tvCapture = findViewById(R.id.tv_capture);
        btnFlush = findViewById(R.id.btn_flush);
        uiCapture = findViewById(R.id.ui_capture);
        uiCaptureFinish = findViewById(R.id.ui_capture_finish);
        ivImage = findViewById(R.id.iv_image);
        tvFlash = findViewById(R.id.tv_flush);


        addListener();
    }

    private void addListener() {
        btnFlush.setOnClickListener(this);
        ivChangeCamera.setOnClickListener(this);
        ivCancel.setOnClickListener(this);
        ivOk.setOnClickListener(this);
        tvCapture.setOnClickListener(this);
        ivImage.setOnClickListener(null);

        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(this);

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated:");
        mSurfaceHolder=holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged:[" + width + "," + height + "]");
        this.width = width;
        this.height = height;
        initCamera();
        CameraUtil.startPreview(mCamera);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
        mSurfaceHolder = null;

    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        mCameraId = CameraUtil.getCameraIdByFacing(mCameraFacing);
        if (mCameraId >= 0) {
            mCamera = Camera.open(mCameraId);
            mParameters = mCamera.getParameters();
            mCameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, mCameraInfo);
            initParams();
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            toast("没有找到相机mCameraFacing=" + mCameraFacing);
            Log.e(TAG, "没有找到相机mCameraFacing=" + mCameraFacing);
        }

    }

    private void initParams() {
        //预览方向
        mPreRotation = CameraUtil.getPreOrientation(this, mCameraInfo, mCameraFacing);
        mCamera.setDisplayOrientation(mPreRotation);
        //存储方向
        mPicRotation = CameraUtil.getPicRotation(mCameraInfo, mCameraFacing, 0);
        mParameters.setRotation(mPicRotation);

        //预览尺寸
        preSize = CameraUtil.getBestSize(width, height, mParameters.getSupportedPreviewSizes(), true);

        videoSize = CameraUtil.getBestSize(width, height, mParameters.getSupportedVideoSizes(), true);
        mParameters.setPreviewSize(preSize.width, preSize.height);

        //聚焦
        if (CameraUtil.supportFocus(mParameters, Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (CameraUtil.supportFocus(mParameters, Parameters.FOCUS_MODE_AUTO)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        } else {
            Log.e(TAG, "聚焦设置失败");
        }

        //设置闪光灯模式
        if(CameraUtil.supportFlashMode(mParameters,mFlashMode )){
            mParameters.setFlashMode(mFlashMode);
        }else{
            mFlashMode=Parameters.FLASH_MODE_OFF;
            setTvFlash();
        }

        //告诉相机需要录像
        mParameters.setRecordingHint(true);
        //设置预览帧
//        mParameters.setPreviewFpsRange(, );
        //是否支持影像稳定能力，支持则开启
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            if (mParameters.isVideoStabilizationSupported()){
                mParameters.setVideoStabilization(true);
            }
        }

        mCamera.setParameters(mParameters);
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if(mMediaRecorder!=null){
            stopRecord();
            mMediaRecorder.release();
            mMediaRecorder=null;
        }

        CameraUtil.releaseCamera(mCamera);
    }

    /**
     * 初始化
     */
    private void initMediaRecorder() {
        if (null == mMediaRecorder) {
            mMediaRecorder = new MediaRecorder();
        }else{
            mMediaRecorder.reset();
        }
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        Log.i(TAG, "开始配置MediaRecorder");
        //设置音视频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//麦克风
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//相机

        //设置视频输出格式
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        //音视频编码格式
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        //声道 1:单声道,2:立体音
        mMediaRecorder.setAudioChannels(1);


        //视频的帧率,决定视频的连贯度
        mMediaRecorder.setVideoFrameRate(30);

        //码率:数据传输时单位时间传送的数据位数，一般我们用的单位是kbps即千位每秒。通俗一点的理解就是取样率,1Byte=8bit
        // 决定清晰度
        mMediaRecorder.setVideoEncodingBitRate(300*1024*8);//每秒输出多大的文件流300k

        //录制视频的宽度和高度
        mMediaRecorder.setVideoSize(videoSize.width, videoSize.height);
        //设置最长录制时间
        mMediaRecorder.setMaxDuration(maxDuration * 1000);

        mMediaRecorder.setOrientationHint(mPreRotation);

        //质量等级对应于最高可用分辨率
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        //设置保存地址
        if (keepPath == null) {
            keepPath = getDefPath();
        }
        Log.d(TAG, "initMediaRecord:keepPath=" + keepPath);
        mMediaRecorder.setOutputFile(keepPath);
//        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.e(TAG, "onError:what="+what+",extra="+extra);
                stopRecord();
                mr.reset();

            }
        });

        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                Log.d(TAG, "onInfo:what="+what+",extra="+extra);

            }
        });
    }

    /**
     * 开始播放
     */
    private void startRecord(){
        if(mCamera==null || recording || !clickable){
            return;
        }
        clickable = false;
        try {
            initMediaRecorder();
            //准备配置
            mMediaRecorder.prepare();
            //开始录制
            Log.i(TAG, "开始录制");
            mMediaRecorder.start();
            tvCapture.setText("停止");
            recording = true;
            clickable = true;
        } catch (IOException e) {
            e.printStackTrace();
            recording = false;
            clickable = true;
            mMediaRecorder.reset();
            toast(e.getMessage());
        }

    }

    /**
     * 停止播放
     */
    private void stopRecord() {
        if (mMediaRecorder != null && recording && clickable) {
            try {
                clickable=false;

                mMediaRecorder.stop();
                mMediaRecorder.reset();
                tvCapture.setText("录像");
                Log.i(TAG, "停止录制");
                recording=false;
                clickable=true;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                recording=false;
                clickable=true;
                mMediaRecorder.reset();
                toast(e.getMessage());
            }
        }
    }



    /**
     * 默认储存地址
     *
     * @return
     */
    private String getDefPath() {
        String fileName = TimeUtils.getCurrentTime("yyyyMMddHHmmss") + ".mp4";
        File file = FileUtils.getExternalFile("0video", fileName);
        if (file != null) {
            return file.getAbsolutePath();
        } else {
            File cacheDir = getCacheDir();
            File folder = new File(cacheDir, "video");
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    return new File(folder, fileName).getAbsolutePath();
                }

            }
        }

        return null;
    }



    /**
     * 设置闪光灯模式
     */
    private void setFlashMode( ) {
        Log.d(TAG, "setFlashMode:mFlashMode="+mFlashMode);
        if(Parameters.FLASH_MODE_OFF.equals(mFlashMode)){
            if(CameraUtil.supportFlashMode(mParameters, Parameters.FLASH_MODE_ON)){
                mFlashMode=Parameters.FLASH_MODE_ON;
            }else{
                return;
            }
        }else{
            if(CameraUtil.supportFlashMode(mParameters, Parameters.FLASH_MODE_OFF)){
                mFlashMode=Parameters.FLASH_MODE_OFF;
            }else{
                return;
            }
        }

        mParameters.setFlashMode(mFlashMode);
        mCamera.setParameters(mParameters);
        setTvFlash();
        Log.d(TAG, "setFlashMode:" + mFlashMode);

    }

    private void setTvFlash() {
        if (Camera.Parameters.FLASH_MODE_AUTO.equals(mFlashMode)) {
            tvFlash.setText("自动");
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mFlashMode)) {
            tvFlash.setText("打开");
        } else if (Camera.Parameters.FLASH_MODE_OFF.equals(mFlashMode)) {
            tvFlash.setText("关闭");
        }else{
            tvFlash.setText("关闭");
        }
    }

    /**
     * 拍照过后改变显示的ui
     *
     * @param captureFinish
     */
    private void setUiAfterCapture(boolean captureFinish) {
        if (captureFinish) {
            if (uiCapture != null) {
                uiCapture.setVisibility(View.GONE);
            }
            if (uiCaptureFinish != null) {
                uiCaptureFinish.setVisibility(View.VISIBLE);
            }
        } else {
            if (uiCapture != null) {
                uiCapture.setVisibility(View.VISIBLE);
            }
            if (uiCaptureFinish != null) {
                uiCaptureFinish.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_flush) {
            setFlashMode();
        } else if (id == R.id.iv_change_camera) {
            //切换摄像头
            if (recording) {
                toast("正在录像,不能切换相机");
            } else {
                if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                } else {
                    mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                releaseCamera();
                initCamera();
                CameraUtil.startPreview(mCamera);
            }


        } else if (id == R.id.tv_capture) {
            //录像
            if(recording){
                stopRecord();
            }else{
                startRecord();
            }

        } else if (id == R.id.iv_cancel) {
            //取消上次的拍摄,清除文件
            if (keepPath != null) {
                File file = new File(keepPath);
                if (file.exists()) {
                    boolean delete = file.delete();
                }
            }
            //开始预览
            CameraUtil.startPreview(mCamera);

        } else if (id == R.id.iv_ok) {

        }
    }

    @Override
    public void onBackPressed() {
        if (uiCaptureFinish != null && uiCaptureFinish.getVisibility() == View.VISIBLE) {

            setUiAfterCapture(false);
            return;
        }
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume:");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause:");
    }

}
