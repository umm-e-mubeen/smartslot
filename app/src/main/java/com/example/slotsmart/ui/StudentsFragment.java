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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentsFragment extends BaseEntityFragment {

    private List<JsonObject> departments = new ArrayList<>();
    private List<JsonObject> programs = new ArrayList<>();

    @Override
    protected String getListAction() { return "get_students"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null) return list;
        JsonElement rows = data;
        if (data.isJsonObject() && data.getAsJsonObject().has("data")) {
            rows = data.getAsJsonObject().get("data");
        }
        if (!rows.isJsonArray()) return list;
        for (JsonElement el : rows.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            String type = safe(o, "student_type");
            list.add(new EntityItem(
                    safe(o, "student_id"),
                    safe(o, "student_name"),
                    safe(o, "registration_number") + " · " + safe(o, "program_name"),
                    type.isEmpty() ? null : type,
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
        f.put("action", "get_dropdowns");
        api.request(f).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonObject()) {
                    JsonObject obj = response.body().data.getAsJsonObject();
                    departments.clear();
                    programs.clear();
                    if (obj.has("departments") && obj.get("departments").isJsonArray())
                        for (JsonElement el : obj.get("departments").getAsJsonArray())
                            departments.add(el.getAsJsonObject());
                    if (obj.has("programs") && obj.get("programs").isJsonArray())
                        for (JsonElement el : obj.get("programs").getAsJsonArray())
                            programs.add(el.getAsJsonObject());
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void showAddDialog() { showFormDialog("Add Student", null); }

    @Override
    protected void showEditDialog(EntityItem item) { showFormDialog("Edit Student", item); }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_student, null);
        TextInputEditText etRegNo = dialogView.findViewById(R.id.etRegNo);
        TextInputEditText etName = dialogView.findViewById(R.id.etStudentName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etStudentEmail);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etStudentPhone);
        TextInputEditText etDOB = dialogView.findViewById(R.id.etStudentDOB);
        AutoCompleteTextView spinnerDept = dialogView.findViewById(R.id.spinnerStudentDept);
        AutoCompleteTextView spinnerProgram = dialogView.findViewById(R.id.spinnerStudentProgram);
        AutoCompleteTextView spinnerGender = dialogView.findViewById(R.id.spinnerStudentGender);
        AutoCompleteTextView spinnerType = dialogView.findViewById(R.id.spinnerStudentType);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        List<String> deptNames = new ArrayList<>();
        for (JsonObject d : departments) deptNames.add(safe(d, "department_name"));
        spinnerDept.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, deptNames));

        List<String> progNames = new ArrayList<>();
        for (JsonObject p : programs) progNames.add(safe(p, "program_name"));
        spinnerProgram.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, progNames));

        spinnerGender.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList("Male", "Female", "Other")));

        spinnerType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList("regular", "transfer")));

        if (item != null) {
            etRegNo.setText(safe(item.raw, "registration_number"));
            etName.setText(safe(item.raw, "student_name"));
            etEmail.setText(safe(item.raw, "email"));
            etPhone.setText(safe(item.raw, "phone_number"));
            etDOB.setText(safe(item.raw, "dob"));
            spinnerDept.setText(safe(item.raw, "department_name"), false);
            spinnerProgram.setText(safe(item.raw, "program_name"), false);
            spinnerGender.setText(safe(item.raw, "gender"), false);
            spinnerType.setText(safe(item.raw, "student_type"), false);
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String reg = etRegNo.getText() != null ? etRegNo.getText().toString().trim() : "";
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String dob = etDOB.getText() != null ? etDOB.getText().toString().trim() : "";
                    String deptName = spinnerDept.getText().toString().trim();
                    String progName = spinnerProgram.getText().toString().trim();
                    String gender = spinnerGender.getText().toString().trim();
                    String type = spinnerType.getText().toString().trim();

                    if (reg.isEmpty() || name.isEmpty() || email.isEmpty() || deptName.isEmpty()) {
                        toast("Reg No, name, email and department are required."); return;
                    }

                    String deptId = "", progId = "";
                    for (JsonObject dept : departments)
                        if (safe(dept, "department_name").equals(deptName))
                            deptId = safe(dept, "department_id");
                    for (JsonObject prog : programs)
                        if (safe(prog, "program_name").equals(progName))
                            progId = safe(prog, "program_id");

                    Map<String, String> fields = new HashMap<>();
                    fields.put("action", item == null ? "add_student" : "update_student");
                    if (item != null) fields.put("id", hiddenId.getText().toString());
                    fields.put("registration_number", reg);
                    fields.put("student_name", name);
                    fields.put("email", email);
                    if (!phone.isEmpty()) fields.put("phone_number", phone);
                    if (!dob.isEmpty()) fields.put("dob", dob);
                    if (!gender.isEmpty()) fields.put("gender", gender);
                    fields.put("department_id", deptId);
                    if (!progId.isEmpty()) fields.put("program_id", progId);
                    if (!type.isEmpty()) fields.put("student_type", type);
                    performRequest(fields, item == null ? "Student added!" : "Student updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_student");
        fields.put("id", item.id);
        performRequest(fields, "Student deleted!");
    }
}
