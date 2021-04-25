// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.grabfruit;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Runnable {
    private int mImageIndex = 0;

    private String[] mTestImages = {
            "test6.jpg", "test7.jpg", "test8.jpg"
    };

    private ImageView mImageView;
    private Button mButtonDetect;
    private Button mButtonLive;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        setContentView(R.layout.activity_main);

        try {
            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);
        final Button buttonTest = findViewById(R.id.testButton);
        buttonTest.setText(("Test Image 1/3"));
        buttonTest.setOnClickListener(v -> {
            mImageIndex = (mImageIndex + 1) % mTestImages.length;
            buttonTest.setText(String.format("Text Image %d/%d", mImageIndex + 1, mTestImages.length));

            try {
                mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
                mImageView.setImageBitmap(mBitmap);
            } catch (IOException e) {
                Log.e("Object Detection", "Error reading assets", e);
                finish();
            }
        });


        final Button buttonSelect = findViewById(R.id.selectButton);
        buttonSelect.setOnClickListener(v -> {

            final CharSequence[] options = { "Choose from Photos", "Take Picture", "Cancel" };
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("New Test Image");

            builder.setItems(options, (dialog, item) -> {
                if (options[item].equals("Take Picture")) {
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, 0);
                }
                else if (options[item].equals("Choose from Photos")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto , 1);
                }
                else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            });
            builder.show();
        });


        mButtonLive = (Button) findViewById(R.id.liveButton);
        mButtonLive.setEnabled(false);
        mButtonLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mButtonDetect = findViewById(R.id.detectButton);
        mProgressBar = findViewById(R.id.progressBar);
        mButtonDetect.setOnClickListener(v -> {

            mButtonDetect.setEnabled(false);
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mButtonDetect.setText(getString(R.string.run_model));

            Thread thread = new Thread(MainActivity.this);
            thread.start();

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        mBitmap = (Bitmap) data.getExtras().get("data");
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90.0f);
                        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                        mImageView.setImageBitmap(mBitmap);
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                mBitmap = BitmapFactory.decodeFile(picturePath);
                                Matrix matrix = new Matrix();
                                matrix.postRotate(90.0f);
                                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                                mImageView.setImageBitmap(mBitmap);
                                cursor.close();
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void run() {

        Classifier classifier;
        classifier = new Classifier(Utils.assetFilePath(this, "grabfruit_resnet_18_jit.torchscript.pt"));
        String pred = classifier.predict(mBitmap);

        System.out.println("print inference: " + pred);

        final TextView setClass = findViewById(R.id.textView);
        setClass.setText(pred);

        runOnUiThread(() -> {
            mButtonDetect.setEnabled(true);
            mButtonDetect.setText(getString(R.string.classify));

            // enable live button
            mButtonLive.setEnabled(true);
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        });
    }
}
