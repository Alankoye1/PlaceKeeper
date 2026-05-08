package com.example.placekeeper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class AddPlaceActivity extends AppCompatActivity {

    private static final int PICK_LOCATION_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;
    private static final int PICK_IMAGE_REQUEST = 3;
    private static final int CAPTURE_IMAGE_REQUEST = 4;

    private TextInputEditText editName, editNote, editLat, editLng, editAddTag;
    private Button buttonSave, buttonAddPhoto;
    private ImageView imagePreview;
    private ChipGroup chipGroupTags;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;
    
    private String placeId;
    private List<String> tags = new ArrayList<>();
    private Uri imageUri;
    private String currentImageUrl;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView textTitle = findViewById(R.id.text_title);
        editName = findViewById(R.id.edit_place_name);
        editNote = findViewById(R.id.edit_note);
        editLat = findViewById(R.id.edit_latitude);
        editLng = findViewById(R.id.edit_longitude);
        editAddTag = findViewById(R.id.edit_add_tag);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        imagePreview = findViewById(R.id.image_preview);
        buttonAddPhoto = findViewById(R.id.button_add_photo);
        buttonSave = findViewById(R.id.button_save);
        Button buttonCancel = findViewById(R.id.button_cancel);
        Button buttonCurrent = findViewById(R.id.button_current_location);

        // Edit Mode
        Intent intent = getIntent();
        if (intent.hasExtra("place_id")) {
            placeId = intent.getStringExtra("place_id");
            textTitle.setText(R.string.title_edit_place);
            editName.setText(intent.getStringExtra("place_name"));
            editNote.setText(intent.getStringExtra("place_note"));
            editLat.setText(String.format(Locale.getDefault(), "%.6f", intent.getDoubleExtra("latitude", 0.0)));
            editLng.setText(String.format(Locale.getDefault(), "%.6f", intent.getDoubleExtra("longitude", 0.0)));
            // Tags and Image would be loaded in a real app, but for Phase 3 we'll handle them if passed
            // In a more robust version, we'd fetch the full Place object from Firestore
        }

        buttonSave.setOnClickListener(v -> savePlace());
        buttonCancel.setOnClickListener(v -> finish());
        buttonCurrent.setOnClickListener(v -> checkLocationPermission());
        
        buttonAddPhoto.setOnClickListener(v -> showImagePickerDialog());
        
        editAddTag.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String tag = editAddTag.getText().toString().trim();
                if (!TextUtils.isEmpty(tag)) {
                    addTag(tag);
                    editAddTag.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                chipGroupTags.removeView(chip);
                tags.remove(tag);
            });
            chipGroupTags.addView(chip);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, PICK_IMAGE_REQUEST);
                    }
                }).show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                imageUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
                imagePreview.setImageURI(imageUri);
                imagePreview.setVisibility(android.view.View.VISIBLE);
            } else if (requestCode == CAPTURE_IMAGE_REQUEST) {
                imagePreview.setImageURI(imageUri);
                imagePreview.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                editLat.setText(String.valueOf(location.getLatitude()));
                editLng.setText(String.valueOf(location.getLongitude()));
            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePlace() {
        String name = Objects.requireNonNull(editName.getText()).toString().trim();
        String note = Objects.requireNonNull(editNote.getText()).toString().trim();
        String latStr = Objects.requireNonNull(editLat.getText()).toString().trim();
        String lngStr = Objects.requireNonNull(editLng.getText()).toString().trim();

        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(latStr)) {
            editLat.setError("Latitude is required");
            return;
        }

        if (TextUtils.isEmpty(lngStr)) {
            editLng.setError("Longitude is required");
            return;
        }

        double latitude, longitude;
        try {
            latitude = Double.parseDouble(latStr);
            longitude = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSave.setEnabled(false);

        if (imageUri != null) {
            uploadImageAndSavePlace(name, note, latitude, longitude);
        } else {
            finalSavePlace(name, note, latitude, longitude, currentImageUrl);
        }
    }

    private void uploadImageAndSavePlace(String name, String note, double latitude, double longitude) {
        Toast.makeText(this, R.string.msg_photo_uploading, Toast.LENGTH_SHORT).show();
        StorageReference ref = storage.getReference().child("places/" + UUID.randomUUID().toString());
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    currentImageUrl = uri.toString();
                    finalSavePlace(name, note, latitude, longitude, currentImageUrl);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(AddPlaceActivity.this, R.string.msg_photo_error, Toast.LENGTH_SHORT).show();
                    buttonSave.setEnabled(true);
                });
    }

    private void finalSavePlace(String name, String note, double latitude, double longitude, String imageUrl) {
        String userId = mAuth.getUid();
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Place place = new Place(name, note, latitude, longitude);
        place.setTags(tags);
        place.setImageUrl(imageUrl);
        
        if (placeId != null) {
            db.collection("users").document(userId).collection("places").document(placeId)
                    .set(place)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddPlaceActivity.this, "Place updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddPlaceActivity.this, "Error updating place", Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                    });
        } else {
            db.collection("users").document(userId).collection("places")
                    .add(place)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddPlaceActivity.this, "Place saved", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddPlaceActivity.this, "Error saving place", Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                    });
        }
    }
}