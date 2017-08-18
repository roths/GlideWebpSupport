# GlideWebpSupport
Glide4.0 Support Webp , use fresco-webp

使用方法：在application.onCreate里面添加下面两行，注册webp的解码器

// webp支持
ResourceDecoder decoder = new WebpResourceDecoder(this);
Glide.get(this).getRegistry().append(InputStream.class, Drawable.class, decoder);

项目用的是Glide jar包集成，也可以用maven集成只需要

    compile files('libs/glide-4.0.0.jar')
    compile files('libs/glide-okhttp3-integration-4.0.0-SNAPSHOT.jar')

//    compile 'com.github.bumptech.glide:glide:4.0.0-RC1'
//    compile 'com.github.bumptech.glide:okhttp3-integration:4.0.0-RC1'
    compile "com.facebook.fresco:animated-webp:0.11.0"

把gradle里面上面两行注释，用下面两行就可以了
