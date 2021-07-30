## 基础知识

[干货集中营 (gank.io)](https://gank.io/post/560e15be2dca930e00da1083)

## 观察者设计模式

### 观察者模式的定义

在对象之间定义了一对多的依赖，当一个对象改变状态，依赖它的对象会收到通知并自动更新。

![](https://img-blog.csdnimg.cn/6987d1e2d7ba4bc397bbc11e3cc37e7f.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L20wXzUwMjYyMjE0,size_16,color_FFFFFF,t_70#pic_center)

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

对于RxJavaPlugins.onAssembly在分析RxJava源码时经常能看见，它是一个Hook方法，可以用来对RxJava的运行过程进行全局监听

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

Observable创建过程时序图：

![](https://github.com/OkAndGreat/RxJavaTeachDemo/blob/master/assets/Observable%E5%88%9B%E5%BB%BA%E8%BF%87%E7%A8%8B%E6%97%B6%E5%BA%8F%E5%9B%BE.png?raw=true)

Observable 与 Observer 订阅的过程时序图：

![](https://github.com/OkAndGreat/RxJavaTeachDemo/blob/master/assets/Observable%20%E4%B8%8E%20Observer%20%E8%AE%A2%E9%98%85%E7%9A%84%E8%BF%87%E7%A8%8B%E6%97%B6%E5%BA%8F%E5%9B%BE.png?raw=true)

### 2.操作符的原理

```java
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
```

和上面的代码相比只加了

```java
.map(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) {
                return integer.toString();
            }
        })
```

由上面的分析可知

```java
Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) {
                emitter.onNext(1);
            }
        })
```

做的就是new 了一个ObservableCreate并将传入的匿名内部类存储下来（这个匿名内部类ObservableOnSubscribe是真正干活的类，也即发送消息），然后返回ObservableCreate

因此要分析map操作符，我们看ObservableCreate的map方法，而ObservableCreate是继承自Observable，它也没有重写这个方法，所以看到Observable的map方法

```
public final <R> Observable<R> map(@NonNull Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMap<>(this, mapper));
    }
```

这里又有一个钩子方法，通过上面的分析可知可以忽略，直接看new ObservableMap<>(this, mapper)做了什么事即可

```java
public ObservableMap(ObservableSource<T> source, Function<? super T, ? extends U> function) {
    super(source);
    this.function = function;
}
```

这里其实和ObservableCreate做的事情差不多，将ObservableCreate存储下来并且将转换函数存储了下来

接着看

```java
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
```

直接看到ObservableMap的subscribeActual方法

```java
@Override
public void subscribeActual(Observer<? super U> t) {
    source.subscribe(new MapObserver<T, U>(t, function));
}
```

这里我们来对比下ObservableCreate的subscribeActual方法

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

![pic2](https://github.com/OkAndGreat/RxJavaTeachDemo/blob/master/assets/pic2.png?raw=true)

![](https://github.com/OkAndGreat/RxJavaTeachDemo/blob/master/assets/map%E6%93%8D%E4%BD%9C%E7%AC%A6.png?raw=true)

其实之前就说过，ObservableCreate才是真正发送消息的那个类，在我们上面的demo中，直接由ObservableCreate发送消息给观察者，而在这里ObservableCreate先将消息传给ObservableMap，然后ObservableMap收到消息后可以把消息更改后再继续传给下游。

来分析源码

source.subscribe(new MapObserver<T, U>(t, function));

其实就是

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

在demo1中ObservableCreate subscribeActual方法传入的真正的observer也即是

```java
new Observer<String>() {
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
                }
```

，而在这里我们传入的是

MapObserver，MapObserver储存了原来本来应该传给ObservableCreate subscribeActual方法的那个Observer和转换函数

当ObservableCreate的subscribeActual方法执行到source.subscribe(parent);

时，emitter.onNext(1);也即是执行observer.onNext(t);

在demo1中此时消息应该就传给我们创建的那个Observer了，但是现在消息先传给了MapObserver，

其实这里可以猜到其实就是MapObserver拦截了消息并利用存储下来的转换函数将消息改变后再发给我们创建的的那个Observer

我们来看看源码

```java
@Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            U v;

            try {
                v = Objects.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            downstream.onNext(v);
        }
```

关键是这俩行

 v = Objects.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");

downstream.onNext(v);



### 3.线程调度的原理

如果理解了上面讲的操作符的原理，那么理解RxJava是怎么进行线程调度就比较简单了，来看

subscribeOn操作符的原理

```java
Observable.create(new ObservableOnSubscribe<String>() {
    @Override
    public void subscribe(ObservableEmitter<String> e) {
        e.onNext("Hello World");

        Log.d(TAG, "subscribe" + Thread.currentThread().getName());
    }
})
        .subscribeOn(Schedulers.io())
        .subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

                Disposable disposable = d;
                Log.d(TAG, "onSubscribe: " + Thread.currentThread().getName());
            }

            @Override
            public void onNext(String s) {
                Log.d(TAG, "onNext: " + Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
```

Schedulers.io()封装了线程池，看到subscribeOn

```java
public final Observable<T> subscribeOn(@NonNull Scheduler scheduler) {
    Objects.requireNonNull(scheduler, "scheduler is null");
    return RxJavaPlugins.onAssembly(new ObservableSubscribeOn<>(this, scheduler));
}
```

是不是和分析map操作符很类似？

接着看ObservableSubscribeOn

```java
public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
    super(source);
    this.scheduler = scheduler;
}
```

可以看到将那个包装了线程池的对象进行了储存

接下来看ObservableSubscribeOn的subscribeActual

```java
@Override
public void subscribeActual(final Observer<? super T> observer) {
    final SubscribeOnObserver<T> parent = new SubscribeOnObserver<>(observer);

    observer.onSubscribe(parent);

    parent.setDisposable(scheduler.scheduleDirect(new SubscribeTask(parent)));
}
```

看到new SubscribeTask(parent)

```java
final class SubscribeTask implements Runnable {
    private final SubscribeOnObserver<T> parent;

    SubscribeTask(SubscribeOnObserver<T> parent) {
        this.parent = parent;
    }

    @Override
    public void run() {
        source.subscribe(parent);
    }
}
```

将观察者包装进了一个Runnable对象里

scheduler.scheduleDirect(new SubscribeTask(parent))将这个任务放进了线程池中执行。

看看scheduler.scheduleDirect

```java
@NonNull
public Disposable scheduleDirect(@NonNull Runnable run) {
    return scheduleDirect(run, 0L, TimeUnit.NANOSECONDS);
}

@NonNull
    public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
        final Worker w = createWorker();

        final Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

        DisposeTask task = new DisposeTask(decoratedRun, w);

        w.schedule(task, delay, unit);

        return task;
    }

@NonNull
        @Override
        public Disposable schedule(@NonNull Runnable action, long delayTime, @NonNull TimeUnit unit) {
            if (tasks.isDisposed()) {
                // don't schedule, we are unsubscribed
                return EmptyDisposable.INSTANCE;
            }

            return threadWorker.scheduleActual(action, delayTime, unit, tasks);
        }

@NonNull
    public ScheduledRunnable scheduleActual(final Runnable run, long delayTime, @NonNull TimeUnit unit, @Nullable DisposableContainer parent) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);

        ScheduledRunnable sr = new ScheduledRunnable(decoratedRun, parent);

        if (parent != null) {
            if (!parent.add(sr)) {
                return sr;
            }
        }

        Future<?> f;
        try {
            if (delayTime <= 0) {
                f = executor.submit((Callable<Object>)sr);
            } else {
                f = executor.schedule((Callable<Object>)sr, delayTime, unit);
            }
            sr.setFuture(f);
        } catch (RejectedExecutionException ex) {
            if (parent != null) {
                parent.remove(sr);
            }
            RxJavaPlugins.onError(ex);
        }

        return sr;
    }
```

接着看ObserverOn的源码

为了方便讲解将RxJava运行在了子线程中

```java
new Thread() {
    @Override
    public void run() {
        super.run();

        ThreadDemo();
    }
}.start();

    void ThreadDemo() {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) {
                e.onNext("qwerty");

                Log.d(TAG, "subscribe " + Thread.currentThread().getName());
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: " + Thread.currentThread().getName());
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext: " + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
```

AndroidSchedulers.mainThread()和Schedulers.io()类似，Schedulers.io()里面封装了线程池以让任务可以执行在子线程中，而AndroidSchedulers.mainThread()封装了handler以便任务可以执行在主线程中

分析observeOn

```java
public final Observable<T> observeOn(@NonNull Scheduler scheduler) {
    return observeOn(scheduler, false, bufferSize());
}

public final Observable<T> observeOn(@NonNull Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableObserveOn<>(this, scheduler, delayError, bufferSize));
    }

public ObservableObserveOn(ObservableSource<T> source, Scheduler scheduler, boolean delayError, int bufferSize) {
        super(source);
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.bufferSize = bufferSize;
    }
```

可知将scheduler进行了存储

有了map操作符和SubscribeOn操作符的基础，分析ObserverOn原理其实是一样的。



 subscribeOn()切换线程时序图：

![](https://github.com/OkAndGreat/RxJavaTeachDemo/blob/master/assets/subscribeOn()%E5%88%87%E6%8D%A2%E7%BA%BF%E7%A8%8B%E6%97%B6%E5%BA%8F%E5%9B%BE.png?raw=true)

对observeOn()与subscribeOn()的使用做一个总结：

1.只有第一个subscribeOn() 起作用（所以多个 subscribeOn() 无意义）；
2.这个 subscribeOn() 控制从流程开始的第一个操作，直到遇到第一个 observeOn()；
3.observeOn() 可以使用多次，每个 observeOn() 将导致一次线程切换()，这次切换开始于这次 observeOn() 的下一个操作；
4.不论是 subscribeOn() 还是 observeOn()，每次线程切换如果不受到下一个 observeOn() 的干预，线程将不再改变，不会自动切换到其他线程。

## RxJava核心思想

用RxJava写出来的代码像一条有始有终的河流，我们在起点放入事件，然后事件从起点流向终点的过程中我们可以不断对事件进行拦截并更改，最终到终点时是我们想要的事件。



什么意思？

比如说我们要下载一张图片并展示，起点事件就是图片String类型的URL，然后我们可以拦截这个事件，然后将URL转变为Bitmap再将事件流向下游，这样事件就发生了更改，我们也可以再次拦截事件然后打一个log然后再次将事件传向下游，这样到终点时事件就是bitmap信息而不是String了，我们此时可以拿得到的bitmap信息做想做的事情，比如赋值给ImageView

```java
public void rxJavaDownloadImageAction(View view) {

        // 起点
        Observable.just(PATH)
                .map(new Function<String, Bitmap>() {
                    @Override
                    public Bitmap apply(String s) throws Exception {
                        URL url = new URL(PATH);
                        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                        httpURLConnection.setConnectTimeout(5000);
                        int responseCode = httpURLConnection.getResponseCode(); // 才开始 request
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStream inputStream = httpURLConnection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            return bitmap;
                        }
                        return null;
                    }
                })
                // 日志记录
                .map(new Function<Bitmap, Bitmap>() {
                    @Override
                    public Bitmap apply(Bitmap bitmap) {
                        Log.d(TAG, "apply: 是这个时候下载了图片啊:" + System.currentTimeMillis());
                        return bitmap;
                    }
                })
                .map(new Function<Bitmap, Bitmap>() {
                    @Override
                    public Bitmap apply(Bitmap bitmap) {
                        Paint paint = new Paint();
                        paint.setTextSize(88);
                        paint.setColor(Color.RED);
                        return drawTextToBitmap(bitmap, "同学们大家好", paint, 88, 88);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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

## 自定义RxJava操作符

仿照其它操作符并借助throttleFirst操作符完成View的防抖操作符

```java
public static <T> Observable<T> create(@NonNull ObservableOnSubscribe<T> source) {
    Objects.requireNonNull(source, "source is null");
    return RxJavaPlugins.onAssembly(new ObservableCreate<>(source));
}

-->
    public class RxView {

    public static Observable<Object> clicks(View view) {
        Objects.requireNonNull(view, "source is null");
        return RxJavaPlugins.onAssembly(new RxViewObservable(view));
    }
}





public final class ObservableCreate<T> extends Observable<T> {
    final ObservableOnSubscribe<T> source;

    public ObservableCreate(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

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
    
    ...
}

-->

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
```

## RxHook

前面的东西都不太好理解，最后讲个好玩的

还记得之前讲的RxJavaPlugins.onAssembly这个钩子方法吗？

上面我们只是简单说了下这个东西，我们来仔细看看这个东西能用了干嘛

```java
public static <T> Observable<T> onAssembly(@NonNull Observable<T> source) {
    Function<? super Observable, ? extends Observable> f = onObservableAssembly;
    if (f != null) {
        return apply(f, source);
    }
    return source;
}
```

之前我们说我们没有设置Function<? super Observable, ? extends Observable> f的话f为null所以这个方法就会什么也没干，传入什么就返回了什么，现在我们来看看怎么设置

一番探寻后可知是

RxJavaPlugins.setOnObservableAssembly

这个方法

```java
public static void setOnObservableAssembly(@Nullable Function<? super Observable, ? extends Observable> onObservableAssembly) {
    if (lockdown) {
        throw new IllegalStateException("Plugins can't be changed anymore");
    }
    RxJavaPlugins.onObservableAssembly = onObservableAssembly;
}
```

假如f不会null的话就会走return apply(f, source);而不是return source; 来看看apply(f, source)

```java
static <T, R> R apply(@NonNull Function<T, R> f, @NonNull T t) {
    try {
        return f.apply(t);
    } catch (Throwable ex) {
        throw ExceptionHelper.wrapOrThrow(ex);
    }
}
```

依据这个可以编写以下代码全局监听RxJava

```
RxJavaPlugins.setOnObservableAssembly(new Function<Observable, Observable>() {
    @Override
    public Observable apply(Observable observable) {
        Log.d(TAG, "apply: 整个项目 全局 监听 到底有多少地方使用 RxJava:" + observable);
        return observable; // 不破坏人家的功能
    }
});
```



关于RxJava我要讲的就是这些了，由于水平限制可能我讲的并不是很清楚，不是很懂的同学可以后面再看看讲义或者跟着demo自己去跟一遍源码。

github仓库地址

[OkAndGreat/RxJavaTeachDemo (github.com)](https://github.com/OkAndGreat/RxJavaTeachDemo)
