package com.luoqiaoyou.webp.webpglide;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.luoqiaoyou.webp.webpglide.imagelist.ImageListActivity;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageView = (ImageView) findViewById(R.id.webp);
        ImageView imageView2 = (ImageView) findViewById(R.id.webp2);
        ImageView imageView3 = (ImageView) findViewById(R.id.webp3);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.get(MainActivity.this).clearDiskCache();
                    }
                }).start();
            }
        });
        RequestOptions options =
                new RequestOptions()
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
        Glide.with(this)
                .load("file:///android_asset/small.webp")
                .apply(options).transition(new DrawableTransitionOptions().crossFade(200))
                .into(imageView);
        Glide.with(this)
                .load("file:///android_asset/sticker1.webp")
                .apply(options).transition(new DrawableTransitionOptions().crossFade(200))
                .into(imageView2);

        Glide.with(this)
                .load("http://a.img.diaoyu-3.com/GGv8401-webp")
                .apply(options).transition(new DrawableTransitionOptions().crossFade(200))
                .into(imageView3);
    }

    public void toListWebP(View view) {
        ImageListActivity.startThis(this, true);
    }

    public void toList(View view) {
        ImageListActivity.startThis(this, false);
    }

}
