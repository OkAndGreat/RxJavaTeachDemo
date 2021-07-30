package com.example.rxjavateachdemo.DownloadDemo;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rxjavateachdemo.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DownloadActivity extends AppCompatActivity {
    private static final String TAG = "DownloadActivity";

    // 网络图片的链接地址
    private final static String PATH = "https://s1.ax1x.com/2020/10/11/0gGYbF.jpg";

    // 弹出加载框
    private ProgressDialog progressDialog;

    // ImageView控件，用来显示结果图像
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        image = findViewById(R.id.image);
    }


    /**
     * 传统方法从网络中下载图片并展示
     *
     * @param view
     */
    public void downloadImageAction(View view) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("下载图片中...");
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(PATH);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout(5000);
                    int responseCode = httpURLConnection.getResponseCode(); // 才开始 request
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = httpURLConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Message message = handler.obtainMessage();
                        message.obj = bitmap;
                        handler.sendMessage(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private final Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            Bitmap bitmap = (Bitmap) msg.obj;
            image.setImageBitmap(bitmap);

            if (progressDialog != null) progressDialog.dismiss();
            return false;
        }
    });

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

    // 图片上绘制文字 加水印
    private final Bitmap drawTextToBitmap(Bitmap bitmap, String text, Paint paint, int paddingLeft, int paddingTop) {
        Bitmap.Config bitmapConfig = bitmap.getConfig();

        paint.setDither(true); // 获取跟清晰的图像采样
        paint.setFilterBitmap(true);// 过滤一些
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawText(text, paddingLeft, paddingTop, paint);
        return bitmap;
    }
}