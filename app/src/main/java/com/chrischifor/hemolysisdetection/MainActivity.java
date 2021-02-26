/* 2016-2021 UFP
"Towards Early Hemolysis: Detection: a Smartphone based Approach" PhD thesis
"HemoDetect" prototype app code
email contact for questions or support: 35727@ufp.edu.pt */

package com.chrischifor.hemolysisdetection;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.chrischifor.hemolysisdetection.utility.PixelDensity;
import com.chrischifor.hemolysisdetection.utility.ColorUtility;
import com.chrischifor.hemolysisdetection.utility.ImageUtility;
import com.chrischifor.hemolysisdetection.utility.PixelLuminance;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends ActionBarActivity {
    static final int REQUEST_TAKE_PHOTO = 1;
    ImageView mImageView;
    Button btnOpenCameraView;
    String mCurrentPhotoPath;


    @Override
    //Activity/Fragment
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Activity creates a window in which the UI is placed
        setContentView(R.layout.activity_main);
        //Retrieval of the widgets in the UI that are used to interact with programmatically
        mImageView = findViewById(R.id.mImageView);
        btnOpenCameraView = findViewById(R.id.btnOpenCameraView);
        initialize();
        Toast.makeText(getApplicationContext(),"Press button to open camera", Toast.LENGTH_LONG).show();
    }

    //Create an instance of the object
    private void initialize() {
        setContentView(R.layout.activity_main);
        //Interface definition for a callback to be invoked when a view is clicked
        findViewById(R.id.btnOpenCameraView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View camera_view) {
                // Call dispatchTakePictureIntent() once the START HEMOLYSIS DETECTION button is pressed
                try {
                    dispatchTakePictureIntent();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Failed to open camera", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        //Call to start an external activity for the camera app, capture an image and return it
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getApplicationContext(), "Failed to create image file", Toast.LENGTH_LONG).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.chrischifor.hemolysisdetection.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setContentView(R.layout.activity_main);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            processImage();
            Toast.makeText(getApplicationContext(),"Image processed successfully", Toast.LENGTH_LONG).show();
            initialize();
        }
    }
    //ViewModel
    private File createImageFile() throws IOException {
        //Create an image file
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        //Save the file
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void processImage() {
        Bitmap bitmap = ImageUtility.loadStandardBitmapFromPath(mCurrentPhotoPath);
        if (bitmap != null) {
            getPixelDensity(bitmap);
            getPixelLuminance(bitmap);
        }
        //Display the image resource, in this case the bitmap
        ((ImageView) findViewById(R.id.mImageView)).setImageBitmap(bitmap);
    }
    //Model
    private void getPixelDensity(Bitmap bitmap) {
        new PixelDensity(new PixelDensity.CallbackInterface() {
            @Override
            public void onCompleted(String hexColor) {
                setDensity(hexColor);
            }
        }).findDominantColor(bitmap);
    }

    private void getPixelLuminance(Bitmap bitmap) {
        new PixelLuminance(new PixelLuminance.CallbackInterface() {
            @Override
            public void onCompleted(String hexColor) {
                setLuminance(hexColor);
            }
        }).findDominantColor(bitmap);
    }

    private void setLuminance(String hexColor) {
        int color = Color.parseColor(hexColor);
        findViewById(R.id.luminance).setBackgroundColor(color);
        ((TextView) findViewById(R.id.luminanceText)).setTextColor(ColorUtility.getOptimizedTextColor(color, 0f));
        ((TextView) findViewById(R.id.luminanceText)).setText("Measured hemolysis level ==>");
    }

    private String setDensity(String hexColor) {
        int color = Color.parseColor(hexColor);
        //establish reference CIELAB values for blood plasma
        double Lr = 79.658;
        double ar = -6.543;
        double br = 68.284;
        //get new CIELAB measured values from the photographed sample
        double Lm = ColorUtility.calculateLightness(ColorUtility.getRedFromColor(color), ColorUtility.getGreenFromColor(color), ColorUtility.getBlueFromColor(color));
        double am = ColorUtility.calculateAm(ColorUtility.getRedFromColor(color), ColorUtility.getGreenFromColor(color), ColorUtility.getBlueFromColor(color));
        double bm = ColorUtility.calculateBm(ColorUtility.getRedFromColor(color), ColorUtility.getGreenFromColor(color), ColorUtility.getBlueFromColor(color));
        //apply formula to calculate the color index
        int i = 2;
        double CI = Math.sqrt(Math.pow((Lm - Lr), i) + Math.pow((am - ar), i) + Math.pow((bm - br), i));
        findViewById(R.id.density).setBackgroundColor(Color.parseColor(hexColor));
        ((TextView) findViewById(R.id.densityText)).setTextColor(ColorUtility.getOptimizedTextColor(color, 0f));
        //map calculated color index to the determined calibration curve
        //hemolysis free (≤ 5 mg/dl), low hemolysis (5–30 mg/dl), medium hemolysis (30 – 60 mg/dl), high hemolysis (60–300 mg/dl), or very high hemolysis (≥300 mg/dl)
        String hemolysis_result;
        if (CI <= 5) {
            hemolysis_result = "Hemolysis free (HF)";
        } else if (5 < CI && CI <= 30) {
            hemolysis_result = "Low hemolysis (LH)";
        } else if (30 < CI && CI <= 60) {
            hemolysis_result = "Medium hemolysis (MH)";
        } else if (60 < CI && CI < 300) {
            hemolysis_result = "High hemolysis (HH)";
        } else if (CI >= 300) {
            hemolysis_result = "Very high hemolysis (VHH)";
        }
        else{
            hemolysis_result = "Hemolysis level undetermined";
        }
        ((TextView) findViewById(R.id.densityText)).setText(
                "Result: " + hemolysis_result/* +
                        "\n" +
                        "CI: " + CI +
                        "\n" +
                        "L*: " + Lm +
                        "\n" +
                        "a*: " + am +
                        "\n" +
                        "b*: " + bm +
                        "\n" +
                        " R:" + ColorUtility.getRedFromColor(color) +
                        " G:" + ColorUtility.getGreenFromColor(color) +
                        " B:" + ColorUtility.getBlueFromColor(color))*/);
        return hemolysis_result;
    }
}