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
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LaunchActivity extends AppCompatActivity {
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    PreviewView cameraFeed;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Executor cameraExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Portion where camera's view is displayed must be a PreviewView element.
        // Replace R.id.cameraFeed with the name of our PreviewView's name (ie R.id.elementName)
        cameraFeed = findViewById(R.id.cameraFeed);

        setContentView(R.layout.activity_main);
        // Uses helper functions to get user's permission to use device camera
        if (allPermissionsGranted()) {
            startCamera(); // Start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

    }

    public void bindPreview(ProcessCameraProvider cameraProvider) {
        // Code for setting PreviewView element to display the camera device
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(cameraFeed.getSurfaceProvider());

        // Code for sending captured image to next activity when user preses the capture button
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        // Function that directly connects the action of the button being clicked with an image
        // subsequently being captured and sent to the next activity
        // Replace R.id.capture_button with the name of our PreviewView's name (ie R.id.elementName)
        findViewById(R.id.capture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                    // The file format of the image captured is a ImageProxy
                    @Override
                    public void onCaptureSuccess(ImageProxy image) {
                        // Conver to bitmap
                        Bitmap bm = getBitMap(image);
                        // Store in temporary file path
                        String filePath= tempFileImage(LaunchActivity.this.getApplicationContext(), bm, "name");
                        Intent intent = new Intent(view.getContext(), MainActivity.class);
                        // Add path as a string to Intent
                        intent.putExtra("path", filePath);
                        startActivity(intent);
                        image.close();
                    }

                    // For handing errors, this should never be reached.
                    @Override
                    public void onError(ImageCaptureException exception) {
                        Log.e("Launch Activity", "ImageCaptureException");
                    }
                });

            }

        });
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);

    }
    // Helper functions for requesting permission for camera use
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //start camera when permissions have been granted otherwise exit app
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
        //check if req permissions have been granted
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Helper function for bindPreview()
    // Converts from ImageProxy to BitMap
    private Bitmap getBitMap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // Helper function for bindPreview()
    // Creates a temporary file and returns the absolute file path
    private static String tempFileImage(Context context, Bitmap bitmap, String name) {

        File outputDir = context.getCacheDir();
        File imageFile = new File(outputDir, name + ".jpg");

        OutputStream os;
        try {
            os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        } catch (Exception e) {
            Log.e(context.getClass().getSimpleName(), "Error writing file", e);
        }

        return imageFile.getAbsolutePath();
    }
}