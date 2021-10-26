package com.example.kyon;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAnalyzerTensorflow extends AppCompatActivity {

    private String detectionModelName = "Dog_Detector_metadata.tflite";
    private String assetLabelName = "label.txt";

    private String classifierModelName = "Dog_Detector_metadata.tflite";

    private boolean isQuant = false;

    //options
    private final Interpreter.Options tfLiteOptions = new Interpreter.Options();

    //Interpreter
    private List<String> labelList;
    private ByteBuffer imgData = null;

    // Only return this many results.
    private static final int NUM_DETECTIONS = 1;


    //depends on size model
    private int DIM_IMG_SIZE = 320;

    private float MIN_CONFIDENCE = 0.5f;

    private RectF detection = null;

    private Context mContext;

    public ImageAnalyzerTensorflow(Context context){
        mContext = context;

    }

    //Detection
    private final ObjectDetector.ObjectDetectorOptions detectorOptions= ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(1)
            .setScoreThreshold(0.5f)
            .build();
    ObjectDetector detector = null;
    List<org.tensorflow.lite.task.vision.detector.Detection> results =null;

    TensorImage tensorIm;



    //Classification
    ImageClassifier.ImageClassifierOptions classifierOptions = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(3)
            .build();
    ImageClassifier classifier =  null;
    List<Classifications> result;



    //NOT ARRAYS
    private String detectLabel;
    private Float detectConfidence;
    private RectF detectLocation;

    public RectF getDetectLocation() {return detectLocation;}
    public String getDetectLabel() {return detectLabel;}
    public Float getDetectConfidence() {return detectConfidence;}

    public void analyzeImage(ImageProxy imageProxy) {




        try {
            detector = ObjectDetector.createFromFileAndOptions(mContext, detectionModelName, detectorOptions);
            labelList = loadLabelList();
        } catch (IOException e) {
            e.printStackTrace();
        }


        @SuppressLint("UnsafeExperimentalUsageError") Image image = imageProxy.getImage();
        Bitmap bitmap_orig = toBitmap(image);
        Bitmap bitmap = getResizedBitmap(bitmap_orig, DIM_IMG_SIZE, DIM_IMG_SIZE);

        tensorIm = TensorImage.fromBitmap(bitmap);
        results = detector.detect(tensorIm);
        // run tfLite


        Log.d("CHECKPOINT", "BITMAP REACHED");
        readOutput();
    }

    private void readOutput() {
        Log.d("CHECKPOINT", "READ OUTPUT REACHED");

        detection = null;
        detectLocation = null;
        detectConfidence = null;
        detectLabel = null;

        if(results.size()!=0){
            Log.d("DETECTION", "hasssssssssssssssssssssssssss detection");
            String dog = labelList.get(0);


            Float resultScores = results.get(0).getCategories().get(0).getScore();
            String fScore =  Integer.toString((int)(resultScores*100));
            Log.d("DETECTION", dog + ": "+ fScore);


            final ArrayList<Detection.Recognition> recognitions = new ArrayList<>(1);
            detection =
                    new RectF(
                            results.get(0).getBoundingBox().left + DIM_IMG_SIZE,
                            results.get(0).getBoundingBox().top + DIM_IMG_SIZE,
                            results.get(0).getBoundingBox().right + DIM_IMG_SIZE,
                            results.get(0).getBoundingBox().bottom + DIM_IMG_SIZE);

            recognitions.add(new Detection.Recognition("" + 0, dog, resultScores, detection));

            for (final Detection.Recognition result : recognitions) {
                if (results.get(0).getBoundingBox() != null && result.getConfidence() >= MIN_CONFIDENCE) {
                    detectLocation = result.getLocation();
                    detectLabel = result.getTitle();
                    detectConfidence = result.getConfidence()*100;
                    Log.d("CHECKPOINT", "DETECTION SUCCESSFUL");
                }
            }
        }

    }


    private Bitmap getResizedBitmap(Bitmap bitmap_orig, int dim_img_size_x, int dim_img_size_y) {
        int width = bitmap_orig.getWidth();
        int height = bitmap_orig.getHeight();

        float scaleWidth =((float) dim_img_size_x) / width;
        float scaleHeigth = ((float) dim_img_size_y) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeigth);

        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            matrix.postRotate(90);
        } else {
            matrix.postRotate(0);
        }

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap_orig, 0, 0, width, height, matrix, false);

        return resizedBitmap;
    }

    private static Bitmap toBitmap(Image image) {
        Log.d("CHECKPOINT", "FUNCTION TO BITMAP");
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(mContext.getAssets().open(assetLabelName)));

        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


        public Boolean detectDog(Uri selectImage) {
        try {
            detector = ObjectDetector.createFromFileAndOptions(mContext, detectionModelName, detectorOptions);
            labelList = loadLabelList();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(),selectImage);
            results = detector.detect(TensorImage.fromBitmap(bitmap));

            if(!results.isEmpty()) {
                return true;
            }
            else {

                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

        public String classifyDog(Context context, Uri image) {
        String str;
        try {
            classifier = ImageClassifier.createFromFileAndOptions(context,classifierModelName, classifierOptions);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(),image);
            result = classifier.classify(TensorImage.fromBitmap(bitmap));
            str = "";
            for(Classifications c:result){
                for(Category cat :c.getCategories()) {
                    str += String.format("%.1f%s %s \n",cat.getScore()*100,"%",cat.getLabel());
                }
                return str;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


}










