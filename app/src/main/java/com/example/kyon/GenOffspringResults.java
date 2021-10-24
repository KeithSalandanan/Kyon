package com.example.kyon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class GenOffspringResults extends AppCompatActivity {
    private Button confirmButton;
    private ImageView dogImage1;
    private ImageView dogImage2;

    private Uri uriImage1;
    private Uri uriImage2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gen_offspring_results);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        confirmButton = findViewById(R.id.btnOk);
        dogImage1 = findViewById(R.id.LoadImg1);
        dogImage2 = findViewById(R.id.LoadImg2);




        loadImages();

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackToHome();
            }
        });
    }

    private void loadImages() {
        String image_path1, image_path2;
        Bundle extras = getIntent().getExtras();

        if(extras == null){
            Toast.makeText(this, "No Image Received", Toast.LENGTH_SHORT).show();
        }else{
            image_path1 = extras.getString("selectedImage1");
            image_path2 = extras.getString("selectedImage2");

            uriImage1 = Uri.parse(image_path1);
            uriImage2 = Uri.parse(image_path2);

            dogImage1.setImageURI(uriImage1);
            dogImage2.setImageURI(uriImage2);

        }

    }

    private void BackToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}