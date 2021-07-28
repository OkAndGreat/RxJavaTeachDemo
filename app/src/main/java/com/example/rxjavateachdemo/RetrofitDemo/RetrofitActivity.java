package com.example.rxjavateachdemo.RetrofitDemo;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rxjavateachdemo.R;

import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitActivity extends AppCompatActivity {
    private static final String TAG = "RetrofitActivity";

    Disposable disposable;

    Retrofit retrofit;

    String BASEURL = "https://www.wanandroid.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retrofit);

        //常规用法
//        retrofit = new Retrofit.Builder()
//                .baseUrl(BASEURL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        retrofit.create(Api.class).getData().enqueue(new Callback<bean>() {
//            @Override
//            public void onResponse(Call<bean> call, Response<bean> response) {
//                Log.d(TAG, "onResponse: -->" + response.body().getData().get(0).toString());
//            }
//
//            @Override
//            public void onFailure(Call<bean> call, Throwable t) {
//
//            }
//        });

        //与RxJava结合
        retrofit = new Retrofit.Builder()
                .baseUrl(BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        retrofit.create(Api.class)
                .getData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<bean>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;
            }

            @Override
            public void onNext(@NonNull bean bean) {
                Log.d(TAG, "onNext: -->"+bean.getData().get(0).toString());
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }


    //防止内存泄漏
    @Override
    protected void onDestroy() {
        if (disposable != null)
            if (!disposable.isDisposed())
                disposable.dispose();
        super.onDestroy();
    }
}