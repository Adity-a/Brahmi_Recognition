package com.ocr.brahmi.translation;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class ScannerActivity extends AppCompatActivity {

    ImageView camera_btn, gallery_btn, extract, page_btn;
    int GALLERY_SELECT_CODE = 100;
    int CAMERA_SELECT_CODE = 200;
    int CAMERA_PERMISSION_CODE = 300;
    Uri imageUri;
    ContentValues values;
    TessBaseAPI tessBaseAPI;
    String tessDataPath;

    ActivityResultLauncher<String> mGetContent;
    ActivityResultLauncher<Uri> mGetImage;
    public final static String MESSAGE_KEY = "dataFrom.sendData.message_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        extract = findViewById(R.id.extract);
        camera_btn = findViewById(R.id.camera_btn);
        page_btn = findViewById(R.id.page_btn);

        tessDataPath = getFilesDir() + "/tesseract/";
        tessBaseAPI = new TessBaseAPI();
        copyTrainedData();
        tessBaseAPI.init(tessDataPath, "brahmi");
        camera_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capture();
            }
        });
        page_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ScannerActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        gallery_btn = findViewById(R.id.gallery_btn);
        gallery_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGetContent.launch("image/*");
            }
        });

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri selectedImageUri) {
                Intent intent = new Intent(ScannerActivity.this, CropperActivity.class);
                intent.putExtra("imageUri", selectedImageUri.toString());
                startActivityForResult(intent, GALLERY_SELECT_CODE);
            }
        });
    }

    private void copyTrainedData() {
        try {
            // Create the directory if it doesn't exist
            File dir = new File(tessDataPath + "tessdata/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Copy the trained data file from assets to the device's filesystem
            String trainedDataName = "brahmi.traineddata";
            InputStream in = getAssets().open("tessdata/" + trainedDataName);
            OutputStream out = new FileOutputStream(tessDataPath + "tessdata/" + trainedDataName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void capture() {
        if (ContextCompat.checkSelfPermission(ScannerActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ScannerActivity.this, "Please Give the Permission to Use Camera", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(ScannerActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
                    values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "New Picture");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                    imageUri = getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CAMERA_SELECT_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode==GALLERY_SELECT_CODE) {

            String finalImage = data.getStringExtra("croppedUri");
            Uri croppedImageUri = null;
            if (finalImage != null) {
                croppedImageUri = Uri.parse(finalImage);
            }
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), croppedImageUri);
                //extract.setImageBitmap(greyScaleConversion(bitmap));
                extractText(bitmap);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        } else if (resultCode == RESULT_OK && requestCode == CAMERA_SELECT_CODE) {

            cropData();

            /*String finalImage = data.getStringExtra("croppedUri");
            Uri croppedImageUri = null;
            if (finalImage != null) {
                croppedImageUri = Uri.parse(finalImage);
            }
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), croppedImageUri);
                //extract.setImageBitmap(greyScaleConversion(bitmap));
                extractText(bitmap);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/
        }
    }

    private void cropData() {

        Toast.makeText(this, "Uri: " + imageUri.toString(), Toast.LENGTH_SHORT).show();
        //mGetImage.launch(imageUri);
        /*Intent intent = new Intent(ScannerActivity.this, CropperActivity.class);
        intent.putExtra("imageUri", imageUri.toString()); // Use a key like "imageUriCamera"
        startActivityForResult(intent, CAMERA_SELECT_CODE);*/
    }

    public static Bitmap greyScaleConversion(Bitmap inputBitmap) {
        // Step 1: Grayscale Conversion
        Bitmap grayBitmap = Bitmap.createBitmap(inputBitmap.getWidth(), inputBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Remove color (convert to grayscale)
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(inputBitmap, 0, 0, paint);

        // Step 2: Binarization (Thresholding)
        Bitmap binaryBitmap = Bitmap.createBitmap(grayBitmap.getWidth(), grayBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        for (int x = 0; x < grayBitmap.getWidth(); x++) {
            for (int y = 0; y < grayBitmap.getHeight(); y++) {
                int pixel = grayBitmap.getPixel(x, y);
                int grayValue = Color.red(pixel); // All RGB components are equal in grayscale
                if (grayValue > 128) {
                    binaryBitmap.setPixel(x, y, Color.WHITE); // Apply threshold
                } else {
                    binaryBitmap.setPixel(x, y, Color.BLACK);
                }
            }
        }

        // Step 3: Noise Reduction (Smoothing with Simple Blur)
        Bitmap blurredBitmap = Bitmap.createBitmap(binaryBitmap);
        Canvas blurredCanvas = new Canvas(blurredBitmap);
        Paint blurPaint = new Paint();
        blurPaint.setAntiAlias(true);
        blurPaint.setFilterBitmap(true);
        blurPaint.setDither(true);
        blurredCanvas.drawBitmap(binaryBitmap, 0, 0, blurPaint);

        // Step 4: Sharpening (Unsharp Mask Approximation)
        Bitmap sharpenedBitmap = Bitmap.createBitmap(blurredBitmap.getWidth(), blurredBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        for (int x = 1; x < blurredBitmap.getWidth() - 1; x++) {
            for (int y = 1; y < blurredBitmap.getHeight() - 1; y++) {
                int centerPixel = blurredBitmap.getPixel(x, y);
                int topPixel = blurredBitmap.getPixel(x, y - 1);
                int bottomPixel = blurredBitmap.getPixel(x, y + 1);
                int leftPixel = blurredBitmap.getPixel(x - 1, y);
                int rightPixel = blurredBitmap.getPixel(x + 1, y);

                int sharpRed = Math.min(Math.max((5 * Color.red(centerPixel)) -
                        Color.red(topPixel) - Color.red(bottomPixel) -
                        Color.red(leftPixel) - Color.red(rightPixel), 0), 255);

                sharpenedBitmap.setPixel(x, y, Color.rgb(sharpRed, sharpRed, sharpRed));
            }
        }

        return sharpenedBitmap;
    }



    private void extractText(Bitmap bitmap) throws IOException {
        tessBaseAPI.setImage(bitmap);

        // Get extracted text
        String extractedText = tessBaseAPI.getUTF8Text();
        //Toast.makeText(this, "Extracted Text: " + extractedText, Toast.LENGTH_LONG).show();

        //Send the extracted text to MainActivity
        Intent intent= new Intent(this ,MainActivity.class);
        intent.putExtra(MESSAGE_KEY,extractedText);
        startActivity(intent);

    }

    @Override
        public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults){
            if (requestCode == CAMERA_PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    capture();
                } else {
                    Toast.makeText(this, "Permission Denied From User", Toast.LENGTH_SHORT).show();
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true); // Move the task containing this activity to the background
    }

    @Override
    protected void onDestroy() {
        // Release Tesseract resources
        if (tessBaseAPI != null) {
            tessBaseAPI.end();
        }
        super.onDestroy();
    }
}