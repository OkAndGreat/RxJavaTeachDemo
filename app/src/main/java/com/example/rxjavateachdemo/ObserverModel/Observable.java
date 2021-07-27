package com.example.rxjavateachdemo.ObserverModel;

public interface Observable {

    // 注册观察者
    void addObserver(Observer observer);

    // 取消注册
    void removeObserver(Observer observer);

    // 被观察者发出了改变
    void notifyObservers();

    // 微信公众号的服务 编辑部门 发布一条消息
    void pushMessage(String message);
}
