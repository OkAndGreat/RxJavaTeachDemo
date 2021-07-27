package com.example.rxjavateachdemo.ObserverModel;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.rxjavateachdemo.R;

public class ObserverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observer);


        // 需要推送的文案
        String msg = "123 321 1234567";

        // 创建一个微信公众号服务（被观察者）
        Observable server = new WechatServerObservable();

        // 创建 用户 （观察者）  多个
        Observer a = new User("a");
        Observer b = new User("b");
        Observer c = new User("c");
        Observer d = new User("d");

        // 订阅  注意：这里的订阅还是 被观察者.订阅(观察者)  关注
        server.addObserver(a);
        server.addObserver(b);
        server.addObserver(c);
        server.addObserver(d);

        // 微信平台 发生了 改变
        // server.pushMessage(msg);

        // b用户 取消了关注
        System.out.println("============================================");
        server.removeObserver(b);

        // 再推送消息
        server.pushMessage(msg);
    }
}