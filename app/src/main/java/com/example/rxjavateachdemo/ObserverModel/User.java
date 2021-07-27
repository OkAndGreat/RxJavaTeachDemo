package com.example.rxjavateachdemo.ObserverModel;

// 观察者 实现
public class User implements Observer {

    private String name;
    private String message;

    public User(String name) {
        this.name = name;
    }

    @Override
    public void update(Object value) {
        this.message = (String) value;

        // 读取消息
        readMessage();
    }

    private void readMessage() {
        System.out.println(name + " 收到了推送消息：" + message);
    }
}
