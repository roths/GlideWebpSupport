package com.luoqiaoyou.webp.webpglide.webp;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.bumptech.glide.load.resource.gif.GifOptions;
import com.facebook.animated.webp.WebPImage;
import com.facebook.soloader.SoLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by luoqiaoyou on 17/7/16.
 */

public class WebpResourceDecoder implements ResourceDecoder<InputStream, WebpDrawable> {

    public final String TAG = "WebpResourceDecoder";
    private final List<ImageHeaderParser> mParsers;
    private final ArrayPool mByteArrayPool;
    private final Context mContext;
    private final BitmapPool mBitmapPool;
    private final GifBitmapProvider mProvider;



    public WebpResourceDecoder(Context context) {
        this(context, Glide.get(context).getRegistry().getImageHeaderParsers(), Glide.get(context).getArrayPool(),
                Glide.get(context).getBitmapPool());
        // if not init Soloader, will get error when decode
        try {
            SoLoader.init(context, 0);
        } catch (IOException e) {
            Log.v(TAG, "Failed to init SoLoader", e);
        }
    }

    public WebpResourceDecoder(Context context, List<ImageHeaderParser> parsers, ArrayPool byteArrayPool, BitmapPool bitmapPool) {
        mContext = context.getApplicationContext();
        mParsers = parsers;
        mByteArrayPool = byteArrayPool;
        mBitmapPool = bitmapPool;
        mProvider = new GifBitmapProvider(bitmapPool, byteArrayPool);

    }

    @Override
    public boolean handles(InputStream inputStream, Options options) throws IOException {
        ImageHeaderParser.ImageType type = ImageHeaderParserUtils.getType(mParsers, inputStream, mByteArrayPool);
        return type == ImageHeaderParser.ImageType.WEBP || type == ImageHeaderParser.ImageType.WEBP_A;
    }

    @Nullable
    @Override
    public Resource<WebpDrawable> decode(InputStream inputStream, int width, int height, Options options) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100]; //buff用于存放循环读取的临时数据
        int rc = 0;
        while ((rc = inputStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in_b = swapStream.toByteArray();
        WebPImage webp = WebPImage.create(in_b);

        int sampleSize = getSampleSize(webp.getWidth(), webp.getHeight(), width, height);
        WebpDecoder webpDecoder = new WebpDecoder(mProvider, webp, sampleSize);
        Bitmap firstFrame = webpDecoder.getNextFrame();
        if (firstFrame == null) {
            return null;
        }

        Transformation<Bitmap> unitTransformation = UnitTransformation.get();

        return new WebpDrawableResource(new WebpDrawable(mContext, webpDecoder, mBitmapPool, unitTransformation, width, height,
                firstFrame));
    }

    private static int getSampleSize(int srcWidth, int srcHeight, int targetWidth, int targetHeight) {
        int exactSampleSize = Math.min(srcHeight / targetHeight,
                srcWidth / targetWidth);
        int powerOfTwoSampleSize = exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);
        // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
        // than 0.
        int sampleSize = Math.max(1, powerOfTwoSampleSize);
        return sampleSize;
    }


    public class WebpDrawableResource extends DrawableResource<WebpDrawable> implements Initializable {
        public WebpDrawableResource(WebpDrawable drawable) {
            super(drawable);
        }

        public Class<WebpDrawable> getResourceClass() {
            return WebpDrawable.class;
        }

        public int getSize() {
            return drawable.getSize();
        }

        public void recycle() {

        }

        public void initialize() {

        }
    }

}
