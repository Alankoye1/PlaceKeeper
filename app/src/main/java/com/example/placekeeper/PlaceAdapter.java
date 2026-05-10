package com.example.placekeeper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
        void onPlaceClick(Place place);
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
        final ChipGroup chipGroupTags;

        PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_place_name);
            textNote = itemView.findViewById(R.id.text_place_note);
            textCoordinates = itemView.findViewById(R.id.text_place_coordinates);
            textDate = itemView.findViewById(R.id.text_place_date);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            chipGroupTags = itemView.findViewById(R.id.chip_group_tags);
        }

        public void bind(final Place place, final OnPlaceClickListener listener) {
            textName.setText(place.getPlaceName());
            textNote.setText(place.getNote());
            textCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", place.getLatitude(), place.getLongitude()));
            
            if (place.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateStr = sdf.format(place.getCreatedAt().toDate());
                textDate.setText(itemView.getContext().getString(R.string.added_date, dateStr));
            } else {
                textDate.setText(itemView.getContext().getString(R.string.added_just_now));
            }

            chipGroupTags.removeAllViews();
            if (place.getTags() != null) {
                for (String tag : place.getTags()) {
                    Chip chip = new Chip(itemView.getContext());
                    chip.setText(tag);
                    chip.setCheckable(false);
                    chip.setClickable(false);
                    chipGroupTags.addView(chip);
                }
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

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });
        }
    }
}