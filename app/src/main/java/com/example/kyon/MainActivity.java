package com.example.kyon;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import soup.neumorphism.NeumorphCardView;

public class MainActivity extends AppCompatActivity {

    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        NeumorphCardView cardClassify= findViewById(R.id.cardClassifyDog);
        NeumorphCardView cardGenerate= findViewById(R.id.cardGenerateOffspring);



        cardClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasCameraPermission()) {
                    startClassify();
                } else {
                    requestPermission();
                }
            }
        });

        cardGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGenerate();
            }
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    private void startClassify() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void startGenerate() {
        Intent intent = new Intent(this, GenOffspringActivity.class);
        startActivity(intent);
    }

}