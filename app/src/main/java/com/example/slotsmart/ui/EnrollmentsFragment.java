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

public class EnrollmentsFragment extends BaseEntityFragment {

    private List<JsonObject> studentList = new ArrayList<>();
    private List<JsonObject> courseList = new ArrayList<>();

    @Override
    protected String getListAction() { return "get_enrollments"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null) return list;
        JsonElement rows = data;
        if (data.isJsonObject() && data.getAsJsonObject().has("data"))
            rows = data.getAsJsonObject().get("data");
        if (!rows.isJsonArray()) return list;
        for (JsonElement el : rows.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            list.add(new EntityItem(
                    safe(o, "enrollment_id"),
                    safe(o, "student_name"),
                    safe(o, "course_name") + " · " + safe(o, "semester"),
                    safe(o, "batch_year"),
                    o));
        }
        return list;
    }

    @Override
    protected void onDataLoaded(JsonElement data) {
        Map<String, String> f = new HashMap<>();
        f.put("action", "get_dropdowns");
        api.request(f).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonObject()) {
                    JsonObject obj = response.body().data.getAsJsonObject();
                    courseList.clear();
                    if (obj.has("courses"))
                        for (JsonElement el : obj.get("courses").getAsJsonArray())
                            courseList.add(el.getAsJsonObject());
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
        });
        loadStudents();
    }

    private void loadStudents() {
        Map<String, String> f = new HashMap<>();
        f.put("action", "get_students");
        api.request(f).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null) {
                    JsonElement rows = response.body().data;
                    if (rows.isJsonArray()) {
                        studentList.clear();
                        for (JsonElement el : rows.getAsJsonArray())
                            studentList.add(el.getAsJsonObject());
                    }
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void showAddDialog() { showFormDialog("Add Enrollment", null); }

    @Override
    protected void showEditDialog(EntityItem item) { showFormDialog("Edit Enrollment", item); }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_enrollment, null);
        AutoCompleteTextView spinnerYear = dialogView.findViewById(R.id.spinnerBatchYear);
        TextInputEditText etSemester = dialogView.findViewById(R.id.etSemester);
        AutoCompleteTextView spinnerStudent = dialogView.findViewById(R.id.spinnerEnrollStudent);
        AutoCompleteTextView spinnerCourse = dialogView.findViewById(R.id.spinnerEnrollCourse);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        List<String> years = new ArrayList<>();
        for (int y = 2026; y >= 2015; y--) years.add(String.valueOf(y));
        spinnerYear.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, years));

        List<String> sNames = new ArrayList<>();
        for (JsonObject s : studentList)
            sNames.add(safe(s, "student_name") + " (" + safe(s, "registration_number") + ")");
        spinnerStudent.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, sNames));

        List<String> cNames = new ArrayList<>();
        for (JsonObject c : courseList)
            cNames.add(safe(c, "course_code") + " – " + safe(c, "course_name"));
        spinnerCourse.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, cNames));

        if (item != null) {
            spinnerYear.setText(safe(item.raw, "batch_year"), false);
            etSemester.setText(safe(item.raw, "semester"));
            spinnerStudent.setText(safe(item.raw, "student_name"), false);
            spinnerCourse.setText(safe(item.raw, "course_code") + " – " + safe(item.raw, "course_name"), false);
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String year = spinnerYear.getText().toString().trim();
                    String sem = etSemester.getText() != null ? etSemester.getText().toString().trim() : "";
                    String sEntry = spinnerStudent.getText().toString().trim();
                    String cEntry = spinnerCourse.getText().toString().trim();
                    if (year.isEmpty() || sem.isEmpty() || sEntry.isEmpty() || cEntry.isEmpty()) {
                        toast("All fields required."); return;
                    }

                    String sId = "", cId = "";
                    for (JsonObject s : studentList) {
                        String label = safe(s, "student_name") + " (" + safe(s, "registration_number") + ")";
                        if (label.equals(sEntry)) { sId = safe(s, "student_id"); break; }
                    }
                    for (JsonObject c : courseList) {
                        String label = safe(c, "course_code") + " – " + safe(c, "course_name");
                        if (label.equals(cEntry)) { cId = safe(c, "course_id"); break; }
                    }
                    if (sId.isEmpty() || cId.isEmpty()) { toast("Invalid selection."); return; }

                    Map<String, String> fields = new HashMap<>();
                    fields.put("action", item == null ? "add_enrollment" : "update_enrollment");
                    if (item != null) fields.put("id", hiddenId.getText().toString());
                    fields.put("batch_year", year);
                    fields.put("semester", sem);
                    fields.put("student_id", sId);
                    fields.put("course_id", cId);
                    performRequest(fields, item == null ? "Enrollment added!" : "Enrollment updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_enrollment");
        fields.put("id", item.id);
        performRequest(fields, "Enrollment deleted!");
    }
}
