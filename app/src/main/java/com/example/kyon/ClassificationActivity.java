package com.example.kyon;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.IOException;
import java.util.List;

public class ClassificationActivity extends AppCompatActivity {

    private ImageView ImgCaptured;
    private TextView imgTextResults;
    private Uri myUri;

    ImageClassifier.ImageClassifierOptions options = null;
    ImageClassifier classifier =  null;
    List<Classifications> result;
    Bitmap bitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classification);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);


        ImgCaptured = findViewById(R.id.ImgCaptured);
        imgTextResults = findViewById(R.id.txtResults);

        options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)
                .build();

        loadImage();

    }

    private void loadImage() {
        String image_path;
        Bundle extras = getIntent().getExtras();
        if(extras == null){
            Toast.makeText(this, "No URI Received", Toast.LENGTH_SHORT).show();
        }else{
            image_path= extras.getString("imagePath");

            myUri = Uri.parse(image_path);
            ImgCaptured.setImageURI(myUri);

            classifyDog(myUri);
        }



    }

    private void classifyDog(Uri image) {
        try {
            classifier = ImageClassifier.createFromFileAndOptions(this,"Dog_Classifier_metadata.tflite",options);
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),image);
            result = classifier.classify(TensorImage.fromBitmap(bitmap));
            String str = "";
            for(Classifications c:result){
                for(Category cat :c.getCategories()) {
                    str += String.format("%.1f%s %s \n",cat.getScore()*100,"%",cat.getLabel());
                }
                imgTextResults.setText(str);
            }
            Toast.makeText(this, "Nag Classify", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ayaw mag classify", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}