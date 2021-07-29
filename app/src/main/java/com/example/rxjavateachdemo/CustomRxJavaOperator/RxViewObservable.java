package com.example.rxjavateachdemo.CustomRxJavaOperator;

import android.view.View;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

public class RxViewObservable extends Observable<Object> {
    View view;
    // 用来给onNext传参的,实际无用
    private static final Object EVENT = new Object();

    public RxViewObservable(View view) {
        this.view = view;
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super Object> observer) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                observer.onNext(EVENT);
            }
        });
    }
}


