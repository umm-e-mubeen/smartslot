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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FacultyFragment extends BaseEntityFragment {

    private List<JsonObject> departments = new ArrayList<>();

    @Override
    protected String getListAction() { return "get_faculty"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null || !data.isJsonArray()) return list;
        for (JsonElement el : data.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            list.add(new EntityItem(
                    safe(o, "faculty_id"),
                    safe(o, "faculty_name"),
                    safe(o, "email") + " · " + safe(o, "department_name"),
                    null,
                    o));
        }
        return list;
    }

    @Override
    protected void onDataLoaded(JsonElement data) {
        loadDropdowns();
    }

    private void loadDropdowns() {
        Map<String, String> f = new HashMap<>();
        f.put("action", "get_departments");
        api.request(f).enqueue(new Callback<ApiResponse>() {
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
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void showAddDialog() { showFormDialog("Add Faculty", null); }

    @Override
    protected void showEditDialog(EntityItem item) { showFormDialog("Edit Faculty", item); }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_faculty, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etFacultyName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etFacultyEmail);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etFacultyPhone);
        AutoCompleteTextView spinnerDept = dialogView.findViewById(R.id.spinnerFacultyDept);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        List<String> deptNames = new ArrayList<>();
        for (JsonObject d : departments) deptNames.add(safe(d, "department_name"));
        spinnerDept.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, deptNames));

        if (item != null) {
            etName.setText(safe(item.raw, "faculty_name"));
            etEmail.setText(safe(item.raw, "email"));
            etPhone.setText(safe(item.raw, "phone_number"));
            spinnerDept.setText(safe(item.raw, "department_name"), false);
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String deptName = spinnerDept.getText().toString().trim();
                    if (name.isEmpty() || email.isEmpty() || deptName.isEmpty()) {
                        toast("Name, email and department are required."); return;
                    }
                    String deptId = "";
                    for (JsonObject dept : departments) {
                        if (safe(dept, "department_name").equals(deptName)) {
                            deptId = safe(dept, "department_id"); break;
                        }
                    }
                    if (deptId.isEmpty()) { toast("Invalid department."); return; }

                    Map<String, String> fields = new HashMap<>();
                    fields.put("action", item == null ? "add_faculty" : "update_faculty");
                    if (item != null) fields.put("id", hiddenId.getText().toString());
                    fields.put("faculty_name", name);
                    fields.put("email", email);
                    fields.put("phone_number", phone);
                    fields.put("department_id", deptId);
                    performRequest(fields, item == null ? "Faculty added!" : "Faculty updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_faculty");
        fields.put("id", item.id);
        performRequest(fields, "Faculty deleted!");
    }
}
