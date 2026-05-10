package com.example.placekeeper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PlaceDetailsActivity extends AppCompatActivity {

    private ImageView imagePlace;
    private TextView textName, textNote, textCoordinates, textDate;
    private ChipGroup chipGroupTags;
    private Button buttonShare, buttonOpenMaps;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String placeId;
    private Place currentPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        imagePlace = findViewById(R.id.image_place);
        textName = findViewById(R.id.text_place_name);
        textNote = findViewById(R.id.text_place_note);
        textCoordinates = findViewById(R.id.text_place_coordinates);
        textDate = findViewById(R.id.text_place_date);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        buttonShare = findViewById(R.id.button_share);
        buttonOpenMaps = findViewById(R.id.button_open_maps);

        placeId = getIntent().getStringExtra("place_id");
        if (placeId == null) {
            finish();
            return;
        }

        loadPlaceDetails();
    }

    private void loadPlaceDetails() {
        String userId = mAuth.getUid();
        if (userId == null) return;

        db.collection("users").document(userId).collection("places").document(placeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentPlace = documentSnapshot.toObject(Place.class);
                    if (currentPlace != null) {
                        displayPlace(currentPlace);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayPlace(Place place) {
        textName.setText(place.getPlaceName());
        textNote.setText(place.getNote());
        textCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f", place.getLatitude(), place.getLongitude()));
        
        if (place.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(place.getCreatedAt().toDate());
            textDate.setText(getString(R.string.added_date, dateStr));
        }

        if (place.getImageUrl() != null && !place.getImageUrl().isEmpty()) {
            imagePlace.setVisibility(View.VISIBLE);
            Glide.with(this).load(place.getImageUrl()).into(imagePlace);
        }

        chipGroupTags.removeAllViews();
        if (place.getTags() != null) {
            for (String tag : place.getTags()) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setCheckable(false);
                chip.setClickable(false);
                chipGroupTags.addView(chip);
            }
        }

        buttonShare.setOnClickListener(v -> sharePlace(place));
        buttonOpenMaps.setOnClickListener(v -> openInMaps(place));
    }

    private void sharePlace(Place place) {
        String shareText = "Check out this place: " + place.getPlaceName() + "\n" +
                "Location: " + place.getLatitude() + ", " + place.getLongitude() + "\n" +
                "Note: " + place.getNote();
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "PlaceKeeper: " + place.getPlaceName());
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void openInMaps(Place place) {
        Uri gmmIntentUri = Uri.parse("geo:" + place.getLatitude() + "," + place.getLongitude() + "?q=" + place.getLatitude() + "," + place.getLongitude() + "(" + place.getPlaceName() + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps app not found", Toast.LENGTH_SHORT).show();
        }
    }
}