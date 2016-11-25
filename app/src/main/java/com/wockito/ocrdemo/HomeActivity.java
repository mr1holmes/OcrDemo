package com.wockito.ocrdemo;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private Button mCaptureBtn;
    private ImageView mImageView;
    private Button mTextBtn;

    private Uri mImageUri;
    private StringBuilder mString;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mCaptureBtn = (Button) findViewById(R.id.capture_btn);
        mImageView = (ImageView) findViewById(R.id.capture_img);
        mTextBtn = (Button) findViewById(R.id.text_btn);
        mString = new StringBuilder();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Loading..");

        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File photo = null;
                try {
                    // place where to store camera taken picture
                    photo = createTemporaryFile("picture", ".jpg");
                    photo.delete();
                } catch (Exception e) {
                    Log.v(TAG, "Can't create file to take picture!");
                }

                if (photo != null) {
                    mImageUri = Uri.fromFile(photo);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }

            }
        });

        mTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(HomeActivity.this)
                        .title("Text")
                        .content(mString)
                        .positiveText("Ok")
                        .show();
            }
        });


    }

    private void parseTextFromBitmap(Bitmap bitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor());

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Low Storage", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Low Storage");
            }
        }

        Frame imageFrame = new Frame.Builder()
                .setBitmap(bitmap)
                .build();
        textRecognizer.setProcessor(new OcrDetectorProcessor());
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);


        mString = new StringBuilder();
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

            Log.i(TAG, textBlock.getValue());
            mString.append(textBlock.getValue() + "\n");
        }

        textRecognizer.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            new ImageTask().execute();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private File createTemporaryFile(String part, String ext) throws Exception {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return File.createTempFile(part, ext, tempDir);
    }

    public void grabImage() {
        getContentResolver().notifyChange(mImageUri, null);
        ContentResolver cr = this.getContentResolver();
        Bitmap bitmap;
        try {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, mImageUri);
            mImageView.setImageBitmap(bitmap);
            parseTextFromBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Failed to load", e);
        }
    }

    private class ImageTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {

            getContentResolver().notifyChange(mImageUri, null);
            ContentResolver cr = getContentResolver();
            Bitmap bitmap = null;
            try {
                bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, mImageUri);

                parseTextFromBitmap(bitmap);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Failed to load", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Failed to load", e);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            }
            mProgressDialog.dismiss();
            super.onPostExecute(bitmap);
        }
    }

}
