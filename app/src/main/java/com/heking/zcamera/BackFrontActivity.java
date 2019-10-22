package com.heking.zcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.heking.android.zcamera.CameraEventCallback;
import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.camera.CaptureCallback;

public class BackFrontActivity extends AppCompatActivity {
    private static final String TAG = "BackFrontActivity";

    private SurfaceView surfaceView;
    private ImageView imageView;

    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_front);

        findViews();

    }

    private void findViews() {
        surfaceView = findViewById(R.id.surfaceView);
        imageView = findViewById(R.id.imageView);

        addListener();

        initCamera();
    }

    private boolean two = false;

    private String path1;
    private String path2;

    private void initCamera() {
        cameraManager = new CameraManager();
        cameraManager.setCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        cameraManager.setSurfaceView(surfaceView);
        cameraManager.setMirror(true);
        cameraManager.setCaptureCallback((code, path) -> {
            if (code == CaptureCallback.SUCCESS) {
                if (!two) {
                    cameraManager.changeCamera();
                    path1 = path;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                    imageView.setImageBitmap(bitmap);
                    two = true;
                } else {
                    path2 = path;
                    BitmapFactory.Options options1 = new BitmapFactory.Options();
                    options1.inSampleSize = 1;
                    Bitmap bitmap1 = BitmapFactory.decodeFile(path1, options1);

                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    options2.inSampleSize = 4;
                    Bitmap bitmap2 = BitmapFactory.decodeFile(path2, options2);
                    Bitmap bitmap = drawBitmap(bitmap1, bitmap2);
                    imageView.setImageBitmap(bitmap);


                }
            }

        });

        cameraManager.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                 Toast.makeText(BackFrontActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenCameraSuccess(android.graphics.Camera camera) {

            }
        });
        cameraManager.init();

    }


    private Bitmap drawBitmap(Bitmap bitmap1, Bitmap bitmap2) {
        Log.d(TAG, "drawBitmap:bitmap1:" + bitmap1.getWidth() + "," + bitmap1.getHeight());
        Log.d(TAG, "drawBitmap:bitmap2:" + bitmap2.getWidth() + "," + bitmap2.getHeight());
        Bitmap bitmap = Bitmap.createBitmap(bitmap1.getWidth(), bitmap1.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);


        canvas.drawBitmap(bitmap1, 0, 0, null);

        //要绘制的Bitmap对象的矩形区域
        Rect rect = new Rect(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
        //要将bitmap绘制在屏幕的什么地方
        canvas.drawBitmap(bitmap2, rect, new RectF(200, 100, 300, 300), null);

        return bitmap;
    }


    private void addListener() {
        findViewById(R.id.btn1).setOnClickListener(v -> {
            cameraManager.captureImage();
        });
    }
}
