package com.example.upload_image;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ServiceAPI {
    String BASE_URL = "http://app.iotstar.vn:8081/appfoods/";

    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    ServiceAPI serviceApi = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ServiceAPI.class);

    @Multipart
    @POST("updateimages.php")
    Call<ResponseBody> updateImage(
            @Part MultipartBody.Part id,
            @Part MultipartBody.Part image
    );

    @Multipart
    @POST("updateimages.php")
    Call<ResponseBody> updateImageWithField(
            @Part("id") RequestBody id,
            @Part MultipartBody.Part image
    );
}