package com.ocr.brahmi.translation;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

public class CropperActivity extends AppCompatActivity {

    Uri imageUri, imageUri1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cropper);

        readIntent();

        String dest_uri = UUID.randomUUID().toString() + ".jpg";

        UCrop.Options options = new UCrop.Options();
        options.setMaxScaleMultiplier(5);
        options.setImageToCropBoundsAnimDuration(666);
        options.setShowCropFrame(true);

        UCrop.of(imageUri, Uri.fromFile(new File(getCacheDir(), dest_uri)))
                .withOptions(options)
                .withMaxResultSize(2000, 2000)
                .start(this);

    }

    private void readIntent() {
        imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP){
            if(data != null){
                final Uri croppedUri = UCrop.getOutput(data);
                Intent finalIntent = new Intent();
                finalIntent.putExtra("croppedUri", croppedUri+"");
                setResult(RESULT_OK, finalIntent);
                finish();
            }
        } else if (resultCode==UCrop.RESULT_ERROR) {
            if(data != null){
                final Throwable cropError = UCrop.getError(data);
                cropError.printStackTrace();
            }
        }
    }
}