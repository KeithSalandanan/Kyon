package com.example.kyon;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    //Permission
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"};

    //Button and views
    private PreviewView mPreviewView;
    private Button mTakePictureButton;
    private SeekBar mZoomSlider;
    private TextView mZoomTextView;
    private ToggleButton mSwitchCameraButton;
    private Button mCloseButton;
    private Button mGalleryButton;
    private TextView mSignalText;

    //variables to store config states
    private String mPhotoPath;
    private int mSwitchCameraState = CameraSelector.LENS_FACING_BACK;
    private float mZoomState = 0.0f;
    private Uri mUri;

    //Camera variable for image capture
    private ImageCapture mImageCapture;
    private Uri selectImage;

    //number of shown detected objects
    private int NUMBER_DETECTED_OBJECTS = 1;





    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        //hide action und status bar, transparent navigation bar
//        getSupportActionBar().hide();
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);




        mPreviewView = findViewById(R.id.viewFinder);
        mTakePictureButton = findViewById(R.id.takePictureButton);
        mZoomSlider = findViewById(R.id.zoomSliderSeekBar);
        mSwitchCameraButton = findViewById(R.id.switchCameraButton);
        mZoomTextView = findViewById(R.id.zoomTextView);
        mCloseButton = findViewById(R.id.btn_close);
        mGalleryButton = findViewById(R.id.openGalleryButton);
        mSignalText = findViewById(R.id.SignalText);

        //initiate zoom level otherwise is not in the foreground -> bug?
        mZoomTextView.setText(mZoomState + "x");
        mZoomTextView.setVisibility(View.INVISIBLE);

        if (allPermissionsGranted()) {
            startCamera(); //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Setup the listener for take photos and switch camera
        mTakePictureButton.setOnClickListener(view -> takePhoto());
        mSwitchCameraButton.setOnClickListener(view -> switchCamera());
        mCloseButton.setOnClickListener(view -> closeCamera());
        mGalleryButton.setOnClickListener(view -> openGallery());
    }

    private void closeCamera() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //save value if screen orientation change and activity restarts
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("photoPath", mPhotoPath);
        outState.putInt("cameraState", mSwitchCameraState);
        outState.putFloat("zoomState", mZoomState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mPhotoPath = savedInstanceState.getString("photoPath");
        mSwitchCameraState = savedInstanceState.getInt("cameraState");
        mZoomState = savedInstanceState.getFloat("zoomState");
        super.onRestoreInstanceState(savedInstanceState);
    }


    private void startCamera() {
        ListenableFuture cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(mSwitchCameraState)
                        .build();

                // Set up the capture use case to allow users to take photos
                mImageCapture = new ImageCapture.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        //.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                //.setTargetResolution(new Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                ImageAnalyzerTensorflow imageAnalyzerTFLite = new ImageAnalyzerTensorflow(this);
                Log.d("CHECKPOINT", "IMAGEANALYZER TFLITE REACHED");
                imageAnalysis.setAnalyzer(Runnable::run, new ImageAnalysis.Analyzer() {
                    @SuppressLint("UnsafeExperimentalUsageError")
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        imageAnalyzerTFLite.analyzeImage(image);
                        Log.d("CHECKPOINT", "ANALYZE CHECKPOINT REACHED");

                        RectF location = imageAnalyzerTFLite.getDetectLocation();
                        String label = imageAnalyzerTFLite.getDetectLabel();
                        Float confidence = imageAnalyzerTFLite.getDetectConfidence();

                        Log.d("CHECKPOINT", "CHECKING BOUNDING BOX");


                        BoundingBox boundingBox = new BoundingBox(CameraActivity.this, CameraActivity.this);

                        if (location!=null) {
                            Log.d("CHECKPOINT", "LOCATION IS NOT EMPTY");
                            boundingBox.drawSingleBox(label, confidence, location);

                            runOnUiThread(() -> mSignalText.setText("Take a Picture"));

                            runOnUiThread(() ->mTakePictureButton.setBackground(ContextCompat.getDrawable(CameraActivity.this, R.drawable.selector_capture)));
                            mTakePictureButton.setEnabled(true);

                        } else {
                            Log.d("CHECKPOINT", "LOCATION IS EMPTY");
                            boundingBox.noBoundingBox();
                            runOnUiThread(() -> mSignalText.setText("No dogs detected"));
                            runOnUiThread(() ->mTakePictureButton.setBackground(ContextCompat.getDrawable(CameraActivity.this, R.drawable.shape_button_pressed)));
                            mTakePictureButton.setEnabled(false);
                        }

                        //clear stream for next image
                        image.close();
                    }
                });


                // Unbind use cases before rebinding
                cameraProvider.unbindAll();
                // Bind use cases to camera
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        mImageCapture,
                        imageAnalysis
                );

                //control zoom
                CameraControl cameraControl = camera.getCameraControl();
                controlZoom(cameraControl);

                preview.setSurfaceProvider(mPreviewView.createSurfaceProvider(camera.getCameraInfo()));

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get() should
                // not block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }


    //take a photo and save it on media storage --> add android:requestLegacyExternalStorage="true" in manifest
    private void takePhoto() {
        // Create file
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/CameraX", System.currentTimeMillis() + ".jpg");

        //File file = new File(System.currentTimeMillis()+".png");
        ImageCapture.OutputFileOptions.Builder outputFileOptionsBuilder = new ImageCapture.OutputFileOptions.Builder(file);

        Intent intent = new Intent(this, ClassificationActivity.class);
        mImageCapture.takePicture(outputFileOptionsBuilder.build(), Runnable::run, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                mUri = Uri.fromFile(file);
                runOnUiThread(()->Toast.makeText(CameraActivity.this, "Photo Captured", Toast.LENGTH_SHORT).show());

                runOnUiThread(() -> intent.putExtra("imagePath", mUri.toString()));

                runOnUiThread(()-> startActivity(intent));


            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                //runOnUiThread(()->Toast.makeText(CameraActivity.this, "Photo Not Captured", Toast.LENGTH_SHORT).show());
                exception.printStackTrace();

            }
        });

    }

    //Opens Gallery
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,3);
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,requestCode,data);
        if(resultCode == RESULT_OK && data!=null){
            selectImage = data.getData();

            ImageAnalyzerTensorflow tfAnalyze = new ImageAnalyzerTensorflow(this);
            if(tfAnalyze.detectDog(selectImage)){
                Toast.makeText(this, "Dogs Detected", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, ClassificationActivity.class);
                intent.putExtra("imagePath", selectImage.toString());
                startActivity(intent);
            }
            else{
                Toast.makeText(this, "No Dogs Detected", Toast.LENGTH_LONG).show();
            }

        }
    }


    //switch between front and back camera
    private void switchCamera() {
        if (!mSwitchCameraButton.isChecked()) {
            mSwitchCameraState = CameraSelector.LENS_FACING_BACK;
            startCamera();
        } else {
            mSwitchCameraState = CameraSelector.LENS_FACING_FRONT;
            startCamera();
        }
    }

    //control zoom doesn't work on emulator
    private void controlZoom(CameraControl cameraControl) {
        cameraControl.setLinearZoom(mZoomState);
        mZoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mZoomState = i/ Float.parseFloat("4");
                cameraControl.setLinearZoom(mZoomState);
                mZoomTextView.setText(mZoomState * 8 + "x");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mZoomTextView.setVisibility(View.VISIBLE);
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                mZoomTextView.postDelayed(() -> mZoomTextView.setVisibility(View.INVISIBLE), 3000);
            }
        });
    }

    //Permission for Camera and Storage
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


}