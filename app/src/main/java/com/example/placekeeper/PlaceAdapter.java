package com.example.placekeeper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    private List<Place> allPlaces = new ArrayList<>();
    private List<Place> displayedPlaces = new ArrayList<>();
    final private OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onDeleteClick(Place place);
        void onEditClick(Place place);
    }

    public PlaceAdapter(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void setPlaces(List<Place> places) {
        this.allPlaces = places;
        this.displayedPlaces = new ArrayList<>(places);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        displayedPlaces.clear();
        if (query.isEmpty()) {
            displayedPlaces.addAll(allPlaces);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (Place place : allPlaces) {
                if (place.getPlaceName().toLowerCase().contains(filterPattern)) {
                    displayedPlaces.add(place);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = displayedPlaces.get(position);
        holder.bind(place, listener);
    }

    @Override
    public int getItemCount() {
        return displayedPlaces.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textNote;
        final TextView textCoordinates;
        final TextView textDate;
        final Button buttonDelete;
        final Button buttonEdit;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_place_name);
            textNote = itemView.findViewById(R.id.text_place_note);
            textCoordinates = itemView.findViewById(R.id.text_place_coordinates);
            textDate = itemView.findViewById(R.id.text_place_date);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonEdit = itemView.findViewById(R.id.button_edit);
        }

        public void bind(final Place place, final OnPlaceClickListener listener) {
            textName.setText(place.getPlaceName());
            textNote.setText(place.getNote());
            textCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", place.getLatitude(), place.getLongitude()));
            
            if (place.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                textDate.setText("Added: " + sdf.format(place.getCreatedAt().toDate()));
            } else {
                textDate.setText("Added: Just now");
            }

            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(place);
                }
            });

            buttonEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(place);
                }
            });
        }
    }
}