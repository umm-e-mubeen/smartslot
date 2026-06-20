package com.example.slotsmart.network;

import com.example.slotsmart.model.ApiResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface AdminApiService {

    @FormUrlEncoded
    @POST("admin_handler.php")
    Call<ApiResponse> request(@FieldMap Map<String, String> fields);
}
