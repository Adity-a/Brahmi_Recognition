package com.ocr.scripts.translation;

import android.Manifest;
import android.app.Activity;
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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.ocr.brahmi.translation.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ScannerActivity extends AppCompatActivity {

    ImageView camera_btn, gallery_btn, extract, page_btn;
    int GALLERY_SELECT_CODE = 100;
    int CAMERA_SELECT_CODE = 200;
    static int REQUEST_ID_MULTIPLE_PERMISSIONS = 101;
    Uri cameraImageUri;
    TessBaseAPI tessBaseAPI;
    String tessDataPath;
    Spinner languageSpinner;
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
        languageSpinner = findViewById(R.id.language_spinner);

        if(checkAndRequestPermissions(ScannerActivity.this)){
            camera_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    capture();
                }
            });

            gallery_btn = findViewById(R.id.gallery_btn);
            gallery_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Take();
                }
            });
        }

        page_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ScannerActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                String langCode = "eng"; // Default to English

                switch (selectedLanguage) {
                    case "Brahmi":
                        langCode = "brahmi";
                        break;
                    case "Sanskrit":
                        langCode = "san";
                        break;
                    case "English":
                        langCode = "eng";
                        break;
                }
                // Reinitialize Tesseract with the new language
                if (tessBaseAPI != null) {
                    tessBaseAPI.end(); // End previous instance
                }
                tessBaseAPI.init(tessDataPath, langCode, TessBaseAPI.OEM_DEFAULT);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public static boolean checkAndRequestPermissions(final Activity context) {
        int Permission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (Permission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded
                    .add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(context, listPermissionsNeeded
                            .toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void copyTrainedData() {
        try {
            File dir = new File(tessDataPath + "tessdata/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String[] languages = {"brahmi", "san", "eng"}; // Add more languages if needed

            for (String lang : languages) {
                String trainedDataName = lang + ".traineddata";
                File trainedDataFile = new File(tessDataPath + "tessdata/" + trainedDataName);

                if (!trainedDataFile.exists()) {
                    InputStream in = getAssets().open("tessdata/" + trainedDataName);
                    OutputStream out = new FileOutputStream(trainedDataFile);

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.flush();
                    out.close();
                    Log.d("Copied", "Copied " + trainedDataName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void capture() {
        File photoFile = new File(getCacheDir(), "IMG_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(ScannerActivity.this, "com.ocr.san.sanskrit.fileprovider", photoFile);
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        takePicture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(takePicture, CAMERA_SELECT_CODE);
    }
    private void Take() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, GALLERY_SELECT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri imageUri = null;

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == GALLERY_SELECT_CODE) {
            imageUri = data.getData();
        }
        else if (resultCode == RESULT_OK && requestCode == CAMERA_SELECT_CODE) {
            imageUri = cameraImageUri;
        }
        if(imageUri != null){
            Intent cropIntent = new Intent(this, CropperActivity.class);
            cropIntent.putExtra("imageUri", imageUri.toString());
            startActivityForResult(cropIntent, 2);
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS){
            if (ContextCompat.checkSelfPermission(ScannerActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                                "FlagUp Requires Access to Camara.", Toast.LENGTH_SHORT)
                        .show();

            } else if (ContextCompat.checkSelfPermission(ScannerActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "FlagUp Requires Access to Your Storage.",
                        Toast.LENGTH_SHORT).show();

            } else {
                camera_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        capture();
                    }
                });

                gallery_btn = findViewById(R.id.gallery_btn);
                gallery_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Take();
                    }
                });
            }
        }
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