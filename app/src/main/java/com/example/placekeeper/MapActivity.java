package com.example.placekeeper;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadMarkers();
    }

    private void loadMarkers() {
        String userId = mAuth.getUid();
        if (userId == null) return;

        db.collection("users").document(userId).collection("places")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(MapActivity.this, "No places to show on map", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Place place = doc.toObject(Place.class);
                        LatLng latLng = new LatLng(place.getLatitude(), place.getLongitude());
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(place.getPlaceName())
                                .snippet(place.getNote()));
                        builder.include(latLng);
                    }

                    LatLngBounds bounds = builder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                })
                .addOnFailureListener(e -> Toast.makeText(MapActivity.this, "Error loading map data", Toast.LENGTH_SHORT).show());
    }
}