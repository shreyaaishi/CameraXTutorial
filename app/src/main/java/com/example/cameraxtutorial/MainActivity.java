package com.example.cameraxtutorial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cameraxtutorial.ml.Model;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    Bitmap bmp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get file path from Intent and use it to retrieve Bitmap (image to analyze)
        Bundle extras = getIntent().getExtras();
        String filePath = extras.getString("path");
        File file = new File(filePath);
        bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
    }
    public void analyzeImage(View view) {
        try {
            // Converts bitmap captured by camera into a TensorImage
            TensorImage tfImage = new TensorImage(DataType.FLOAT32);
            tfImage.load(bmp);

            // Build ImageProcessor object
            ImageProcessor imageProcessor = new ImageProcessor.Builder().
                    add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR)).
                    build();
            tfImage = imageProcessor.process(tfImage);

            // Code copied directly from sample. To view it, open your .tflite within Android Studio
            // Make sure to import the correct Model class! Should be along the lines of something
            // like com.example.projectname.ml
            Model model = Model.newInstance(view.getContext());
            Model.Outputs outputs = model.process(tfImage);

        } catch(IOException e) {
            Log.e("MainActivity", "IOException");
        }
    }
}