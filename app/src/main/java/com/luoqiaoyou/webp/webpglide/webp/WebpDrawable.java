package com.luoqiaoyou.webp.webpglide.webp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.VisibleForTesting;
import android.view.Gravity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Preconditions;

import java.nio.ByteBuffer;

/**
 * Created by luoqiaoyou on 17/7/16.
 */
public class WebpDrawable extends Drawable implements WebpFrameLoader.FrameCallback, Animatable {
    public static final int LOOP_FOREVER = -1;
    public static final int LOOP_INTRINSIC = 0;
    private final GifState state;
    private boolean isRunning;
    private boolean isStarted;
    private boolean isRecycled;
    private boolean isVisible;
    private int loopCount;
    private int maxLoopCount;
    private boolean applyGravity;
    private Paint paint;
    private Rect destRect;

    public WebpDrawable(Context context, GifDecoder gifDecoder, BitmapPool bitmapPool, Transformation<Bitmap> frameTransformation, int targetFrameWidth, int targetFrameHeight, Bitmap firstFrame) {
        this(new GifState(bitmapPool, new WebpFrameLoader(Glide.get(context), gifDecoder, targetFrameWidth, targetFrameHeight, frameTransformation, firstFrame)));
    }

    WebpDrawable(GifState state) {
        this.isVisible = true;
        this.maxLoopCount = -1;
        this.state = Preconditions.checkNotNull(state);
    }

    @VisibleForTesting
    WebpDrawable(WebpFrameLoader frameLoader, BitmapPool bitmapPool, Paint paint) {
        this(new GifState(bitmapPool, frameLoader));
        this.paint = paint;
    }

    public int getSize() {
        return this.state.frameLoader.getSize();
    }

    public Bitmap getFirstFrame() {
        return this.state.frameLoader.getFirstFrame();
    }

    public void setFrameTransformation(Transformation<Bitmap> frameTransformation, Bitmap firstFrame) {
        this.state.frameLoader.setFrameTransformation(frameTransformation, firstFrame);
    }

    public Transformation<Bitmap> getFrameTransformation() {
        return this.state.frameLoader.getFrameTransformation();
    }

    public ByteBuffer getBuffer() {
        return this.state.frameLoader.getBuffer();
    }

    public int getFrameCount() {
        return this.state.frameLoader.getFrameCount();
    }

    public int getFrameIndex() {
        return this.state.frameLoader.getCurrentIndex();
    }

    private void resetLoopCount() {
        this.loopCount = 0;
    }

    public void startFromFirstFrame() {
        Preconditions.checkArgument(!this.isRunning, "You cannot restart a currently running animation.");
        this.state.frameLoader.setNextStartFromFirstFrame();
        this.start();
    }

    public void start() {
        this.isStarted = true;
        this.resetLoopCount();
        if(this.isVisible) {
            this.startRunning();
        }

    }

    public void stop() {
        this.isStarted = false;
        this.stopRunning();
    }

    private void startRunning() {
        Preconditions.checkArgument(!this.isRecycled, "You cannot start a recycled Drawable. Ensure thatyou clear any references to the Drawable when clearing the corresponding request.");
        if(this.state.frameLoader.getFrameCount() == 1) {
            this.invalidateSelf();
        } else if(!this.isRunning) {
            this.isRunning = true;
            this.state.frameLoader.subscribe(this);
            this.invalidateSelf();
        }

    }

    private void stopRunning() {
        this.isRunning = false;
        this.state.frameLoader.unsubscribe(this);
    }

    public boolean setVisible(boolean visible, boolean restart) {
        Preconditions.checkArgument(!this.isRecycled, "Cannot change the visibility of a recycled resource. Ensure that you unset the Drawable from your View before changing the View\'s visibility.");
        this.isVisible = visible;
        if(!visible) {
            this.stopRunning();
        } else if(this.isStarted) {
            this.startRunning();
        }

        return super.setVisible(visible, restart);
    }

    public int getIntrinsicWidth() {
        return this.state.frameLoader.getWidth();
    }

    public int getIntrinsicHeight() {
        return this.state.frameLoader.getHeight();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.applyGravity = true;
    }

    public void draw(Canvas canvas) {
        if(!this.isRecycled) {
            if(this.applyGravity) {
                Gravity.apply(GifState.GRAVITY, this.getIntrinsicWidth(), this.getIntrinsicHeight(), this.getBounds(), this.getDestRect());
                this.applyGravity = false;
            }

            Bitmap currentFrame = this.state.frameLoader.getCurrentFrame();
            canvas.drawBitmap(currentFrame, (Rect)null, this.getDestRect(), this.getPaint());
        }
    }

    public void setAlpha(int i) {
        this.getPaint().setAlpha(i);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.getPaint().setColorFilter(colorFilter);
    }

    private Rect getDestRect() {
        if(this.destRect == null) {
            this.destRect = new Rect();
        }

        return this.destRect;
    }

    private Paint getPaint() {
        if(this.paint == null) {
            this.paint = new Paint(2);
        }

        return this.paint;
    }

    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    public void onFrameReady() {
        if(this.getCallback() == null) {
            this.stop();
            this.invalidateSelf();
        } else {
            this.invalidateSelf();
            if(this.getFrameIndex() == this.getFrameCount() - 1) {
                ++this.loopCount;
            }

            if(this.maxLoopCount != LOOP_FOREVER && this.loopCount >= this.maxLoopCount) {
                this.stop();
            }

        }
    }

    public ConstantState getConstantState() {
        return this.state;
    }

    public void recycle() {
        this.isRecycled = true;
        this.state.frameLoader.clear();
    }

    boolean isRecycled() {
        return this.isRecycled;
    }

    public void setLoopCount(int loopCount) {
        if(loopCount <= 0 && loopCount != LOOP_FOREVER && loopCount != LOOP_INTRINSIC) {
            throw new IllegalArgumentException("Loop count must be greater than 0, or equal to LOOP_FOREVER, or equal to LOOP_INTRINSIC");
        } else {
            if(loopCount == LOOP_INTRINSIC) {
                int intrinsicCount = this.state.frameLoader.getLoopCount();
                this.maxLoopCount = intrinsicCount == LOOP_INTRINSIC ? LOOP_FOREVER : intrinsicCount;
            } else {
                this.maxLoopCount = loopCount;
            }

        }
    }

    static class GifState extends ConstantState {
        static final int GRAVITY = Gravity.FILL;
        final BitmapPool bitmapPool;
        final WebpFrameLoader frameLoader;

        public GifState(BitmapPool bitmapPool, WebpFrameLoader frameLoader) {
            this.bitmapPool = bitmapPool;
            this.frameLoader = frameLoader;
        }

        public Drawable newDrawable(Resources res) {
            return this.newDrawable();
        }

        public Drawable newDrawable() {
            return new WebpDrawable(this);
        }

        public int getChangingConfigurations() {
            return 0;
        }
    }
}
