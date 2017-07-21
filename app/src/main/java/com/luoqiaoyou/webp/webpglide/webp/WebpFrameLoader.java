package com.luoqiaoyou.webp.webpglide.webp;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by luoqiaoyou on 17/7/19.
 */

public class WebpFrameLoader {

    private final GifDecoder gifDecoder;
    private final Handler handler;
    private final List<FrameCallback> callbacks;
    final RequestManager requestManager;
    private final BitmapPool bitmapPool;
    private boolean isRunning;
    private boolean isLoadPending;
    private boolean startFromFirstFrame;
    private RequestBuilder<Bitmap> requestBuilder;
    private DelayTarget current;
    private boolean isCleared;
    private DelayTarget next;
    private Bitmap firstFrame;
    private Transformation<Bitmap> transformation;

    public WebpFrameLoader(Glide glide, GifDecoder gifDecoder, int width, int height, Transformation<Bitmap> transformation, Bitmap firstFrame) {
        this(glide.getBitmapPool(), Glide.with(glide.getContext()), gifDecoder, (Handler)null, getRequestBuilder(Glide.with(glide.getContext()), width, height), transformation, firstFrame);
    }

    WebpFrameLoader(BitmapPool bitmapPool, RequestManager requestManager, GifDecoder gifDecoder, Handler handler, RequestBuilder<Bitmap> requestBuilder, Transformation<Bitmap> transformation, Bitmap firstFrame) {
        this.callbacks = new ArrayList();
        this.isRunning = false;
        this.isLoadPending = false;
        this.startFromFirstFrame = false;
        this.requestManager = requestManager;
        if(handler == null) {
            handler = new Handler(Looper.getMainLooper(), new FrameLoaderCallback());
        }

        this.bitmapPool = bitmapPool;
        this.handler = handler;
        this.requestBuilder = requestBuilder;
        this.gifDecoder = gifDecoder;
        this.setFrameTransformation(transformation, firstFrame);
    }

    void setFrameTransformation(Transformation<Bitmap> transformation, Bitmap firstFrame) {
        this.transformation = (Transformation) Preconditions.checkNotNull(transformation);
        this.firstFrame = (Bitmap)Preconditions.checkNotNull(firstFrame);
        this.requestBuilder = this.requestBuilder.apply((new RequestOptions()).transform(transformation));
    }

    Transformation<Bitmap> getFrameTransformation() {
        return this.transformation;
    }

    Bitmap getFirstFrame() {
        return this.firstFrame;
    }

    void subscribe(FrameCallback frameCallback) {
        if(this.isCleared) {
            throw new IllegalStateException("Cannot subscribe to a cleared frame loader");
        } else {
            boolean start = this.callbacks.isEmpty();
            if(this.callbacks.contains(frameCallback)) {
                throw new IllegalStateException("Cannot subscribe twice in a row");
            } else {
                this.callbacks.add(frameCallback);
                if(start) {
                    this.start();
                }

            }
        }
    }

    void unsubscribe(FrameCallback frameCallback) {
        this.callbacks.remove(frameCallback);
        if(this.callbacks.isEmpty()) {
            this.stop();
        }

    }

    int getWidth() {
        return this.getCurrentFrame().getWidth();
    }

    int getHeight() {
        return this.getCurrentFrame().getHeight();
    }

    int getSize() {
        return this.gifDecoder.getByteSize() + this.getFrameSize();
    }

    int getCurrentIndex() {
        return this.current != null?this.current.index:-1;
    }

    private int getFrameSize() {
        return Util.getBitmapByteSize(this.getCurrentFrame().getWidth(), this.getCurrentFrame().getHeight(), this.getCurrentFrame().getConfig());
    }

    ByteBuffer getBuffer() {
        return this.gifDecoder.getData().asReadOnlyBuffer();
    }

    int getFrameCount() {
        return this.gifDecoder.getFrameCount();
    }

    int getLoopCount() {
        return this.gifDecoder.getTotalIterationCount();
    }

    private void start() {
        if(!this.isRunning) {
            this.isRunning = true;
            this.isCleared = false;
            this.loadNextFrame();
        }
    }

    private void stop() {
        this.isRunning = false;
    }

    void clear() {
        this.callbacks.clear();
        this.recycleFirstFrame();
        this.stop();
        if(this.current != null) {
            this.requestManager.clear(this.current);
            this.current = null;
        }

        if(this.next != null) {
            this.requestManager.clear(this.next);
            this.next = null;
        }

        this.gifDecoder.clear();
        this.isCleared = true;
    }

    Bitmap getCurrentFrame() {
        return this.current != null?this.current.getResource():this.firstFrame;
    }

    private void loadNextFrame() {
        if(this.isRunning && !this.isLoadPending) {
            if(this.startFromFirstFrame) {
                this.gifDecoder.resetFrameIndex();
                this.startFromFirstFrame = false;
            }

            this.isLoadPending = true;
            int delay = this.gifDecoder.getNextDelay();
            long targetTime = SystemClock.uptimeMillis() + (long)delay;
            this.gifDecoder.advance();
            this.next = new DelayTarget(this.handler, this.gifDecoder.getCurrentFrameIndex(), targetTime);
            this.requestBuilder.clone().apply(RequestOptions.signatureOf(new FrameSignature())).load(this.gifDecoder).into(this.next);
        }
    }

    private void recycleFirstFrame() {
        if(this.firstFrame != null) {
            this.bitmapPool.put(this.firstFrame);
            this.firstFrame = null;
        }

    }

    void setNextStartFromFirstFrame() {
        Preconditions.checkArgument(!this.isRunning, "Can\'t restart a running animation");
        this.startFromFirstFrame = true;
    }

    void onFrameReady(DelayTarget delayTarget) {
        if(this.isCleared) {
            this.handler.obtainMessage(2, delayTarget).sendToTarget();
        } else {
            if(delayTarget.getResource() != null) {
                this.recycleFirstFrame();
                DelayTarget previous = this.current;
                this.current = delayTarget;

                for(int i = this.callbacks.size() - 1; i >= 0; --i) {
                    FrameCallback cb = (FrameCallback)this.callbacks.get(i);
                    cb.onFrameReady();
                }

                if(previous != null) {
                    this.handler.obtainMessage(2, previous).sendToTarget();
                }
            }

            this.isLoadPending = false;
            this.loadNextFrame();
        }
    }

    private static RequestBuilder<Bitmap> getRequestBuilder(RequestManager requestManager, int width, int height) {
        return requestManager.asBitmap().apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE).skipMemoryCache(true).override(width, height));
    }

    static class FrameSignature implements Key {
        private final UUID uuid;

        public FrameSignature() {
            this(UUID.randomUUID());
        }

        FrameSignature(UUID uuid) {
            this.uuid = uuid;
        }

        public boolean equals(Object o) {
            if(o instanceof FrameSignature) {
                FrameSignature other = (FrameSignature)o;
                return other.uuid.equals(this.uuid);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return this.uuid.hashCode();
        }

        public void updateDiskCacheKey(MessageDigest messageDigest) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    static class DelayTarget extends SimpleTarget<Bitmap> {
        private final Handler handler;
        final int index;
        private final long targetTime;
        private Bitmap resource;

        DelayTarget(Handler handler, int index, long targetTime) {
            this.handler = handler;
            this.index = index;
            this.targetTime = targetTime;
        }

        Bitmap getResource() {
            return this.resource;
        }

        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
            this.resource = resource;
            Message msg = this.handler.obtainMessage(1, this);
            this.handler.sendMessageAtTime(msg, this.targetTime);
        }
    }

    private class FrameLoaderCallback implements Handler.Callback {
        public static final int MSG_DELAY = 1;
        public static final int MSG_CLEAR = 2;

        FrameLoaderCallback() {
        }

        public boolean handleMessage(Message msg) {
            DelayTarget target;
            if(msg.what == 1) {
                target = (DelayTarget)msg.obj;
                WebpFrameLoader.this.onFrameReady(target);
                return true;
            } else {
                if(msg.what == 2) {
                    target = (DelayTarget)msg.obj;
                    WebpFrameLoader.this.requestManager.clear(target);
                }

                return false;
            }
        }
    }

    public interface FrameCallback {
        void onFrameReady();
    }
}
