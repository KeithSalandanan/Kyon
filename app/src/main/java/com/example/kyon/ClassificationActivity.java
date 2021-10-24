package com.example.kyon;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class ClassificationActivity extends AppCompatActivity {

    private ImageView ImgCaptured;
    private Uri myUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classification);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        ImgCaptured = findViewById(R.id.ImgCaptured);

        String image_path;
        Bundle extras = getIntent().getExtras();
        if(extras == null){
            image_path = "No uri received";
        }else{
            image_path= extras.getString("imagePath");
            myUri = Uri.parse(image_path);

        }


        ImgCaptured.setImageURI(myUri);
    }
}