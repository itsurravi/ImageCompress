package com.ravisharma.imagecompress;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView actualImageView;
    private ImageView compressedImageView;
    private TextView actualSizeTextView;
    private TextView compressedSizeTextView;
    private TextView textPath;
    private File actualImage;
    private File compressedImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actualImageView = (ImageView) findViewById(R.id.actual_image);
        compressedImageView = (ImageView) findViewById(R.id.compressed_image);
        actualSizeTextView = (TextView) findViewById(R.id.actual_size);
        compressedSizeTextView = (TextView) findViewById(R.id.compressed_size);
        textPath = (TextView) findViewById(R.id.textPath);

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                            actualImageView.setBackgroundColor(getRandomColor());
                            clearImage();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            Toast.makeText(MainActivity.this, "All Permissions are Required", Toast.LENGTH_SHORT).show();
                            finish();
                            System.exit(0);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }

    public void chooseImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    public void compressImage(View view) {
        if (actualImage == null) {
            showError("Please choose an image!");
        } else {

            // Compress image to bitmap in main thread
            try {
//                 Compress image in main thread
                compressedImage = new Compressor(this).compressToFile(actualImage);
                compressedImageView.setImageBitmap(new Compressor(this).compressToBitmap(actualImage));
                setCompressedImage();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Compress image using RxJava in background thread
            /*new Compressor(this)
                    .compressToFileAsFlowable(actualImage)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<File>() {
                        @Override
                        public void accept(File file) {
                            compressedImage = file;
                            setCompressedImage();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            throwable.printStackTrace();
                            showError(throwable.getMessage());
                        }
                    });*/
        }
    }

    public void customCompressImage(View view) {
        if (actualImage == null) {
            showError("Please choose an image!");
        } else {
            // Compress image in main thread using custom Compressor
            try {
                compressedImage = new Compressor(this)
                        .setMaxWidth(640)
                        .setMaxHeight(480)
                        .setQuality(75)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath())
                        .compressToFile(actualImage);

                setCompressedImage();
            } catch (IOException e) {
                e.printStackTrace();
                showError(e.getMessage());
            }

            // Compress image using RxJava in background thread with custom Compressor
            /*new Compressor(this)
                    .setMaxWidth(640)
                    .setMaxHeight(480)
                    .setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.WEBP)
                    .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES).getAbsolutePath())
                    .compressToFileAsFlowable(actualImage)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<File>() {
                        @Override
                        public void accept(File file) {
                            compressedImage = file;
                            setCompressedImage();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            throwable.printStackTrace();
                            showError(throwable.getMessage());
                        }
                    });*/
        }

    }

    private void setCompressedImage() {
        compressedImageView.setImageBitmap(BitmapFactory.decodeFile(compressedImage.getAbsolutePath()));
        compressedSizeTextView.setText(String.format("Size : %s", getReadableFileSize(compressedImage.length())));
        textPath.setText(compressedImage.getPath());
        Toast.makeText(this, "Compressed image save in " + compressedImage.getPath(), Toast.LENGTH_LONG).show();
        Log.d("Compressor", "Compressed image save in " + compressedImage.getPath());
    }

    private void clearImage() {
        actualImageView.setBackgroundColor(getRandomColor());
        compressedImageView.setImageDrawable(null);
        compressedImageView.setBackgroundColor(getRandomColor());
        compressedSizeTextView.setText("Size : -");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data == null) {
                showError("Failed to open picture!");
                return;
            }
            try {
                actualImage = FileUtil.from(this, data.getData());
                actualImageView.setImageBitmap(BitmapFactory.decodeFile(actualImage.getAbsolutePath()));
                actualSizeTextView.setText(String.format("Size : %s", getReadableFileSize(actualImage.length())));
                clearImage();
            } catch (IOException e) {
                showError("Failed to read picture data!");
                e.printStackTrace();
            }
        }
    }

    public void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private int getRandomColor() {
        Random rand = new Random();
        return Color.argb(100, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    public String getReadableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
