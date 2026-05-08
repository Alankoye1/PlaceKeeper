## Plan: Resolve PlaceKeeper Map & Camera Bugs

Here is a proposed plan to remove the unused export feature and fix the bugs related to Google Maps and the Camera picker based on the project code.

### Steps
1. **Remove Export Data** string `btn_export` from `strings.xml` (no other backend/export code was found).
2. **Fix Map Bug** in `MapActivity.java` by wrapping `mMap.animateCamera` in `mMap.setOnMapLoadedCallback` to ensure map layout occurs before determining padding.
3. **Handle Single Marker Bounds** in MapActivity by zooming directly to a single `LatLng` rather than using bounds if `queryDocumentSnapshots` size is 1.
4. **Fix Camera Bug** by adding a `<queries>` block for `android.media.action.IMAGE_CAPTURE` in `AndroidManifest.xml` to resolve Android 11+ package visibility rules causing `resolveActivity` to fail.
5. **Update FileProvider Paths** in `file_paths.xml` using `<external-files-path>` instead of `<external-path>` to prevent camera URI authorization crashes on newer devices.

### Further Considerations
1. If "Export Data" was meant to be the "Share Place" feature within `PlaceDetailsActivity.java`, please confirm and I will plan to remove that instead.
2. Does this draft look good to you, or would you like to make any adjustments?
