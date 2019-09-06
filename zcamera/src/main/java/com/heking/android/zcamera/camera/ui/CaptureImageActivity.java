package com.heking.android.zcamera.camera.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.heking.android.zcamera.R;
import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.camera.CaptureCallback;
import com.heking.android.zcamera.camera.Utils.SpUtil;
import com.heking.android.zcamera.exceptions.CameraError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 拍照
 */
public class CaptureImageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CaptureImageActivity";
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

    private CameraManager mCameraManager;


    private int picWidth = 1080;
    private int picHeight = 1920;

    public static final String KEY_FILE_PATH = "key_file_path";
    public static final String KEY_PIC_WIDTH = "key_pic_width";
    public static final String KEY_PIC_HEIGHT = "key_pic_height";

    private String keepPath = null;
    private String mFlashMode;
    private int mCameraFacing;

    private List<String> flashList = new ArrayList<>();

    private volatile boolean initFlashList = false;
    private int flashIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zcamera_activity_capture_image);
        initData();
        findViews();
        initCamera();
    }

    public static Intent getStartIntent(Context context, String keepPath, int picWidth, int picHeight) {
        Intent intent = new Intent(context, CaptureImageActivity.class);
        intent.putExtra(CaptureImageActivity.KEY_FILE_PATH, keepPath);
        intent.putExtra(CaptureImageActivity.KEY_PIC_WIDTH, picWidth);
        intent.putExtra(CaptureImageActivity.KEY_PIC_HEIGHT, picHeight);
        return intent;
    }

    /**
     * 获取intent中的数据
     */
    private void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            keepPath = intent.getStringExtra(KEY_FILE_PATH);
            int picWidth1 = intent.getIntExtra(KEY_PIC_WIDTH, 1080);
            int picHeight1 = intent.getIntExtra(KEY_PIC_HEIGHT, 1920);
            picWidth = picWidth1 <= 0 ? 1080 : picWidth1;
            picHeight = picHeight1 <= 0 ? 1920 : picHeight1;
            Log.d(TAG, "intentData:keepPath=" + keepPath);
            Log.d(TAG, "intentData:picWidth=" + picWidth + ",picHeight=" + picHeight);
        }

        mCameraFacing = SpUtil.getCameraFacing(this);
        mFlashMode = SpUtil.getFlashMode(this);
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
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        if (mCameraManager == null) {
            mCameraManager = new CameraManager();
            surfaceView.setCameraManager(mCameraManager);
            mCameraManager.setCameraFacing(mCameraFacing)
                    .setSurfaceView(surfaceView)
                    .setContinuePreView(false)
                    .setPicSize(picWidth, picHeight)
                    .setPreSize(1080, 1920);
            mCameraManager.setCameraFacing(mCameraFacing);
            /*if(mFlashMode!=null){
                mCameraManager.setFlashMode(mFlashMode);
            }*/

            mCameraManager.setCaptureCallback(new CaptureCallback() {
                @Override
                public void onResult(int code, String path) {
                    if (code == CaptureCallback.SUCCESS) {
                        keepPath = path;
                        setCacheImage();
                        setUiAfterCapture(true);
                    } else if (code == CaptureCallback.FAILURE) {
                        toast("拍照失败");
                    }

                }
            });
            //预览回掉
            mCameraManager.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    //初始化闪光灯模式集合
                    if (!initFlashList) {
                        Log.d(TAG, "onPreviewFrame:初始化闪光灯模式集合");
                        initFlashList = true;
                        flashList.clear();
                        Camera.Parameters parameters = camera.getParameters();
                        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
                        if(supportedFlashModes==null){
                            return;
                        }
                        if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                            flashList.add(Camera.Parameters.FLASH_MODE_AUTO);
                            Log.d(TAG, "onPreviewFrame:flash支持自动");
                        }
                        if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                            flashList.add(Camera.Parameters.FLASH_MODE_ON);
                            Log.d(TAG, "onPreviewFrame:flash支持开启");
                        }
                        if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                            flashList.add(Camera.Parameters.FLASH_MODE_OFF);
                            Log.d(TAG, "onPreviewFrame:flash支持关闭");
                        }
                        //设置模式
                        if (mFlashMode != null && flashList.size() > 0) {
                            //-1不支持
                            flashIndex = flashList.indexOf(mFlashMode);
                            if (flashIndex < 0) {
                                flashIndex = 0;
                            }
                            setFlashMode(flashIndex);
                        }
                    }

                }
            });
        }

        try {
            mCameraManager.init();
        } catch (CameraError cameraError) {
            cameraError.printStackTrace();
        }
    }


    /**
     * 设置闪光灯模式
     */
    private void setFlashMode(int index) {
        if (flashList != null && flashList.size() > 0) {
            flashIndex = index;
            if (flashIndex >= flashList.size() || flashIndex < 0) {
                flashIndex = 0;
            }
            mFlashMode = flashList.get(flashIndex);
            mCameraManager.setFlashMode(mFlashMode);
            setTvFlash();
            SpUtil.keepFlashMode(this, mFlashMode);
            Log.d(TAG, "setFlashMode:"+mFlashMode);
        }else{
            toast("没有支持的模式");
        }
    }

    private void setTvFlash() {
        if (Camera.Parameters.FLASH_MODE_AUTO.equals(mFlashMode)) {
            tvFlash.setText("自动");
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mFlashMode)) {
            tvFlash.setText("打开");
        } else if (Camera.Parameters.FLASH_MODE_OFF.equals(mFlashMode)) {
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


    /**
     * 设置图片
     */
    private void setCacheImage() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        if (keepPath != null && ivImage != null) {
            //按照宽高压缩
            BitmapFactory.Options options = new BitmapFactory.Options();
            //设置为true 解码器将返回null，只是在Options对象中存入了图片的参数
            options.inJustDecodeBounds = true;
            //拿到图片的真实像素
            BitmapFactory.decodeFile(keepPath, options);
            Log.i(TAG, "decodeFile: 压缩前:width+" + options.outWidth + ",height=" + options.outHeight);
            int widthScale = 1;//缩进比例
            int heightScale = 1;
            if (width > 0) {
                widthScale = Math.round((float) options.outWidth / (float) width);
            }
            if (height > 0) {
                heightScale = Math.round((float) options.outHeight / (float) height);
            }
            int inSampleSize = widthScale > heightScale ? heightScale : widthScale;
            Log.i(TAG, "decodeFile: 计算出的inSampleSize=" + inSampleSize);

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize > 1 ? inSampleSize : 1;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(keepPath, options);
            ivImage.setImageBitmap(bitmap);
        }

    }

    /**
     * 完成拍照,
     */
    private void captureResult() {
        Intent intent = new Intent();
        intent.putExtra(KEY_FILE_PATH, keepPath);
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_flush) {
            setFlashMode(++flashIndex);

        } else if (id == R.id.iv_change_camera) {
            //切换摄像头
            if (mCameraManager != null) {
                mCameraManager.changeCamera();
                flashList.clear();
                initFlashList=false;
                mCameraFacing = mCameraManager.getCameraFacing();
                SpUtil.keepCameraFacing(this, mCameraFacing);
            }


        } else if (id == R.id.tv_capture) {
            //拍照
            mCameraManager.captureImage(keepPath);

        } else if (id == R.id.iv_cancel) {
            //取消上次的拍摄,清除图片
            if (keepPath != null) {
                File file = new File(keepPath);
                if (file.exists()) {
                    boolean delete = file.delete();
                }
            }
            //开始预览
            if (mCameraManager != null) {
                mCameraManager.startPreview();
                setUiAfterCapture(false);
            }
        } else if (id == R.id.iv_ok) {
            captureResult();
        }
    }

    @Override
    public void onBackPressed() {
        if (uiCaptureFinish != null && uiCaptureFinish.getVisibility() == View.VISIBLE) {
            mCameraManager.startPreview();
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
