package com.example.placekeeper;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class AddPlaceActivity extends AppCompatActivity {

    private TextInputEditText editName, editNote, editLat, editLng;
    private Button buttonSave;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editName = findViewById(R.id.edit_place_name);
        editNote = findViewById(R.id.edit_note);
        editLat = findViewById(R.id.edit_latitude);
        editLng = findViewById(R.id.edit_longitude);
        buttonSave = findViewById(R.id.button_save);
        Button buttonCancel = findViewById(R.id.button_cancel);

        buttonSave.setOnClickListener(v -> savePlace());
        buttonCancel.setOnClickListener(v -> finish());
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