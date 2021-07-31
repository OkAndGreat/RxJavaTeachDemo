package com.example.rxjavateachdemo.RetrofitDemo;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.http.GET;

public interface Api {

    @GET("/article/top/json")
    Call<bean> getData();

}
