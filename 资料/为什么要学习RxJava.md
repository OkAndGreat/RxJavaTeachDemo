## 基础知识

[干货集中营 (gank.io)](https://gank.io/post/560e15be2dca930e00da1083)

## 观察者设计模式

### 观察者模式的定义

在对象之间定义了一对多的依赖，当一个对象改变状态，依赖它的对象会收到通知并自动更新。

### 使用场景例子

有一个微信公众号服务，不定时发布一些消息，关注公众号就可以收到推送消息，取消关注就收不到推送消息。

### Android中的观察者模式

android源码中也有很多使用了观察者模式，比如OnClickListener、ContentObserver、android.database.Observable等

以OnClickListener为例

OnClickListener为观察者，View为被观察者，通过setOnClickListener俩者完成订阅关系，View在被点击的时候通知观察者，也就是OnClickListener.

## RxJava源码

### 1.观察者和被观察者是怎么完成订阅关系并发送信息的

```java
//demo1
        //观察者和被观察者怎么完成订阅过程并传递消息的
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) {
                emitter.onNext(1);
            }
        })

                //导火索，将被观察者与观察者连接在一起
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Integer integer) {
                        Log.d(TAG, "onNext: 我是自定义Observer，我收到被观察者的信息啦！数字为-->" + integer);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
```

首先看到Observable.create

```java
public static <T> Observable<T> create(@NonNull ObservableOnSubscribe<T> source) {
    Objects.requireNonNull(source, "source is null");
    return RxJavaPlugins.onAssembly(new ObservableCreate<>(source));
}
```

首先进行了判空，然后返回RxJavaPlugins.onAssembly(new ObservableCreate<>(source))

对于RxJavaPlugins.onAssembly在分析RxJava源码时经常能看见，它是一个Hook函数，可以用来对RxJava的运行过程进行全局监听

```java
public static <T> Observable<T> onAssembly(@NonNull Observable<T> source) {
        Function<? super Observable, ? extends Observable> f = onObservableAssembly;
        if (f != null) {
            return apply(f, source);
        }
        return source;
    }
```

Function<? super Observable, ? extends Observable> f我们没有去设置的话一般为null，所以会直接返回source，也就是说如果没有特意设置我们传递什么参数给RxJavaPlugins.onAssembly它就会原封不动的返回，因此我们下面分析RxJava源码时再碰到这个Hook方法可以忽略。

因此Observable.create的结果就是创建了ObservableCreate这个类并把我们创建的匿名内部类当作参数传递了进去

```java
final ObservableOnSubscribe<T> source;

public ObservableCreate(ObservableOnSubscribe<T> source) {
    this.source = source;
}
```

再看subscribe

```java
@SchedulerSupport(SchedulerSupport.NONE)
    @Override
    public final void subscribe(@NonNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);

            Objects.requireNonNull(observer, "The RxJavaPlugins.onSubscribe hook returned a null Observer. Please change the handler provided to RxJavaPlugins.setOnObservableSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins");

            subscribeActual(observer);
        } catch (NullPointerException e) { // NOPMD
            throw e;
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // can't call onError because no way to know if a Disposable has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);

            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }
    }
```

核心代码只有一句

subscribeActual(observer)

直接看到ObservableCreate的subscribeActual

```java
@Override
protected void subscribeActual(Observer<? super T> observer) {
    CreateEmitter<T> parent = new CreateEmitter<>(observer);
    observer.onSubscribe(parent);

    try {
        source.subscribe(parent);
    } catch (Throwable ex) {
        Exceptions.throwIfFatal(ex);
        parent.onError(ex);
    }
}
```

可以看到这里new了一个CreateEmitter并将观察者当作参数传给了它，后面再通过source.subscribe(parent);将CreateEmitter与被观察者进行订阅，看到这里我们的第一个目的就达到了.

接下来看被观察者是怎么发送消息给观察者的

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
    @Override
    public void subscribe(@NonNull ObservableEmitter<Integer> emitter) {
        emitter.onNext(1);
    }
})
```

source就是这里我们创建的匿名内部类，通过emitter.onNext(1);消息就从被观察者传到了观察者

```java
@Override
public void onNext(T t) {
    if (t == null) {
        onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
        return;
    }
    if (!isDisposed()) {
        observer.onNext(t);
    }
}
```

分析完上述源码后发现RxJava用的并不是纯粹的的观察者模式，而是扩展过的。

### 2.操作符的原理

### 3.线程调度的原理

## RxJava核心思想

用RxJava写出来的代码像一条有始有终的河流，我们在起点放入事件，然后事件从起点流向终点的过程中我们不断对事件进行拦截并更改，最终到终点时是我们想要的事件。

什么意思？

比如说我们要下载一张图片并展示，起点事件就是图片String类型的URL，然后我们可以拦截这个事件，然后将URL转变为Bitmap再将事件流向下游，这样事件就发生了更改，我们也可以再次拦截事件然后打一个log然后再次将事件传向下游，这样到终点时事件就是bitmap信息而不是String了，我们此时可以拿得到的bitmap信息做想做的事情，比如赋值给ImageView

```java
// 网络图片的链接地址
private final static String PATH = "https://s1.ax1x.com/2020/10/11/0gGYbF.jpg";

public void rxJavaDownloadImageAction() {

        // 起点
        Observable.just(PATH)
                .map(new Function<String, Bitmap>() {
                    @Override
                    public Bitmap apply(String s) throws Exception {
                        URL url = new URL(PATH);
                        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                        httpURLConnection.setConnectTimeout(5000);
                        int responseCode = httpURLConnection.getResponseCode(); 
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStream inputStream = httpURLConnection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            return bitmap;
                        }
                        return null;
                    }
                })
                .map(new Function<Bitmap, Bitmap>() {
                    @Override
                    public Bitmap apply(Bitmap bitmap) throws Exception {
                        Log.d(TAG, "apply: 是这个时候下载了图片啊:" + System.currentTimeMillis());
                        return bitmap;
                    }
                })
                //线程切换
                //表示在io线程中下载图片并在主线程中展示图片
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                //终点
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(DownloadActivity.this);
                        progressDialog.setTitle("download run");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        image.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        if (progressDialog != null)
                            progressDialog.dismiss();
                    }
                });
    }
```

## RxHook

