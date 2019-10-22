package com.heking.zcamera;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.heking.android.zcamera.CameraEventCallback;
import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.camera.ui.CaptureImageActivity;
import com.heking.android.zcamera.camera.ui.CaptureVideoActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button btn1;
    private ImageView imageView;
    private int REQUEST_CAMERA_IMAGE = 101;

    private SurfaceView surfaceView1;
    private SurfaceView surfaceView2;
    private CameraManager cameraManager1;
    private CameraManager cameraManager2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

    }

    private void findViews() {
        btn1 = findViewById(R.id.btn1);
        imageView = findViewById(R.id.iv_pic);
        surfaceView1 = findViewById(R.id.surfaceView1);
        surfaceView2 = findViewById(R.id.surfaceView2);
        surfaceView2.setVisibility(View.GONE);
        addListener();

        initCamera();

    }

    private void addListener() {
        btn1.setOnClickListener(v -> {
            File folder = Environment.getExternalStoragePublicDirectory("0image");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File(folder, "image_" + System.currentTimeMillis() + ".jpg");
            Intent startIntent = CaptureImageActivity.getStartIntent(MainActivity.this,
                    file.getAbsolutePath(),
                    1080, 1920);
            startActivityForResult(startIntent, REQUEST_CAMERA_IMAGE);

        });

        findViewById(R.id.btn2).setOnClickListener(v -> {
            Intent intent = new Intent(this, BackFrontActivity.class);
            startActivity(intent);

        });


        findViewById(R.id.btn3).setOnClickListener(v -> {
            Intent intent = new Intent(this, CaptureVideoActivity.class);
            startActivity(intent);

        });
    }

    private void initCamera() {
        cameraManager1 = new CameraManager();
        cameraManager1.setSurfaceView(surfaceView1);
        cameraManager1.setPreviewFormat(ImageFormat.JPEG);
        cameraManager1.setCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        cameraManager1.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenCameraSuccess(android.graphics.Camera camera) {

            }
        });

        cameraManager1.init();

        Log.d(TAG, "run:打开第二个相机");
        Toast.makeText(this, "打开第二个相机", Toast.LENGTH_SHORT).show();
        cameraManager2 = new CameraManager();
        cameraManager2.setSurfaceView(surfaceView2);
        cameraManager2.setPreviewFormat(ImageFormat.JPEG);
        cameraManager2.setCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        cameraManager2.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenCameraSuccess(android.graphics.Camera camera) {

            }
        });

        cameraManager2.init();
        surfaceView2.setVisibility(View.VISIBLE);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA_IMAGE) {
            if (data != null) {
                String path = data.getStringExtra(CaptureImageActivity.KEY_FILE_PATH);

                imageView.setImageBitmap(BitmapFactory.decodeFile(path));

                Toast.makeText(this, "" + path, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "取消", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
