package com.example.slotsmart.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.slotsmart.R;
import com.example.slotsmart.model.EntityItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomsFragment extends BaseEntityFragment {

    @Override
    protected String getListAction() {
        return "get_rooms";
    }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null || !data.isJsonArray()) return list;

        for (JsonElement el : data.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            String projector = safe(o, "has_projector").equals("1") ? " • Projector ✓" : "";
            list.add(new EntityItem(
                safe(o, "room_id"),
                safe(o, "room_number"),
                safe(o, "room_type") + " • Cap: " + safe(o, "capacity") + projector,
                null,
                o
            ));
        }
        return list;
    }

    @Override
    protected void showAddDialog() {
        showFormDialog("Add Room", null);
    }

    @Override
    protected void showEditDialog(EntityItem item) {
        showFormDialog("Edit Room", item);
    }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_room, null);

        EditText etNumber = dialogView.findViewById(R.id.etRoomNumber);
        AutoCompleteTextView etType = dialogView.findViewById(R.id.etRoomType);
        EditText etCapacity = dialogView.findViewById(R.id.etCapacity);
        CheckBox cbProjector = dialogView.findViewById(R.id.cbHasProjector);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        // Populate room types dropdown
        String[] roomTypes = {"Lecture Hall", "Lab", "Seminar Room"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, roomTypes);
        etType.setAdapter(typeAdapter);

        if (item != null) {
            etNumber.setText(safe(item.raw, "room_number"));
            etType.setText(safe(item.raw, "room_type"), false);
            etCapacity.setText(safe(item.raw, "capacity"));
            cbProjector.setChecked(safe(item.raw, "has_projector").equals("1"));
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String number = etNumber.getText().toString().trim();
                    String type = etType.getText().toString().trim();
                    String capacity = etCapacity.getText().toString().trim();

                    if (number.isEmpty() || type.isEmpty() || capacity.isEmpty()) {
                        toast("All fields are required.");
                        return;
                    }

                    Map<String, String> fields = new HashMap<>();
                    if (item == null) {
                        fields.put("action", "add_room");
                    } else {
                        fields.put("action", "update_room");
                        fields.put("id", hiddenId.getText().toString());
                    }
                    fields.put("room_number", number);
                    fields.put("room_type", type);
                    fields.put("capacity", capacity);
                    fields.put("has_projector", cbProjector.isChecked() ? "1" : "0");

                    performRequest(fields, item == null ? "Room added!" : "Room updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_room");
        fields.put("id", item.id);
        performRequest(fields, "Room deleted!");
    }
}
