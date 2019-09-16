package com.heking.zcamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;

import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.exceptions.CameraError;
import com.zhoug.android.common.prodivers.ZFileProvider;
import com.zhoug.android.common.utils.BitmapUtils;
import com.zhoug.android.common.utils.FileUtils;
import com.zhoug.android.common.utils.IOUtils;
import com.zhoug.android.common.utils.IntentUtils;
import com.zhoug.android.common.utils.TimeUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "VideoActivity";

    private SurfaceView surfaceView;
    private Button btnStart, btnLook;
    private CameraManager cameraManager;
    private volatile boolean taking = false;
    private String path;

    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        findViews();
        initCamera();
    }

    private void findViews() {
        surfaceView = findViewById(R.id.surfaceView);
        btnStart = findViewById(R.id.btn_start);
        btnLook = findViewById(R.id.btn_look);

        addListener();
    }

    private void addListener() {
        btnStart.setOnClickListener(v -> {
            if (btnStart.getText().toString().equals("开始录像")) {
                taking = true;
                btnStart.setText("停止录像");
            } else {
                taking = false;
                btnStart.setText("开始录像");
            }

        });

        btnLook.setOnClickListener(v -> {
            Intent readFileIntent = IntentUtils.getReadFileIntent(this, path, "video/*");
            startActivity(readFileIntent);
        });

    }

    private void initCamera() {
        cameraManager = new CameraManager();
        cameraManager.setCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        cameraManager.setSurfaceView(surfaceView);
        cameraManager.setPreviewFormat(ImageFormat.NV21);
        cameraManager.setPreviewCallback((data, camera) -> {
            if (taking) {
                Log.d(TAG, "preview" + (++count) + ":" + (data.length / 1024) + "K");
                keepVideo(data,true);
                keepImage(data);
                taking = false;
            }

        });


        try {
            cameraManager.init();
        } catch (CameraError cameraError) {
            cameraError.printStackTrace();
        }
    }

    private void keepImage(byte[] data) {
        path = FileUtils.getExternalFile("0video", TimeUtils.getCurrentTime("yyyyMMddHHmmss") + ".jpg").getAbsolutePath();

        int w = cameraManager.getPreSize().width;
        int h = cameraManager.getPreSize().height;

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, w, h, null);
        Rect rect = new Rect(0, 0, w, h);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        boolean b = yuvImage.compressToJpeg(rect, 100, bos);
        if (b) {
            byte[] bytes = bos.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(cameraManager.getPreRotation());
            Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            IOUtils.keepFile(path, BitmapUtils.compressQuality(bitmap1, 100));

        }

    }

    private void keepVideo(byte[] data, boolean append) {
        if (path == null) {
            path = FileUtils.getExternalFile("0video", TimeUtils.getCurrentTime("yyyyMMddHHmmss") + ".jpg").getAbsolutePath();
        }
        FileOutputStream fos = null;
        File parentFile = new File(path).getParentFile();
        if (!parentFile.exists()) {
            boolean mkdirs = parentFile.mkdirs();
            parentFile = null;
        }
        try {
            fos = new FileOutputStream(path, append);
            fos.write(data, 0, data.length);
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
