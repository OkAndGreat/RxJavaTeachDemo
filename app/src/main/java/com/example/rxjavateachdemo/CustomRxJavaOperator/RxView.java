package com.example.rxjavateachdemo.CustomRxJavaOperator;

import android.view.View;

import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class RxView {
    public static Observable<Object> clicks(View view) {
        Objects.requireNonNull(view, "source is null");
        return RxJavaPlugins.onAssembly(new RxViewObservable(view));
    }
}
