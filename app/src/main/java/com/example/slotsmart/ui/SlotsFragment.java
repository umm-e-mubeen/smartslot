package com.example.slotsmart.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.slotsmart.R;
import com.example.slotsmart.model.EntityItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotsFragment extends BaseEntityFragment {

    private static final List<String> DAYS = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

    @Override
    protected String getListAction() { return "get_slots"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null || !data.isJsonArray()) return list;
        for (JsonElement el : data.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            String day = safe(o, "day");
            String start = safe(o, "start_time");
            String end = safe(o, "end_time");
            list.add(new EntityItem(
                    safe(o, "slot_id"),
                    day,
                    start + " – " + end,
                    null,
                    o));
        }
        return list;
    }

    @Override
    protected void showAddDialog() { showFormDialog("Add Time Slot", null); }

    @Override
    protected void showEditDialog(EntityItem item) { showFormDialog("Edit Time Slot", item); }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_slot, null);
        AutoCompleteTextView spinnerDay = dialogView.findViewById(R.id.spinnerDay);
        TextInputEditText etStart = dialogView.findViewById(R.id.etStartTime);
        TextInputEditText etEnd = dialogView.findViewById(R.id.etEndTime);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        spinnerDay.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, DAYS));

        if (item != null) {
            spinnerDay.setText(safe(item.raw, "day"), false);
            etStart.setText(safe(item.raw, "start_time"));
            etEnd.setText(safe(item.raw, "end_time"));
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String day = spinnerDay.getText().toString().trim();
                    String start = etStart.getText() != null ? etStart.getText().toString().trim() : "";
                    String end = etEnd.getText() != null ? etEnd.getText().toString().trim() : "";
                    if (day.isEmpty() || start.isEmpty() || end.isEmpty()) {
                        toast("All fields required."); return;
                    }
                    Map<String, String> fields = new HashMap<>();
                    fields.put("action", item == null ? "add_slot" : "update_slot");
                    if (item != null) fields.put("id", hiddenId.getText().toString());
                    fields.put("day", day);
                    fields.put("start_time", start);
                    fields.put("end_time", end);
                    performRequest(fields, item == null ? "Slot added!" : "Slot updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_slot");
        fields.put("id", item.id);
        performRequest(fields, "Slot deleted!");
    }
}
