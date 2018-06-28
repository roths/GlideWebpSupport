package com.luoqiaoyou.webp.webpglide.imagelist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.luoqiaoyou.webp.webpglide.R;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created on 2018/6/27.
 *
 * @author ice
 */
public class ImageListActivity extends FragmentActivity {
    private static final String KEY_SUPPORT_WEBP = "support_webp";

    public static void startThis(Context context, boolean isSupportWebp) {
        Intent intent = new Intent(context, ImageListActivity.class);
        intent.putExtra(KEY_SUPPORT_WEBP, isSupportWebp);
        context.startActivity(intent);
    }

    private RecyclerView mRecyclerView;
    private MyAdapter mMyAdapter;
    private static boolean mIsSupportWebp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);
        mIsSupportWebp = getIntent().getBooleanExtra(KEY_SUPPORT_WEBP, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(ImageListActivity.this).clearDiskCache();
            }
        }).start();

        initView(savedInstanceState);
        getData();
    }

    protected void initView(@Nullable Bundle savedInstanceState) {
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mMyAdapter = new MyAdapter(this);
        mRecyclerView.setAdapter(mMyAdapter);
    }

    private void getData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = getHttpResponse("http://gank.io/api/random/data/福利/30");
                    final GankMeizhi gankMeizhi = JSON.parseObject(result, GankMeizhi.class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMyAdapter.refreshData(gankMeizhi);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    String getHttpResponse(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = new OkHttpClient().newCall(request).execute();
        if (response == null || response.body() == null) {
            return null;
        }
        return response.body().string();
    }

    private static class MyAdapter extends RecyclerView.Adapter {
        private Context mContext;
        private GankMeizhi mGankMeizhi;

        public MyAdapter(Context context) {
            mContext = context;
        }

        public void refreshData(GankMeizhi gankMeizhi) {
            mGankMeizhi = gankMeizhi;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_meizhi, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (mGankMeizhi == null) {
                return;
            }
            GankMeizhi.ResultsBean resultsBean = mGankMeizhi.results.get(position);
            MyViewHolder myViewHolder = (MyViewHolder) holder;
            myViewHolder.mTextView.setText(resultsBean.desc);
            String url = resultsBean.url;
            if (position % 3 == 0)
                url = "http://a.img.diaoyu-3.com/GGv8401-webp";

            if (mIsSupportWebp) {
                Glide.with(mContext)
                        .load(url)
                        .into(myViewHolder.mImageView);
            } else {
                Glide.with(mContext)
                        .load(url)
                        .into(myViewHolder.mImageView);
            }
        }

        @Override
        public int getItemCount() {
            if (mGankMeizhi == null)
                return 0;
            else
                return mGankMeizhi.results.size();
        }
    }


    private static class MyViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImageView;
        public TextView mTextView;

        public MyViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.meizhi_iv);
            mTextView = itemView.findViewById(R.id.desc_tv);
        }
    }

}
