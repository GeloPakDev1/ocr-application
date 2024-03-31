package com.example.ocr_application;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.CAMERA;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {
    private MaterialButton inputImgButton;
    private ShapeableImageView imageView;
    private EditText recognizedText;

    private static final String TAG = "MAIN_TAG";
    private Uri imageUri = null;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    private String[] cameraPermissions;

    private ProgressDialog progressDialog;

    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputImgButton = findViewById(R.id.inputImgButton);
        MaterialButton recognizeTextButton = findViewById(R.id.recognizeTextButton);
        imageView = findViewById(R.id.imageView);
        recognizedText = findViewById(R.id.recognizedText);

        cameraPermissions = new String[]{CAMERA, WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        inputImgButton.setOnClickListener(view -> showInputImageDialog());

        recognizeTextButton.setOnClickListener(view -> {
            if (imageUri == null) {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            } else {
                recognizeTextFromImage();
            }
        });
    }

    private void recognizeTextFromImage() {
        Log.d(TAG, "recognizeTextFromImage: ");
        progressDialog.setMessage("Recognizing text...");
        progressDialog.show();
        try {
            InputImage image = InputImage.fromFilePath(getApplicationContext(), imageUri);
            progressDialog.setMessage("Recognizing text...");
            Task<Text> textTaskResult = textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        progressDialog.dismiss();
                        recognizedText.setText(text.getText());
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed recognizing text due to " + e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    }

    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImgButton);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == 1) {
                Log.d(TAG, "showInputImageDialog: ");
                if (checkCameraPermission()) {
                    pickFromCamera();
                } else {
                    requestCameraPermission();
                }
            } else if (id == 2) {
                pickImageGallery();
            }
            return true;
        });
    }

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    imageUri = data.getData();
                    imageView.setImageURI(imageUri);
                } else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image to Text");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    imageView.setImageURI(imageUri);
                } else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private boolean checkCameraPermission() {
        Log.d(TAG, "checkCameraPermission: ");
        return ContextCompat.checkSelfPermission(this, CAMERA) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission: ");
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Camera & Storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        pickImageGallery();
                    } else {
                        Toast.makeText(this, "Storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}