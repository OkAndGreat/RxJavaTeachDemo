package com.example.rxjavateachdemo.SimpleDemo;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rxjavateachdemo.R;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;

public class SimpleActivity extends AppCompatActivity {
    private static final String TAG = "SimpleActivity";

    Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);

        //demo1
        //观察者和被观察者怎么完成订阅过程并传递消息的
//        Observable.create(new ObservableOnSubscribe<Integer>() {
//            @Override
//            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) {
//                emitter.onNext(1);
//            }
//        })
//
//                //导火索，将被观察者与观察者连接在一起
//                .subscribe(new Observer<Integer>() {
//                    @Override
//                    public void onSubscribe(@NonNull Disposable d) {
//                        disposable=d;
//                    }
//
//                    @Override
//                    public void onNext(@NonNull Integer integer) {
//                        Log.d(TAG, "onNext: 我是自定义Observer，我收到被观察者的信息啦！数字为-->" + integer);
//                    }
//
//                    @Override
//                    public void onError(@NonNull Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onComplete() {
//
//                    }
//                });

        //demo2
        //map操作符的源码
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) {
                emitter.onNext(1);
            }
        })
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) {
                        return integer.toString();
                    }
                })
                //导火索，将被观察者与观察者连接在一起
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull String s) {
                        Log.d(TAG, "onNext: 我是自定义Observer，我收到被观察者的信息啦！信息的类型为-->"
                                + s.getClass().getName());
                    }


                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        //demo3
        //线程调度的原理
    }

    @Override
    protected void onDestroy() {
        if(disposable!=null)
            disposable.dispose();
        super.onDestroy();
    }
}