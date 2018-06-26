package com.luoqiaoyou.webp.webpglide.webp;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.facebook.animated.webp.WebPFrame;
import com.facebook.animated.webp.WebPImage;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by luoqiaoyou on 17/7/19.
 */

public class WebpDecoder implements GifDecoder {

    private WebPImage mWebPImage;
    private GifDecoder.BitmapProvider mProvider;
    private int mFramePointer;
    private int[] mFrameDurations;
    private int downsampledWidth;
    private int downsampledHeight;


    public WebpDecoder(GifDecoder.BitmapProvider provider, WebPImage webPImage, int sampleSize) {
        mProvider = provider;
        mWebPImage = webPImage;
        mFrameDurations = webPImage.getFrameDurations();
        downsampledWidth = webPImage.getWidth() / sampleSize;
        downsampledHeight = webPImage.getHeight() / sampleSize;
    }

    @Override
    public int getWidth() {
        return mWebPImage.getWidth();
    }

    @Override
    public int getHeight() {
        return mWebPImage.getHeight();
    }

    @Override
    public ByteBuffer getData() {
        return null;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void advance() {
        mFramePointer = (mFramePointer + 1) % mWebPImage.getFrameCount();
    }

    @Override
    public int getDelay(int n) {
        int delay = -1;
        if ((n >= 0) && (n < mFrameDurations.length)) {
            delay = mFrameDurations[n];
        }
        return delay;
    }

    @Override
    public int getNextDelay() {
        if (mFrameDurations.length == 0 || mFramePointer < 0) {
            return 0;
        }

        return getDelay(mFramePointer);
    }

    @Override
    public int getFrameCount() {
        return mWebPImage.getFrameCount();
    }

    @Override
    public int getCurrentFrameIndex() {
        return mFramePointer;
    }

    @Override
    public void resetFrameIndex() {
        mFramePointer = -1;
    }

    @Override
    public int getLoopCount() {
        return mWebPImage.getLoopCount();
    }

    @Override
    public int getNetscapeLoopCount() {
        return mWebPImage.getLoopCount();
    }

    @Override
    public int getTotalIterationCount() {
        if (mWebPImage.getLoopCount() == 0) {
            return TOTAL_ITERATION_COUNT_FOREVER;
        }
        return mWebPImage.getFrameCount() + 1;
    }

    @Override
    public int getByteSize() {
        return mWebPImage.getSizeInBytes();
    }

    @Override
    public Bitmap getNextFrame() {
        Bitmap result = mProvider.obtain(downsampledWidth, downsampledHeight, Bitmap.Config.ARGB_8888);
        WebPFrame frame = mWebPImage.getFrame(getCurrentFrameIndex());
        frame.renderFrame(downsampledWidth, downsampledHeight, result);
        return result;
    }

    @Override
    public int read(InputStream inputStream, int i) {
        return 0;
    }

    @Override
    public void clear() {
        mWebPImage.dispose();
        mWebPImage = null;
    }

    @Override
    public void setData(GifHeader gifHeader, byte[] bytes) {

    }

    @Override
    public void setData(GifHeader gifHeader, ByteBuffer byteBuffer) {

    }

    @Override
    public void setData(GifHeader gifHeader, ByteBuffer byteBuffer, int i) {

    }

    @Override
    public int read(byte[] bytes) {
        return 0;
    }

    @Override
    public void setDefaultBitmapConfig(@NonNull Bitmap.Config format) {
    }
}
