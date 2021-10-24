package com.example.kyon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActivityChooserView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class GenOffspringActivity extends AppCompatActivity {
    private ImageView selectImage1;
    private ImageView selectImage2;
    private Button btnGenerate;

    //check each image selector has image
    public Boolean firstImg = false;
    public Boolean secondImg = false;

    //photo uri
    public Uri uriImage1;
    public Uri uriImage2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gen_offspring);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        selectImage1 = findViewById(R.id.selectImg1);
        selectImage2 = findViewById(R.id.selectImg2);
        btnGenerate = findViewById(R.id.btnGenerate);





        selectImage1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,3);
            }
        });

        selectImage2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intent,3);
                secondImg = true;
            }
        });

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //For Temporary
                openResults();
            }
        });

    }


    //show Results
    private void openResults() {

        if(firstImg == true && secondImg ==  true){
            Intent intent = new Intent(this, GenOffspringResults.class);
            intent.putExtra("selectedImage1", uriImage1.toString());
            intent.putExtra("selectedImage2", uriImage2.toString());
            startActivity(intent);
        }else{
            Toast.makeText(this, "Please insert two images", Toast.LENGTH_SHORT).show();
        }

    }


    //to generate image after selecting from gallery
    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,requestCode,data);
        if(resultCode == RESULT_OK && data!=null && firstImg == false){

            uriImage1 = data.getData();
            selectImage1.setImageURI(uriImage1);
            firstImg = Boolean.TRUE;
        }

        if(resultCode == RESULT_OK && data!=null &&  secondImg == true){

            uriImage2 = data.getData();
            selectImage2.setImageURI(uriImage2);
        }
    }
}