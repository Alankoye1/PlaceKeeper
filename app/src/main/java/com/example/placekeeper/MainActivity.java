package com.example.placekeeper;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.OnPlaceClickListener {

    private PlaceAdapter adapter;
    private TextView textEmpty;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<Place> allPlacesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.recycler_places);
        textEmpty = findViewById(R.id.text_empty);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceAdapter(this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddPlaceActivity.class)));

        loadPlaces();
    }

    private void loadPlaces() {
        String userId = mAuth.getUid();
        if (userId == null) return;

        db.collection("users").document(userId).collection("places")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(MainActivity.this, "Error loading places", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allPlacesList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Place place = doc.toObject(Place.class);
                            place.setId(doc.getId());
                            allPlacesList.add(place);
                        }
                    }

                    adapter.setPlaces(allPlacesList);
                    textEmpty.setVisibility(allPlacesList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onDeleteClick(Place place) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    String userId = mAuth.getUid();
                    if (userId != null && place.getId() != null) {
                        db.collection("users").document(userId).collection("places").document(place.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Place deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error deleting place", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public void onEditClick(Place place) {
        Intent intent = new Intent(this, AddPlaceActivity.class);
        intent.putExtra("place_id", place.getId());
        intent.putExtra("place_name", place.getPlaceName());
        intent.putExtra("place_note", place.getNote());
        intent.putExtra("latitude", place.getLatitude());
        intent.putExtra("longitude", place.getLongitude());
        startActivity(intent);
    }

    @Override
    public void onPlaceClick(Place place) {
        Intent intent = new Intent(this, PlaceDetailsActivity.class);
        intent.putExtra("place_id", place.getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapter.filter(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.filter(newText);
                    return false;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}