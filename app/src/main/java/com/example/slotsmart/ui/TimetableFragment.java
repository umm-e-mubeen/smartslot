package com.example.slotsmart.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slotsmart.R;
import com.example.slotsmart.SessionManager;
import com.example.slotsmart.model.ApiResponse;
import com.example.slotsmart.network.ApiClient;
import com.example.slotsmart.network.AdminApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimetableFragment extends Fragment {

    private SessionManager session;
    private AdminApiService api;

    private AutoCompleteTextView spinnerSession, spinnerRun, spinnerDept, spinnerFaculty, spinnerProgram;
    private AutoCompleteTextView spinnerBatch, spinnerCourse, spinnerSemester, spinnerRoom;
    private MaterialButton btnApplyFilter, btnToggleFilters, btnToggleLandscape;
    private MaterialButton btnGenerateGA, btnSavedTimetables, btnPrint, btnExportCSV;
    private ScrollView filterContainer;
    private TextView tvViewingInfo, tvStatus;
    private TableLayout timetableGrid;

    private List<String> sessions = new ArrayList<>();
    private List<JsonObject> runs = new ArrayList<>();
    private List<JsonObject> departments = new ArrayList<>();
    private List<JsonObject> faculties = new ArrayList<>();
    private List<JsonObject> programs = new ArrayList<>();
    private List<JsonObject> batches = new ArrayList<>();
    private List<JsonObject> courses = new ArrayList<>();
    private List<String> semesters = new ArrayList<>();
    private List<JsonObject> rooms = new ArrayList<>();

    private boolean filtersExpanded = true;
    private boolean isLandscape = true;

    private String currentSession = "";
    private Map<String, Map<String, List<JsonObject>>> timetableData = new HashMap<>();
    private Map<String, List<JsonObject>> cellDataStore = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_timetable, container, false);

            session = new SessionManager(requireContext());
            api = ApiClient.getService(session.getServerUrl());

        spinnerSession = view.findViewById(R.id.spinnerSession);
        spinnerRun = view.findViewById(R.id.spinnerRun);
        spinnerDept = view.findViewById(R.id.spinnerDept);
        spinnerFaculty = view.findViewById(R.id.spinnerFaculty);
        spinnerProgram = view.findViewById(R.id.spinnerProgram);
        spinnerBatch = view.findViewById(R.id.spinnerBatch);
        spinnerCourse = view.findViewById(R.id.spinnerCourse);
        spinnerSemester = view.findViewById(R.id.spinnerSemester);
        spinnerRoom = view.findViewById(R.id.spinnerRoom);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnToggleFilters = view.findViewById(R.id.btnToggleFilters);
        btnToggleLandscape = view.findViewById(R.id.btnToggleLandscape);
        filterContainer = view.findViewById(R.id.filterContainer);
        tvViewingInfo = view.findViewById(R.id.tvViewingInfo);
        tvStatus = view.findViewById(R.id.tvStatus);
        timetableGrid = view.findViewById(R.id.timetableGrid);

        btnGenerateGA = view.findViewById(R.id.btnGenerateGA);
        btnSavedTimetables = view.findViewById(R.id.btnSavedTimetables);
        btnPrint = view.findViewById(R.id.btnPrint);
        btnExportCSV = view.findViewById(R.id.btnExportCSV);

        btnToggleFilters.setOnClickListener(v -> toggleFilters());
        btnToggleLandscape.setOnClickListener(v -> toggleOrientation());
        btnApplyFilter.setOnClickListener(v -> {
            loadTimetable();
            // Auto-close filters after applying so timetable is visible
            if (filtersExpanded) {
                toggleFilters();
            }
        });
        btnGenerateGA.setOnClickListener(v -> showGenerateDialog());
        btnSavedTimetables.setOnClickListener(v -> showSavedTimetablesDialog());
        btnPrint.setOnClickListener(v -> toast("Print feature (export to PDF) coming soon!"));
        btnExportCSV.setOnClickListener(v -> toast("CSV export coming soon!"));

            loadDropdowns();
            return view;
        } catch (Exception e) {
            // Crash protection - show error and return minimal view
            Toast.makeText(requireContext(),
                "Error loading Timetable: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            e.printStackTrace(); // Print to Logcat

            // Return a simple view to prevent crash
            View errorView = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
            return errorView;
        }
    }

    private void toggleFilters() {
        filtersExpanded = !filtersExpanded;
        filterContainer.setVisibility(filtersExpanded ? View.VISIBLE : View.GONE);
        btnToggleFilters.setText(filtersExpanded ? "▼" : "▶");
    }

    private void toggleOrientation() {
        isLandscape = !isLandscape;
        if (isLandscape) {
            requireActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            toast("Switched to Landscape");
        } else {
            requireActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            toast("Switched to Portrait");
        }
    }

    private void loadDropdowns() {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_dropdowns");
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonObject()) {
                    JsonObject obj = response.body().data.getAsJsonObject();
                    departments.clear();
                    faculties.clear();
                    programs.clear();
                    batches.clear();
                    courses.clear();
                    semesters.clear();
                    rooms.clear();

                    if (obj.has("departments") && obj.get("departments").isJsonArray())
                        for (JsonElement el : obj.get("departments").getAsJsonArray())
                            departments.add(el.getAsJsonObject());

                    if (obj.has("faculty") && obj.get("faculty").isJsonArray())
                        for (JsonElement el : obj.get("faculty").getAsJsonArray())
                            faculties.add(el.getAsJsonObject());

                    if (obj.has("programs") && obj.get("programs").isJsonArray())
                        for (JsonElement el : obj.get("programs").getAsJsonArray())
                            programs.add(el.getAsJsonObject());

                    if (obj.has("batches") && obj.get("batches").isJsonArray())
                        for (JsonElement el : obj.get("batches").getAsJsonArray())
                            batches.add(el.getAsJsonObject());

                    if (obj.has("courses") && obj.get("courses").isJsonArray())
                        for (JsonElement el : obj.get("courses").getAsJsonArray())
                            courses.add(el.getAsJsonObject());

                    if (obj.has("semesters") && obj.get("semesters").isJsonArray())
                        for (JsonElement el : obj.get("semesters").getAsJsonArray()) {
                            // Handle both string format and object format
                            if (el.isJsonPrimitive()) {
                                semesters.add(el.getAsString());
                            } else if (el.isJsonObject()) {
                                JsonObject semObj = el.getAsJsonObject();
                                if (semObj.has("semester")) {
                                    semesters.add(semObj.get("semester").getAsString());
                                }
                            }
                        }

                    if (obj.has("rooms") && obj.get("rooms").isJsonArray())
                        for (JsonElement el : obj.get("rooms").getAsJsonArray())
                            rooms.add(el.getAsJsonObject());

                    populateDropdowns();
                    loadSessions();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                toast("Failed to load filters: " + t.getMessage());
            }
        });
    }

    private void loadSessions() {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_sessions");
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonArray()) {
                    sessions.clear();
                    for (JsonElement el : response.body().data.getAsJsonArray()) {
                        sessions.add(el.getAsString());
                    }
                    if (!sessions.isEmpty()) {
                        currentSession = sessions.get(0);
                        populateDropdowns();
                        loadTimetable();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {}
        });
    }

    private void loadRunsForSession(String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return;
        if (!isAdded()) return;

        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_timetable_runs");
        fields.put("base_session", sessionName);

        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonArray()) {
                    runs.clear();
                    for (JsonElement el : response.body().data.getAsJsonArray()) {
                        runs.add(el.getAsJsonObject());
                    }

                    // Update ONLY the Run dropdown (don't trigger populateDropdowns - causes infinite loop!)
                    if (spinnerRun != null) {
                        List<String> runLabels = new ArrayList<>();
                        runLabels.add("Latest Version");
                        for (JsonObject r : runs) {
                            String label = safe(r, "run_label");
                            if (label.isEmpty()) label = "Run " + safe(r, "run_id");
                            runLabels.add(label);
                        }
                        spinnerRun.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, runLabels));
                        spinnerRun.setText("Latest Version", false);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                // Silent fail - runs are optional
            }
        });
    }

    private void populateDropdowns() {
        if (!isAdded()) return;

        try {
            if (spinnerSession != null) {
                spinnerSession.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, sessions));
                if (!currentSession.isEmpty())
                    spinnerSession.setText(currentSession, false);

                // Load runs when session changes (NO infinite loop - don't call populateDropdowns)
                spinnerSession.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedSession = sessions.get(position);
                    loadRunsForSession(selectedSession);
                    // Show Run filter when session is selected
                    if (spinnerRun != null && spinnerRun.getParent() != null) {
                        ((View) spinnerRun.getParent().getParent()).setVisibility(View.VISIBLE);
                    }
                });
            }

            if (spinnerRun != null) {
                List<String> runLabels = new ArrayList<>();
                runLabels.add("Latest Version");
                for (JsonObject r : runs) {
                    String label = safe(r, "run_label");
                    if (label.isEmpty()) label = "Run " + safe(r, "run_id");
                    runLabels.add(label);
                }
                spinnerRun.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, runLabels));
                spinnerRun.setText("Latest Version", false);

                // Hide by default (like web) - only show when session selected
                if (spinnerRun.getParent() != null) {
                    ((View) spinnerRun.getParent().getParent()).setVisibility(View.GONE);
                }
            }

            if (spinnerDept != null) {
                List<String> deptNames = new ArrayList<>();
                deptNames.add("All Departments");
                for (JsonObject d : departments) deptNames.add(safe(d, "department_name"));
                spinnerDept.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, deptNames));
            }

            if (spinnerFaculty != null) {
                List<String> facNames = new ArrayList<>();
                facNames.add("All Faculty");
                for (JsonObject f : faculties) facNames.add(safe(f, "faculty_name"));
                spinnerFaculty.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, facNames));
            }

            if (spinnerBatch != null) {
                List<String> batchYears = new ArrayList<>();
                batchYears.add("All Batches");
                for (JsonObject b : batches) batchYears.add(safe(b, "batch_year"));
                spinnerBatch.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, batchYears));
            }

            if (spinnerProgram != null) {
                List<String> progNames = new ArrayList<>();
                progNames.add("All Programs");
                for (JsonObject p : programs) progNames.add(safe(p, "program_name"));
                spinnerProgram.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, progNames));
            }

            if (spinnerCourse != null) {
                List<String> courseNames = new ArrayList<>();
                courseNames.add("All Courses");
                for (JsonObject c : courses) courseNames.add(safe(c, "course_name"));
                spinnerCourse.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, courseNames));
            }

            if (spinnerSemester != null) {
                List<String> semesterList = new ArrayList<>();
                semesterList.add("All Semesters");
                semesterList.addAll(semesters);
                spinnerSemester.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, semesterList));
            }

            if (spinnerRoom != null) {
                List<String> roomNumbers = new ArrayList<>();
                roomNumbers.add("All Rooms");
                for (JsonObject r : rooms) roomNumbers.add(safe(r, "room_number"));
                spinnerRoom.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, roomNumbers));
            }
        } catch (Exception e) {
            toast("Error populating filters: " + e.getMessage());
        }
    }

    private void loadTimetable() {
        if (spinnerSession == null) {
            toast("Filters not initialized yet.");
            return;
        }

        String session = spinnerSession.getText().toString().trim();
        if (session.isEmpty()) {
            toast("Please select a session.");
            return;
        }

        currentSession = session;
        if (tvViewingInfo != null) tvViewingInfo.setText("Viewing: " + session);
        if (tvStatus != null) tvStatus.setText("Status: Loading...");

        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_generated_timetable");
        fields.put("session", session);

        // Run/Version filter (optional - for viewing specific timetable versions)
        if (spinnerRun != null) {
            String runText = spinnerRun.getText().toString().trim();
            if (!runText.isEmpty() && !runText.equals("Latest Version")) {
                // Find the run_id for the selected run
                for (JsonObject r : runs) {
                    String label = safe(r, "run_label");
                    if (label.isEmpty()) label = "Run " + safe(r, "run_id");
                    if (label.equals(runText)) {
                        fields.put("run_id", safe(r, "run_id"));
                        break;
                    }
                }
            }
        }

        // Department filter
        String dept = spinnerDept != null ? spinnerDept.getText().toString().trim() : "";
        if (!dept.isEmpty() && !dept.equals("All Departments")) {
            for (JsonObject d : departments) {
                if (safe(d, "department_name").equals(dept)) {
                    fields.put("department_id", safe(d, "department_id"));
                    break;
                }
            }
        } else {
            fields.put("department_id", "all");
        }

        // Faculty filter
        String fac = spinnerFaculty != null ? spinnerFaculty.getText().toString().trim() : "";
        if (!fac.isEmpty() && !fac.equals("All Faculty")) {
            for (JsonObject f : faculties) {
                if (safe(f, "faculty_name").equals(fac)) {
                    fields.put("faculty_id", safe(f, "faculty_id"));
                    break;
                }
            }
        } else {
            fields.put("faculty_id", "all");
        }

        // Batch filter
        String batch = spinnerBatch != null ? spinnerBatch.getText().toString().trim() : "";
        if (!batch.isEmpty() && !batch.equals("All Batches")) {
            fields.put("batch_year", batch);
        } else {
            fields.put("batch_year", "");
        }

        // Program filter
        String prog = spinnerProgram != null ? spinnerProgram.getText().toString().trim() : "";
        if (!prog.isEmpty() && !prog.equals("All Programs")) {
            for (JsonObject p : programs) {
                if (safe(p, "program_name").equals(prog)) {
                    fields.put("program_id", safe(p, "program_id"));
                    break;
                }
            }
        } else {
            fields.put("program_id", "all");
        }

        // Course filter
        String course = spinnerCourse != null ? spinnerCourse.getText().toString().trim() : "";
        if (!course.isEmpty() && !course.equals("All Courses")) {
            for (JsonObject c : courses) {
                if (safe(c, "course_name").equals(course)) {
                    fields.put("course_id", safe(c, "course_id"));
                    break;
                }
            }
        } else {
            fields.put("course_id", "all");
        }

        // Semester filter
        String semester = spinnerSemester != null ? spinnerSemester.getText().toString().trim() : "";
        if (!semester.isEmpty() && !semester.equals("All Semesters")) {
            fields.put("semester", semester);
        } else {
            fields.put("semester", "");
        }

        // Room filter
        String room = spinnerRoom != null ? spinnerRoom.getText().toString().trim() : "";
        if (!room.isEmpty() && !room.equals("All Rooms")) {
            for (JsonObject r : rooms) {
                if (safe(r, "room_number").equals(room)) {
                    fields.put("room_id", safe(r, "room_id"));
                    break;
                }
            }
        } else {
            fields.put("room_id", "all");
        }

        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().success && response.body().data != null) {
                        parseTimetable(response.body().data);
                        tvStatus.setText("Status: Ready");
                    } else {
                        toast(response.body().message);
                        tvStatus.setText("Status: Error");
                    }
                } else {
                    tvStatus.setText("Status: Failed");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                toast("Network error: " + t.getMessage());
                tvStatus.setText("Status: Error");
            }
        });
    }

    private void parseTimetable(JsonElement data) {
        timetableData.clear();

        // Handle both array (old format) and object with 'scheduled' array (new format)
        JsonArray scheduled = null;
        if (data.isJsonArray()) {
            scheduled = data.getAsJsonArray();
        } else if (data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();
            if (obj.has("scheduled") && obj.get("scheduled").isJsonArray()) {
                scheduled = obj.get("scheduled").getAsJsonArray();
            }
        }

        if (scheduled != null) {
            for (JsonElement el : scheduled) {
                JsonObject entry = el.getAsJsonObject();
                String day = safe(entry, "day");
                String slot = safe(entry, "start_time") + " - " + safe(entry, "end_time");

                if (!timetableData.containsKey(day)) {
                    timetableData.put(day, new LinkedHashMap<>());
                }
                if (!timetableData.get(day).containsKey(slot)) {
                    timetableData.get(day).put(slot, new ArrayList<>());
                }
                timetableData.get(day).get(slot).add(entry);
            }
        }

        renderTimetable();
    }

    private void renderTimetable() {
        timetableGrid.removeAllViews();

        List<String> days = new ArrayList<>();
        days.add("Monday");
        days.add("Tuesday");
        days.add("Wednesday");
        days.add("Thursday");
        days.add("Friday");
        days.add("Saturday");

        List<String> allSlots = new ArrayList<>();
        for (String day : days) {
            if (timetableData.containsKey(day)) {
                for (String slot : timetableData.get(day).keySet()) {
                    if (!allSlots.contains(slot)) {
                        allSlots.add(slot);
                    }
                }
            }
        }

        // Header row
        TableRow headerRow = new TableRow(requireContext());
        TextView cornerCell = createCell("Time", true);
        headerRow.addView(cornerCell);
        for (String day : days) {
            headerRow.addView(createCell(day.substring(0, 3), true));
        }
        timetableGrid.addView(headerRow);

        // Data rows
        for (String slot : allSlots) {
            TableRow row = new TableRow(requireContext());
            row.addView(createCell(slot, true));

            for (String day : days) {
                List<JsonObject> classes = new ArrayList<>();
                if (timetableData.containsKey(day) && timetableData.get(day).containsKey(slot)) {
                    classes = timetableData.get(day).get(slot);
                }
                row.addView(createClassCell(classes));
            }
            timetableGrid.addView(row);
        }
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setPadding(8, 10, 8, 10);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(10);
        if (isHeader) {
            tv.setBackgroundColor(Color.parseColor("#F8F9FA"));
            tv.setTextColor(Color.parseColor("#212529"));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tv.setBackgroundColor(Color.WHITE);
            tv.setTextColor(Color.parseColor("#6C757D"));
        }

        // Use dp values for consistent sizing
        float density = getResources().getDisplayMetrics().density;
        int cellWidth = isHeader ? (int)(100 * density) : (int)(140 * density);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                cellWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(2, 2, 2, 2);
        tv.setLayoutParams(params);
        return tv;
    }

    private View createClassCell(List<JsonObject> classes) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.WHITE);
        container.setPadding(3, 3, 3, 3);
        container.setMinimumHeight(100);

        // Use dp values for consistent sizing
        float density = getResources().getDisplayMetrics().density;
        int cellWidth = (int)(140 * density);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                cellWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(2, 2, 2, 2);
        container.setLayoutParams(params);

        if (classes.isEmpty()) {
            return container;
        }

        // Store all classes for this cell for the modal
        String cellId = "cell_" + System.currentTimeMillis() + "_" + classes.hashCode();
        cellDataStore.put(cellId, classes);

        // Only show the first class in the grid
        JsonObject cls = classes.get(0);
        {
            View classCard = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_timetable_cell, container, false);

            TextView tvCode = classCard.findViewById(R.id.tvCourseCode);
            TextView tvName = classCard.findViewById(R.id.tvCourseName);
            TextView tvFaculty = classCard.findViewById(R.id.tvFacultyName);
            TextView tvProgram = classCard.findViewById(R.id.tvProgramName);
            TextView tvCreditsRoom = classCard.findViewById(R.id.tvCreditsRoom);
            TextView tvEnrollment = classCard.findViewById(R.id.tvEnrollment);
            TextView tvType = classCard.findViewById(R.id.tvComponentType);
            android.widget.ProgressBar progressEnrollment = classCard.findViewById(R.id.progressEnrollment);

            // Basic info
            tvCode.setText(safe(cls, "course_code"));
            tvName.setText(safe(cls, "course_name"));
            tvFaculty.setText("👤 " + safe(cls, "faculty_name"));
            tvProgram.setText("📚 " + safe(cls, "program_name"));

            // Credits and Room
            String credits = safe(cls, "credit_hours");
            String room = safe(cls, "room_number");
            tvCreditsRoom.setText("🏫 " + room + " • " + (credits.isEmpty() ? "--" : credits));

            // Component type badge
            String compType = safe(cls, "component_type");
            tvType.setText("Lab".equalsIgnoreCase(compType) ? "LAB" : "LEC");

            // Enrollment percentage
            int enrolled = 0;
            int capacity = 0;
            try {
                enrolled = Integer.parseInt(safe(cls, "enrolled_students"));
            } catch (NumberFormatException e) {}
            try {
                capacity = Integer.parseInt(safe(cls, "capacity"));
            } catch (NumberFormatException e) {}

            int percentage = capacity > 0 ? Math.round((enrolled * 100.0f) / capacity) : 0;
            tvEnrollment.setText(enrolled + "/" + capacity + " (" + percentage + "%)");
            progressEnrollment.setProgress(percentage);

            // Color based on enrollment
            int barColor;
            if (percentage > 90) {
                barColor = Color.parseColor("#DC3545"); // Red
            } else if (percentage > 70) {
                barColor = Color.parseColor("#FD7E14"); // Orange
            } else {
                barColor = Color.parseColor("#198754"); // Green
            }
            progressEnrollment.getProgressDrawable().setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN);

            // Card styling based on type
            MaterialCardView card = (MaterialCardView) classCard;
            if ("Lab".equalsIgnoreCase(compType)) {
                card.setCardBackgroundColor(Color.parseColor("#E7F1FF"));
                card.setStrokeColor(Color.parseColor("#0D6EFD"));
                tvType.setTextColor(Color.parseColor("#0D6EFD"));
            } else {
                card.setCardBackgroundColor(Color.parseColor("#F0FDF4"));
                card.setStrokeColor(Color.parseColor("#198754"));
                tvType.setTextColor(Color.parseColor("#198754"));
            }

            container.addView(classCard);
        }

        // Add "+ X more" button if there are multiple classes
        if (classes.size() > 1) {
            MaterialButton btnExpand = new MaterialButton(requireContext());
            btnExpand.setText("+ " + (classes.size() - 1) + " more");
            btnExpand.setTextSize(8);
            btnExpand.setTextColor(Color.WHITE);
            btnExpand.setBackgroundColor(Color.parseColor("#4361EE"));
            btnExpand.setAllCaps(false);
            btnExpand.setPadding(8, 2, 8, 2);
            btnExpand.setMinHeight(0);
            btnExpand.setMinimumHeight(0);

            // Set corner radius programmatically
            btnExpand.setCornerRadius(12);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (28 * getResources().getDisplayMetrics().density)); // 28dp height
            btnParams.setMargins(0, 4, 0, 0);
            btnExpand.setLayoutParams(btnParams);

            final String finalCellId = cellId;
            btnExpand.setOnClickListener(v -> showViewSlotModal(finalCellId));

            container.addView(btnExpand);
        }

        return container;
    }

    private void showGenerateDialog() {
        String session = spinnerSession.getText().toString().trim();
        if (session.isEmpty()) {
            toast("Please select a session first.");
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Generate Timetable (GA)")
                .setMessage("Generate optimised timetable for \"" + session + "\"?\n\n" +
                        "Runs in the background — you can watch progress live.\n" +
                        "A new version will be saved without overwriting previous ones.")
                .setPositiveButton("Generate", (d, w) -> {
                    startGAGeneration(session);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startGAGeneration(String sessionName) {
        tvStatus.setText("Status: Starting...");
        btnGenerateGA.setEnabled(false);
        btnGenerateGA.setText("⏳ Starting...");

        // Step 1: Start GA job (POST to ga_handler.php, NOT admin_handler.php)
        String gaUrl = session.getServerUrl() + "ga_handler.php";

        RequestBody formBody = new FormBody.Builder()
                .add("session", sessionName)
                .build();

        Request request = new Request.Builder()
                .url(gaUrl)
                .post(formBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        toast("Network error: " + e.getMessage());
                        resetGAButton();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JsonObject json = new Gson().fromJson(body, JsonObject.class);
                    boolean success = json.has("success") && json.get("success").getAsBoolean();
                    if (success && json.has("job_id")) {
                        String jobId = json.get("job_id").getAsString();
                        // Step 2: Start polling
                        pollGAStatus(jobId, sessionName, System.currentTimeMillis());
                    } else {
                        String msg = json.has("message") ? json.get("message").getAsString() : "Could not start GA.";
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                toast(msg);
                                resetGAButton();
                            });
                        }
                    }
                } catch (Exception e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            toast("Failed to parse response");
                            resetGAButton();
                        });
                    }
                }
            }
        });
    }

    private Handler gaHandler = new Handler(Looper.getMainLooper());
    private Runnable gaPollRunnable;
    private AlertDialog currentDialog;

    private void pollGAStatus(String jobId, String sessionName, long startTime) {
        String statusUrl = this.session.getServerUrl() + "ga_status.php?job_id=" + jobId;

        Request request = new Request.Builder()
                .url(statusUrl)
                .get()
                .build();

        new OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                // Network blip - keep polling, don't abort
                schedulePoll(jobId, sessionName, startTime);
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JsonObject st = new Gson().fromJson(body, JsonObject.class);
                    boolean done = st.has("done") && st.get("done").getAsBoolean();
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;

                    if (!done) {
                        // Still running - update UI and keep polling
                        String progress = st.has("progress") ? st.get("progress").getAsString() : "Running...";
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                btnGenerateGA.setText("⏳ " + progress + " (" + elapsed + "s)");
                                tvStatus.setText("Status: " + progress);
                            });
                        }
                        schedulePoll(jobId, sessionName, startTime);
                    } else {
                        // Job finished
                        boolean failed = st.has("failed") && st.get("failed").getAsBoolean();
                        if (failed) {
                            String msg = st.has("progress") ? st.get("progress").getAsString() : "GA failed.";
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    toast(msg);
                                    resetGAButton();
                                    tvStatus.setText("Status: Failed");
                                });
                            }
                        } else {
                            int classCount = st.has("classes_count") ? st.get("classes_count").getAsInt() : 0;
                            String msg = st.has("message") ? st.get("message").getAsString() : "Done";
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    toast(msg + " | " + classCount + " classes | " + elapsed + "s");
                                    resetGAButton();
                                    tvStatus.setText("Status: Done — " + classCount + " classes (" + elapsed + "s)");
                                    currentSession = sessionName;
                                    spinnerSession.setText(sessionName, false);
                                    loadSessions();
                                    loadTimetable();
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    schedulePoll(jobId, sessionName, startTime);
                }
            }
        });
    }

    private void schedulePoll(String jobId, String sessionName, long startTime) {
        if (gaPollRunnable != null) {
            gaHandler.removeCallbacks(gaPollRunnable);
        }
        gaPollRunnable = () -> pollGAStatus(jobId, sessionName, startTime);
        gaHandler.postDelayed(gaPollRunnable, 2000); // Poll every 2 seconds
    }

    private void resetGAButton() {
        btnGenerateGA.setEnabled(true);
        btnGenerateGA.setText("⚙ Generate Timetable (GA)");
    }

    private void showSavedTimetablesDialog() {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_timetable_sessions"); // ✅ Exact web action
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonArray()) {
                    showSavedTimetablesList(response.body().data.getAsJsonArray());
                } else {
                    toast("No saved timetables found.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                toast("Failed to load saved timetables.");
            }
        });
    }

    private void showSavedTimetablesList(JsonArray runs) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_saved_timetables, null);
        RecyclerView recycler = dialogView.findViewById(R.id.recyclerSavedTimetables);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptySaved);

        if (runs.size() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            recycler.setAdapter(new SavedTimetablesAdapter(runs));
        }

        currentDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Saved Timetables")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .show();
    }

    private class SavedTimetablesAdapter extends RecyclerView.Adapter<SavedTimetablesAdapter.ViewHolder> {
        private List<JsonObject> items = new ArrayList<>();

        SavedTimetablesAdapter(JsonArray data) {
            for (JsonElement el : data) items.add(el.getAsJsonObject());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saved_timetable, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            JsonObject item = items.get(pos);
            String sessionName = safe(item, "session_name");
            String runLabel = safe(item, "run_label");
            String totalClasses = safe(item, "total_classes");
            String fitness = safe(item, "fitness_score");

            h.tvSession.setText(sessionName);
            h.tvLabel.setText(runLabel.isEmpty() ? sessionName : runLabel);
            h.tvMeta.setText(totalClasses + " classes · Fitness: " + (fitness.equals("0") ? "Perfect" : fitness));

            h.btnLoad.setOnClickListener(v -> {
                // Close dialog (matches web behavior - dialog closes on load)
                if (currentDialog != null) currentDialog.dismiss();

                // Load this saved timetable (matches web viewSavedRun function)
                currentSession = sessionName;
                spinnerSession.setText(sessionName, false);
                gaHandler.postDelayed(() -> loadTimetable(), 300);
                toast("Loading: " + (runLabel.isEmpty() ? sessionName : runLabel));
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSession, tvLabel, tvMeta;
            MaterialButton btnLoad;

            ViewHolder(View v) {
                super(v);
                tvSession = v.findViewById(R.id.tvSavedSession);
                tvLabel = v.findViewById(R.id.tvSavedLabel);
                tvMeta = v.findViewById(R.id.tvSavedMeta);
                btnLoad = v.findViewById(R.id.btnLoadTimetable);
            }
        }
    }

    private String safe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : "";
    }

    private AlertDialog currentViewSlotDialog;

    private void showViewSlotModal(String cellId) {
        List<JsonObject> classes = cellDataStore.get(cellId);
        if (classes == null || classes.isEmpty()) {
            toast("No classes found");
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_view_slot, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvSlotModalTitle);
        RecyclerView recycler = dialogView.findViewById(R.id.recyclerSlotClasses);

        // Set title with day and time info
        JsonObject firstClass = classes.get(0);
        String day = safe(firstClass, "day");
        String time = safe(firstClass, "start_time");
        tvTitle.setText(classes.size() + " Class" + (classes.size() > 1 ? "es" : "") +
                       " on " + day + " at " + time);

        // Set up RecyclerView with GridLayoutManager for 2 columns
        recycler.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        recycler.setAdapter(new SlotClassesAdapter(classes));

        currentViewSlotDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        currentViewSlotDialog.show();
    }

    private class SlotClassesAdapter extends RecyclerView.Adapter<SlotClassesAdapter.ClassViewHolder> {
        private List<JsonObject> classes;

        SlotClassesAdapter(List<JsonObject> classes) {
            this.classes = classes;
        }

        @NonNull
        @Override
        public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_slot_class_card, parent, false);
            return new ClassViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ClassViewHolder h, int position) {
            JsonObject cls = classes.get(position);

            h.tvCourseCode.setText(safe(cls, "course_code"));
            h.tvCourseName.setText(safe(cls, "course_name"));
            h.tvFaculty.setText("👤 " + safe(cls, "faculty_name"));
            h.tvCredits.setText("📚 Credit Hours: " + safe(cls, "credit_hours"));
            h.tvRoom.setText("🏫 " + safe(cls, "room_number"));

            String compType = safe(cls, "component_type");
            boolean isLab = "Lab".equalsIgnoreCase(compType);
            h.tvType.setText(isLab ? "LAB" : "LEC");

            // Enrollment
            int enrolled = 0, capacity = 0;
            try { enrolled = Integer.parseInt(safe(cls, "enrolled_students")); } catch (Exception e) {}
            try { capacity = Integer.parseInt(safe(cls, "capacity")); } catch (Exception e) {}
            int percentage = capacity > 0 ? Math.round((enrolled * 100.0f) / capacity) : 0;
            h.tvEnrollment.setText(enrolled + "/" + capacity + " (" + percentage + "%)");
            h.progressEnrollment.setProgress(percentage);

            // Color based on enrollment
            int barColor = percentage > 90 ? Color.parseColor("#DC3545") :
                          percentage > 70 ? Color.parseColor("#FD7E14") :
                          Color.parseColor("#198754");
            h.progressEnrollment.getProgressDrawable().setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN);
            h.tvEnrollment.setTextColor(barColor);

            // Card styling
            MaterialCardView card = (MaterialCardView) h.itemView;
            if (isLab) {
                card.setCardBackgroundColor(Color.parseColor("#E7F1FF"));
                card.setStrokeColor(Color.parseColor("#0D6EFD"));
                h.tvType.setBackgroundColor(Color.parseColor("#0D6EFD"));
            } else {
                card.setCardBackgroundColor(Color.parseColor("#F0FDF4"));
                card.setStrokeColor(Color.parseColor("#198754"));
                h.tvType.setBackgroundColor(Color.parseColor("#198754"));
            }

            // Click to open edit modal
            h.itemView.setOnClickListener(v -> {
                // Dismiss view slot modal first
                if (currentViewSlotDialog != null && currentViewSlotDialog.isShowing()) {
                    currentViewSlotDialog.dismiss();
                }
                // Wait for dismiss animation to complete before opening edit modal
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showEditEntryModal(cls);
                }, 200);
            });
        }

        @Override
        public int getItemCount() { return classes.size(); }

        class ClassViewHolder extends RecyclerView.ViewHolder {
            TextView tvCourseCode, tvCourseName, tvFaculty, tvCredits, tvRoom, tvType, tvEnrollment;
            android.widget.ProgressBar progressEnrollment;

            ClassViewHolder(View v) {
                super(v);
                tvCourseCode = v.findViewById(R.id.tvModalCourseCode);
                tvCourseName = v.findViewById(R.id.tvModalCourseName);
                tvFaculty = v.findViewById(R.id.tvModalFaculty);
                tvCredits = v.findViewById(R.id.tvModalCredits);
                tvRoom = v.findViewById(R.id.tvModalRoom);
                tvType = v.findViewById(R.id.tvModalType);
                tvEnrollment = v.findViewById(R.id.tvModalEnrollment);
                progressEnrollment = v.findViewById(R.id.progressModalEnrollment);
            }
        }
    }

    private void showEditEntryModal(JsonObject entry) {
        if (!isAdded()) return;

        try {
            View dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_edit_timetable_entry, null);

            TextView tvInfo = dialogView.findViewById(R.id.tvEditInfo);
            AutoCompleteTextView spinnerRoom = dialogView.findViewById(R.id.spinnerEditRoom);
            AutoCompleteTextView spinnerSlot = dialogView.findViewById(R.id.spinnerEditSlot);
            View conflictCard = dialogView.findViewById(R.id.conflictCard);
            TextView tvConflict = dialogView.findViewById(R.id.tvConflictWarning);
            EditText hiddenId = dialogView.findViewById(R.id.hiddenTimetableId);

            // Display class info
            String courseCode = safe(entry, "course_code");
            String courseName = safe(entry, "course_name");
            String compType = safe(entry, "component_type");
            tvInfo.setText(courseCode + " — " + courseName + "\n" + compType);

            String timetableId = safe(entry, "timetable_id");
            String currentRoomId = safe(entry, "room_id");
            String currentSlotId = safe(entry, "slot_id");

            if (timetableId.isEmpty()) {
                toast("Error: Missing timetable ID");
                return;
            }

            hiddenId.setText(timetableId);

            // Check if rooms are loaded
            if (rooms == null || rooms.isEmpty()) {
                toast("Loading rooms data...");
                // Load rooms and then show modal again
                loadDropdowns();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showEditEntryModal(entry);
                }, 1000);
                return;
            }

            // Populate rooms dropdown
            List<String> roomLabels = new ArrayList<>();
            for (JsonObject r : rooms) {
                String roomNumber = safe(r, "room_number");
                String roomType = safe(r, "room_type");
                String capacity = safe(r, "capacity");
                roomLabels.add(roomNumber + " (" + roomType + ", cap: " + capacity + ")");
            }
            spinnerRoom.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, roomLabels));

            // Set current room
            for (int i = 0; i < rooms.size(); i++) {
                if (safe(rooms.get(i), "room_id").equals(currentRoomId)) {
                    spinnerRoom.setText(roomLabels.get(i), false);
                    break;
                }
            }

            // Get all slots from API
            loadSlotsForEdit(spinnerSlot, currentSlotId);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("✏️ Edit Class Entry")
                    .setView(dialogView)
                    .setPositiveButton("Save Changes", null)
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.setOnShowListener(d -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    conflictCard.setVisibility(View.GONE);

                    // Get selected room ID
                    String selectedRoomLabel = spinnerRoom.getText().toString().trim();
                    String selectedRoomId = "";
                    for (int i = 0; i < roomLabels.size(); i++) {
                        if (roomLabels.get(i).equals(selectedRoomLabel)) {
                            selectedRoomId = safe(rooms.get(i), "room_id");
                            break;
                        }
                    }

                    // Get selected slot ID (stored in tag)
                    String selectedSlotId = (String) spinnerSlot.getTag();

                    if (selectedRoomId.isEmpty() || selectedSlotId == null || selectedSlotId.isEmpty()) {
                        toast("Please select both room and time slot");
                        return;
                    }

                    // Call API to update
                    updateTimetableEntry(timetableId, selectedRoomId, selectedSlotId, dialog, conflictCard, tvConflict);
                });
            });

            dialog.show();
        } catch (Exception e) {
            if (isAdded()) {
                toast("Error opening edit modal: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadSlotsForEdit(AutoCompleteTextView spinnerSlot, String currentSlotId) {
        if (!isAdded() || spinnerSlot == null) return;

        Map<String, String> fields = new HashMap<>();
        fields.put("action", "get_slots");
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && response.body().data.isJsonArray()) {
                    List<JsonObject> allSlots = new ArrayList<>();
                    for (JsonElement el : response.body().data.getAsJsonArray()) {
                        allSlots.add(el.getAsJsonObject());
                    }

                    List<String> slotLabels = new ArrayList<>();
                    for (JsonObject s : allSlots) {
                        String day = safe(s, "day");
                        String start = safe(s, "start_time").substring(0, 5);
                        String end = safe(s, "end_time").substring(0, 5);
                        slotLabels.add(day + " " + start + "–" + end);
                    }

                    spinnerSlot.setAdapter(new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, slotLabels));

                    // Set current slot and store slot ID mapping
                    for (int i = 0; i < allSlots.size(); i++) {
                        if (safe(allSlots.get(i), "slot_id").equals(currentSlotId)) {
                            spinnerSlot.setText(slotLabels.get(i), false);
                            spinnerSlot.setTag(currentSlotId);
                            break;
                        }
                    }

                    // Update tag when selection changes
                    spinnerSlot.setOnItemClickListener((parent, view, position, id) -> {
                        spinnerSlot.setTag(safe(allSlots.get(position), "slot_id"));
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                toast("Failed to load time slots");
            }
        });
    }

    private void updateTimetableEntry(String timetableId, String roomId, String slotId,
                                      AlertDialog dialog, View conflictCard, TextView tvConflict) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "update_timetable_entry");
        fields.put("timetable_id", timetableId);
        fields.put("room_id", roomId);
        fields.put("slot_id", slotId);

        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().success) {
                        toast("Class updated successfully!");
                        dialog.dismiss();
                        loadTimetable(); // Reload to show changes
                    } else {
                        // Show conflict error
                        conflictCard.setVisibility(View.VISIBLE);
                        tvConflict.setText("⚠️ " + response.body().message);
                    }
                } else {
                    toast("Failed to update");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                toast("Network error: " + t.getMessage());
            }
        });
    }

    private void toast(String msg) {
        if (isAdded()) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
