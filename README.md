# GlideWebpSupport
Glide 4.7.1 Support Webp , use fresco-webp 1.9.0

add this code into Application.onCreate

添加下面的代码就能支持webp静态/动态图
```
// webp support
ResourceDecoder decoder = new WebpResourceDecoder(this);
Glide.get(this).getRegistry().append(InputStream.class, Drawable.class, decoder);
```

加载网络图片需要添加oktthp网络集成库的依赖:
```
// enable load remote webp
compile "com.github.bumptech.glide:okhttp3-integration:4.6.1"
```
