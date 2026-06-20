package com.example.slotsmart.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.slotsmart.R;
import com.example.slotsmart.model.ApiResponse;
import com.example.slotsmart.model.EntityItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgramsFragment extends BaseEntityFragment {

    private List<JsonObject> departments = new ArrayList<>();

    @Override
    protected String getListAction() { return "get_programs"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null || !data.isJsonArray()) return list;
        for (JsonElement el : data.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            list.add(new EntityItem(
                    safe(o, "program_id"),
                    safe(o, "program_name"),
                    safe(o, "department_name"),
                    null,
                    o));
        }
        return list;
    }

    @Override
    protected void onDataLoaded(JsonElement data) {
        loadDepartments();
    }

    private void loadDepartments() {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_departments");
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonArray()) {
                    departments.clear();
                    for (JsonElement el : response.body().data.getAsJsonArray()) {
                        departments.add(el.getAsJsonObject());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void showAddDialog() { showFormDialog("Add Program", null); }

    @Override
    protected void showEditDialog(EntityItem item) { showFormDialog("Edit Program", item); }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_program, null);
        AutoCompleteTextView spinnerDept = dialogView.findViewById(R.id.spinnerDept);
        TextInputEditText etName = dialogView.findViewById(R.id.etProgramName);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        List<String> deptNames = new ArrayList<>();
        for (JsonObject d : departments) deptNames.add(safe(d, "department_name"));
        spinnerDept.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, deptNames));

        if (item != null) {
            etName.setText(safe(item.raw, "program_name"));
            spinnerDept.setText(safe(item.raw, "department_name"), false);
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String deptName = spinnerDept.getText().toString().trim();
                    if (name.isEmpty() || deptName.isEmpty()) { toast("All fields required."); return; }

                    String deptId = "";
                    for (JsonObject dept : departments) {
                        if (safe(dept, "department_name").equals(deptName)) {
                            deptId = safe(dept, "department_id");
                            break;
                        }
                    }
                    if (deptId.isEmpty()) { toast("Invalid department."); return; }

                    Map<String, String> fields = new HashMap<>();
                    fields.put("action", item == null ? "add_program" : "update_program");
                    if (item != null) fields.put("id", hiddenId.getText().toString());
                    fields.put("program_name", name);
                    fields.put("department_id", deptId);
                    performRequest(fields, item == null ? "Program added!" : "Program updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_program");
        fields.put("id", item.id);
        performRequest(fields, "Program deleted!");
    }
}
