package com.heking.zcamera;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.heking.android.zcamera.camera.ui.CaptureImageActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button btn1;
    private ImageView imageView;
    private int REQUEST_CAMERA_IMAGE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

    }

    private void findViews() {
        btn1 = findViewById(R.id.btn1);
        imageView = findViewById(R.id.iv_pic);

        addListener();
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
