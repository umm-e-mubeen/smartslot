package com.example.slotsmart.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.slotsmart.R;
import com.example.slotsmart.SessionManager;
import com.example.slotsmart.adapter.EntityAdapter;
import com.example.slotsmart.model.ApiResponse;
import com.example.slotsmart.model.EntityItem;
import com.example.slotsmart.network.ApiClient;
import com.example.slotsmart.network.AdminApiService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

/**
 * Reusable base for all admin list screens.
 * Subclasses implement getListAction(), buildItems(), showAddDialog(), showEditDialog().
 */
public abstract class BaseEntityFragment extends Fragment {

    protected EntityAdapter adapter;
    protected SwipeRefreshLayout swipeRefresh;
    protected TextView tvEmpty;
    protected SessionManager session;
    protected AdminApiService api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entity_list, container, false);

        session = new SessionManager(requireContext());
        api = ApiClient.getService(session.getServerUrl());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

        adapter = new EntityAdapter(new EntityAdapter.OnActionListener() {
            @Override
            public void onEdit(EntityItem item) {
                showEditDialog(item);
            }

            @Override
            public void onDelete(EntityItem item) {
                confirmDelete(item);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::loadData);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString());
                updateEmpty();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabAdd.setOnClickListener(v -> showAddDialog());

        loadData();
        return view;
    }

    protected void loadData() {
        swipeRefresh.setRefreshing(true);
        Map<String, String> fields = new HashMap<>();
        fields.put("action", getListAction());
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().success) {
                    JsonElement data = response.body().data;
                    List<EntityItem> items = buildItems(data);
                    adapter.setData(items);
                    updateEmpty();
                    onDataLoaded(data);
                } else {
                    toast("Failed to load data.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                swipeRefresh.setRefreshing(false);
                toast("Network error: " + t.getMessage());
            }
        });
    }

    protected void performRequest(Map<String, String> fields, String successMsg) {
        api.request(fields).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call,
                                   @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().success) {
                        toast(successMsg);
                        loadData();
                    } else {
                        toast(response.body().message);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                toast("Network error: " + t.getMessage());
            }
        });
    }

    private void confirmDelete(EntityItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete")
                .setMessage(getString(R.string.delete_confirm))
                .setPositiveButton("Delete", (d, w) -> deleteItem(item))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    protected void updateEmpty() {
        if (isAdded()) {
            tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    protected void toast(String msg) {
        if (isAdded()) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    protected String safe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : "";
    }

    /** Action name passed to admin_handler.php to fetch the list. */
    protected abstract String getListAction();

    /** Convert raw JSON array from server into EntityItem list for the adapter. */
    protected abstract List<EntityItem> buildItems(JsonElement data);

    /** Show add dialog for this entity type. */
    protected abstract void showAddDialog();

    /** Show pre-filled edit dialog for an existing record. */
    protected abstract void showEditDialog(EntityItem item);

    /** Delete the given item — subclass builds the right fields. */
    protected abstract void deleteItem(EntityItem item);

    /** Called after data is loaded; subclasses can use it to cache dropdown data. */
    protected void onDataLoaded(JsonElement data) {}
}
