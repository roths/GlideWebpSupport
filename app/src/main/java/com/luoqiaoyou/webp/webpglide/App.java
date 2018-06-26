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

        // webp support
        ResourceDecoder decoder = new WebpResourceDecoder(this);
        // use prepend() avoid intercept by default decoder
        Glide.get(this).getRegistry()
                .prepend(InputStream.class, Drawable.class, decoder);
    }
}
