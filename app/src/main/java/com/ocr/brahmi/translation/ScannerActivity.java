package com.ocr.brahmi.translation;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ScannerActivity extends AppCompatActivity {

    ImageView camera_btn, gallery_btn, extract, page_btn;
    int GALLERY_SELECT_CODE = 100;
    int CAMERA_SELECT_CODE = 200;
    int CAMERA_PERMISSION_CODE = 300;
    Uri imageUri;
    ContentValues values;
    TessBaseAPI tessBaseAPI;
    String tessDataPath;
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
                //capture();
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
                gallery();
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please Give the Permission to Use Camera", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
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
    private void gallery() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(i, "Gallery_Select_Code"), GALLERY_SELECT_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_SELECT_CODE) {
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(), selectedImageUri);
                        //Bitmap resizedImage = resizeImage(bitmap, newWidth, newHeight);
                        //extract.setImageBitmap(grayScaleConversion(bitmap));
                        //extract.setImageBitmap(bitmap);
                        //extractText(grayScaleConversion(bitmap));
                        extractText(bitmap);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (requestCode == CAMERA_SELECT_CODE) {
                try {
                    Bitmap thumbnail = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), imageUri);
                    if (imageUri != null) {
                        //extract.setImageBitmap(thumbnail);
                        //Bitmap resizedImage = resizeImage(thumbnail, newWidth, newHeight);
                        extract.setImageBitmap(grayScaleConversion(thumbnail));
                        extractText(thumbnail);
                    } else {
                        Toast.makeText(this, "Image capture failed!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Bitmap grayScaleConversion(Bitmap thumbnail) {
        int width, height;
        height = thumbnail.getHeight();
        width = thumbnail.getWidth();

        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(grayscaleBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Setting saturation to 0 converts the image to grayscale
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(thumbnail, 0, 0, paint);

        return grayscaleBitmap;
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