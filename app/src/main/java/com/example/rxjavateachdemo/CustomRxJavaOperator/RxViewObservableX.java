package com.example.rxjavateachdemo.CustomRxJavaOperator;

import android.os.Looper;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

//可以用disposable来中断流
public class RxViewObservableX extends Observable<Object> {
    View view;
    // 用来给onNext传参的,实际无用
    private static final Object EVENT = new Object();

    public RxViewObservableX(View view) {
        this.view = view;
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super Object> observer) {
        MyListener myListener = new MyListener(view, observer);
        observer.onSubscribe(myListener);
        view.setOnClickListener(myListener);
    }

    static final class MyListener implements View.OnClickListener, Disposable {

        private final View view;
        private Observer<Object> observer;
        //线程安全
        private final AtomicBoolean isDisposable = new AtomicBoolean();

        public MyListener(View view, Observer<Object> observer) {
            this.view = view;
            this.observer = observer;
        }

        @Override
        public void onClick(View v) {
            //流没有被中断才能继续传递
            if (isDisposed() == false) {
                observer.onNext(EVENT);
            }
        }

        // 如果调用了中断
        @Override
        public void dispose() {
            // 如果没有中断过,才能取消view.setOnClickListener(null);
            if (isDisposable.compareAndSet(false, true)) {
                // 主线程 很好的中断
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    view.setOnClickListener(null);

                } else {
                    AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            view.setOnClickListener(null);
                        }
                    });
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return isDisposable.get();
        }
    }
}

