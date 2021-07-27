package com.example.rxjavateachdemo.RxHook;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rxjavateachdemo.R;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class RxHookActivity extends AppCompatActivity {
    private static final String TAG = "RxHookActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_hook);

        //RxJava Hook demo
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) {
                e.onNext("A");
            }
        })
                .map(new Function<Object, Boolean>() {
                    @Override
                    public Boolean apply(Object o) {
                        return true;
                    }
                })

                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {

                    }
                });

        RxJavaPlugins.setOnObservableAssembly(new Function<Observable, Observable>() {
            @Override
            public Observable apply(Observable observable) throws Exception {
                Log.d(TAG, "apply: 整个项目 全局 监听 到底有多少地方使用 RxJava:" + observable);
                return null; // 不破坏人家的功能
            }
        });
    }
}