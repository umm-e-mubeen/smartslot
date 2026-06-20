package com.example.slotsmart.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.slotsmart.R;
import com.example.slotsmart.model.EntityItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentsFragment extends BaseEntityFragment {

    @Override
    protected String getListAction() { return "get_departments"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null || !data.isJsonArray()) return list;
        for (JsonElement el : data.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            list.add(new EntityItem(
                    safe(o, "department_id"),
                    safe(o, "department_name"),
                    "ID: " + safe(o, "department_id"),
                    null,
                    o));
        }
        return list;
    }

    @Override
    protected void showAddDialog() {
        showFormDialog("Add Department", null);
    }

    @Override
    protected void showEditDialog(EntityItem item) {
        showFormDialog("Edit Department", item);
    }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_department, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etDeptName);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        if (item != null) {
            etName.setText(safe(item.raw, "department_name"));
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (name.isEmpty()) { toast("Name is required."); return; }
                    Map<String, String> fields = new HashMap<>();
                    if (item == null) {
                        fields.put("action", "add_department");
                    } else {
                        fields.put("action", "update_department");
                        fields.put("id", hiddenId.getText().toString());
                    }
                    fields.put("department_name", name);
                    performRequest(fields, item == null ? "Department added!" : "Department updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_department");
        fields.put("id", item.id);
        performRequest(fields, "Department deleted!");
    }
}
