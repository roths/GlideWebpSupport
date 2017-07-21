package com.luoqiaoyou.webp.webpglide;

import android.app.Application;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.luoqiaoyou.webp.webpglide.webp.WebpResourceDecoder;

import java.io.InputStream;

/**
 * Created by luoqiaoyou on 17/7/21.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // webp支持
        ResourceDecoder decoder = new WebpResourceDecoder(this);
        Glide.get(this).getRegistry().append(InputStream.class, Drawable.class, decoder);
    }
}
