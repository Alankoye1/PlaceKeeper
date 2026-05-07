package com.example.placekeeper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Locale;
import java.util.Objects;

public class AddPlaceActivity extends AppCompatActivity {

    private static final int PICK_LOCATION_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;

    private TextInputEditText editName, editNote, editLat, editLng;
    private Button buttonSave;
    private TextView textTitle;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private String placeId; // Used for editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        textTitle = findViewById(R.id.text_title);
        editName = findViewById(R.id.edit_place_name);
        editNote = findViewById(R.id.edit_note);
        editLat = findViewById(R.id.edit_latitude);
        editLng = findViewById(R.id.edit_longitude);
        buttonSave = findViewById(R.id.button_save);
        Button buttonCancel = findViewById(R.id.button_cancel);
        Button buttonCurrent = findViewById(R.id.button_current_location);
        Button buttonPick = findViewById(R.id.button_pick_map);

        // Check for edit mode
        Intent intent = getIntent();
        if (intent.hasExtra("place_id")) {
            placeId = intent.getStringExtra("place_id");
            textTitle.setText(R.string.title_edit_place);
            editName.setText(intent.getStringExtra("place_name"));
            editNote.setText(intent.getStringExtra("place_note"));
            editLat.setText(String.format(Locale.getDefault(), "%.6f", intent.getDoubleExtra("latitude", 0.0)));
            editLng.setText(String.format(Locale.getDefault(), "%.6f", intent.getDoubleExtra("longitude", 0.0)));
        }

        buttonSave.setOnClickListener(v -> savePlace());
        buttonCancel.setOnClickListener(v -> finish());
        buttonPick.setOnClickListener(v -> startActivityForResult(new Intent(AddPlaceActivity.this, LocationPickerActivity.class), PICK_LOCATION_REQUEST));
        buttonCurrent.setOnClickListener(v -> checkLocationPermission());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0.0);
            double lng = data.getDoubleExtra("longitude", 0.0);
            editLat.setText(String.valueOf(lat));
            editLng.setText(String.valueOf(lng));
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

        String userId = mAuth.getUid();
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Place place = new Place(name, note, latitude, longitude);
        
        if (placeId != null) {
            // Update existing
            db.collection("users").document(userId).collection("places").document(placeId)
                    .set(place)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddPlaceActivity.this, "Place updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddPlaceActivity.this, "Error updating place: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                    });
        } else {
            // Add new
            db.collection("users").document(userId).collection("places")
                    .add(place)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddPlaceActivity.this, "Place saved", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddPlaceActivity.this, "Error saving place: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                    });
        }
    }
}