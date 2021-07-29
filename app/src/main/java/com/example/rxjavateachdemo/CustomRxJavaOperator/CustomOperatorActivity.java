package com.example.rxjavateachdemo.CustomRxJavaOperator;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rxjavateachdemo.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.functions.Consumer;

public class CustomOperatorActivity extends AppCompatActivity {
    private static final String TAG = "CustomOperatorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_operator);

        Button button = findViewById(R.id.button);

        RxView.clicks(button)
                .throttleFirst(20, TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Throwable {
                        Log.d(TAG, "accept:防抖测试 ");
                    }
                });
    }
}