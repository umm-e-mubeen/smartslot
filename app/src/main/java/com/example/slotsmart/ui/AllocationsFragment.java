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

public class AllocationsFragment extends BaseEntityFragment {

    private List<JsonObject> facultyList = new ArrayList<>();
    private List<JsonObject> courseList = new ArrayList<>();

    @Override
    protected String getListAction() { return "get_allocations"; }

    @Override
    protected List<EntityItem> buildItems(JsonElement data) {
        List<EntityItem> list = new ArrayList<>();
        if (data == null || !data.isJsonArray()) return list;
        for (JsonElement el : data.getAsJsonArray()) {
            JsonObject o = el.getAsJsonObject();
            list.add(new EntityItem(
                    safe(o, "allocation_id"),
                    safe(o, "faculty_name") + " → " + safe(o, "course_name"),
                    "Session: " + safe(o, "session_name"),
                    null,
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
                    facultyList.clear(); courseList.clear();
                    if (obj.has("faculty"))
                        for (JsonElement el : obj.get("faculty").getAsJsonArray())
                            facultyList.add(el.getAsJsonObject());
                    if (obj.has("courses"))
                        for (JsonElement el : obj.get("courses").getAsJsonArray())
                            courseList.add(el.getAsJsonObject());
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {}
        });
    }

    @Override
    protected void showAddDialog() { showFormDialog("Add Allocation", null); }

    @Override
    protected void showEditDialog(EntityItem item) { showFormDialog("Edit Allocation", item); }

    private void showFormDialog(String title, EntityItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_allocation, null);
        TextInputEditText etSession = dialogView.findViewById(R.id.etAllocSession);
        AutoCompleteTextView spinnerFaculty = dialogView.findViewById(R.id.spinnerAllocFaculty);
        AutoCompleteTextView spinnerCourse = dialogView.findViewById(R.id.spinnerAllocCourse);
        EditText hiddenId = dialogView.findViewById(R.id.hiddenId);

        List<String> fNames = new ArrayList<>();
        for (JsonObject f : facultyList) fNames.add(safe(f, "faculty_name"));
        spinnerFaculty.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, fNames));

        List<String> cNames = new ArrayList<>();
        for (JsonObject c : courseList)
            cNames.add(safe(c, "course_code") + " – " + safe(c, "course_name"));
        spinnerCourse.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, cNames));

        if (item != null) {
            etSession.setText(safe(item.raw, "session_name"));
            spinnerFaculty.setText(safe(item.raw, "faculty_name"), false);
            spinnerCourse.setText(safe(item.raw, "course_code") + " – " + safe(item.raw, "course_name"), false);
            hiddenId.setText(item.id);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String session = etSession.getText() != null ? etSession.getText().toString().trim() : "";
                    String fName = spinnerFaculty.getText().toString().trim();
                    String cEntry = spinnerCourse.getText().toString().trim();
                    if (session.isEmpty() || fName.isEmpty() || cEntry.isEmpty()) {
                        toast("All fields required."); return;
                    }

                    String fId = "", cId = "";
                    for (JsonObject f : facultyList)
                        if (safe(f, "faculty_name").equals(fName)) fId = safe(f, "faculty_id");
                    for (JsonObject c : courseList) {
                        String label = safe(c, "course_code") + " – " + safe(c, "course_name");
                        if (label.equals(cEntry)) { cId = safe(c, "course_id"); break; }
                    }
                    if (fId.isEmpty() || cId.isEmpty()) { toast("Invalid selection."); return; }

                    Map<String, String> fields = new HashMap<>();
                    fields.put("action", item == null ? "add_allocation" : "update_allocation");
                    if (item != null) fields.put("id", hiddenId.getText().toString());
                    fields.put("session_name", session);
                    fields.put("faculty_id", fId);
                    fields.put("course_id", cId);
                    performRequest(fields, item == null ? "Allocation added!" : "Allocation updated!");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void deleteItem(EntityItem item) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "delete_allocation");
        fields.put("id", item.id);
        performRequest(fields, "Allocation deleted!");
    }
}
