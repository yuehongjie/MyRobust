开源地址：[https://github.com/Meituan-Dianping/Robust](https://github.com/Meituan-Dianping/Robust)

本文环境使用的是 robust:0.3.3 版本

## 一、准备工作

**1. 添加依赖**

在 app 下 build.gradle 文件中添加依赖


```
apply plugin: 'com.android.application'
//制作补丁时才将这个打开，auto-patch-plugin 紧跟着 com.android.application
apply plugin: 'auto-patch-plugin'
apply plugin: 'robust'

// ...,...


dependencies {
    
    compile 'com.meituan.robust:robust:0.3.3'
    
}

```

在项目主目录下的 build.gradle 文件中添加 classPath 依赖

```
// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.1'

        // robust
        classpath 'com.meituan.robust:gradle-plugin:0.3.3'
        classpath 'com.meituan.robust:auto-patch-plugin:0.3.3'
    }
}

//...

```

**2. 在 app 目录下添加 robust.xm 文件，即与 src 同级**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <switch>
        <!--true代表打开Robust，请注意即使这个值为true，Robust也默认只在Release模式下开启-->
        <!--false代表关闭Robust，无论是Debug还是Release模式都不会运行robust-->
        <turnOnRobust>true</turnOnRobust>
        <!--<turnOnRobust>false</turnOnRobust>-->

        <!--是否开启手动模式，手动模式会去寻找配置项patchPackname包名下的所有类，自动的处理混淆，然后把patchPackname包名下的所有类制作成补丁-->
        <!--这个开关只是把配置项patchPackname包名下的所有类制作成补丁，适用于特殊情况，一般不会遇到-->
        <!--<manual>true</manual>-->
        <manual>false</manual>

        <!--是否强制插入插入代码，Robust默认在debug模式下是关闭的，开启这个选项为true会在debug下插入代码-->
        <!--但是当配置项turnOnRobust是false时，这个配置项不会生效-->

        <!--<forceInsert>true</forceInsert>-->
        <forceInsert>false</forceInsert>

        <!--是否捕获补丁中所有异常，建议上线的时候这个开关的值为true，测试的时候为false-->
        <catchReflectException>true</catchReflectException>
        <!--<catchReflectException>false</catchReflectException>-->

        <!--是否在补丁加上log，建议上线的时候这个开关的值为false，测试的时候为true-->
        <patchLog>true</patchLog>
        <!--<patchLog>false</patchLog>-->

        <!--项目是否支持progaurd-->
        <proguard>true</proguard>
        <!--<proguard>false</proguard>-->
    </switch>

    <!--需要热补的包名或者类名，这些包名下的所有类都被会插入代码-->
    <!--这个配置项是各个APP需要自行配置，就是你们App里面你们自己代码的包名，
    这些包名下的类会被Robust插入代码，没有被Robust插入代码的类Robust是无法修复的-->
    <packname name="hotfixPackage">
        <name>com.yu.myrobust</name>
    </packname>

    <!--不需要Robust插入代码的包名，Robust库不需要插入代码，如下的配置项请保留，还可以根据各个APP的情况执行添加-->
    <exceptPackname name="exceptPackage">
        <name>com.meituan.robust</name>
    </exceptPackname>

    <!--补丁的包名，请保持和类PatchManipulateImp中fetchPatchList方法中设置的补丁类名保持一致（ setPatchesInfoImplClassFullName("com.yu.myrobust.PatchesInfoImpl")），
    各个App可以独立定制，需要确保的是setPatchesInfoImplClassFullName设置的包名是如下的配置项，类名必须是：PatchesInfoImpl-->
    <patchPackname name="patchPackname">
        <name>com.yu.myrobust</name>
    </patchPackname>

    <!--自动化补丁中，不需要反射处理的类，这个配置项慎重选择-->
    <noNeedReflectClass name="classes no need to reflect">

    </noNeedReflectClass>
</resources>

```

**必须需要修改的地方有两个**（此时可以先不修改，等撸完热更新的代码后，再修改即可）：

![robust.xml](http://ong9pclk3.bkt.clouddn.com/robust1.jpg)

到这里准备工作是做完了，sync 项目应该就不会报错了。

## 二、撸代码工作

**1. 效果图：**

![效果图](http://ong9pclk3.bkt.clouddn.com/robust_patching2.gif)

操作很简单，就是加载补丁，修改跳转后的 Activity 显示的内容。

**2. 添加权限**，本例使用到了 读写 sd 卡的权限，6.0   以上的手机还需要动态申请

```xml
<uses-permission android:name="android.permission.Write_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

**3. 实现获取补丁的过程**

自定义一个类如 PatchManipulateImp 需要继承 PatchManipulate 并实现至少三个方法：

    List<Patch> fetchPatchList(Context context)
    
**加载补丁列表**，应该在这个方法中联网获取补丁，并设置补丁的路径（setLocalPath）以及 **robust.xml 中提到的 patchPackname** （方法 setPatchesInfoImplClassFullName）

    boolean ensurePatchExist(Patch patch)
  
 **确认补丁是否存在**，严格来说，应该验证 md5 ，不存在则动态下载  
    
    boolean verifyPatch(Context context, Patch patch)
    
**验证补丁有效性**（ MD5 是否一致 ），本例只是拷贝了补丁

完整 PatchManipulateImp.java 代码，注释也挺详细：

```java
package com.yu.myrobust;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.meituan.robust.Patch;
import com.meituan.robust.PatchManipulate;
import com.meituan.robust.RobustApkHashUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017-4-22.
 * 加载补丁的实现
 */

public class PatchManipulateImp extends PatchManipulate{

    private static final String TAG = "PatchManipulateImp";

    //获取补丁列表
    @Override
    protected List<Patch> fetchPatchList(Context context) {

        //将 app 自己的 robustApkHash 上报给服务端，服务端根据 robustApkHash 来区分每一次 apk build 来给 app 下发缺少的补丁
        String robustApkHash = RobustApkHashUtils.readRobustApkHash(context);
        Log.d(TAG , "fetchPatchList --> robustApkHash : " + robustApkHash);

        //应该在这里去联网获取补丁列表

        Patch patch = new Patch();
        patch.setName("patch_version_0.0.1");
        //会自动加上后缀 ... sdcard/robust/patch.jar
        patch.setLocalPath(Environment.getExternalStorageDirectory().getPath()+ File.separator+"robust"+File.separator + "patch");

        Log.d(TAG , "patch: " + patch.getName() + "   path: " + patch.getLocalPath());

        // setPatchesInfoImplClassFullName 设置项各个 App 可以独立定制，
        // 需要确保的是 setPatchesInfoImplClassFullName 设置的包名是和 robust.xml 配置项 patchPackname 保持一致，而且类名必须是：PatchesInfoImpl
        patch.setPatchesInfoImplClassFullName("com.yu.myrobust.PatchesInfoImpl");

        List<Patch>  patches = new ArrayList<>();
        patches.add(patch);
        return patches;
    }

    //验证补丁有效性（ MD5 是否一致 ），这里只是拷贝了补丁
    @Override
    protected boolean verifyPatch(Context context, Patch patch) {
        //放到app的私有目录 /data/data/com.yu.myrobust/cache/robust/patch_temp.jar 会自动加上 _temp.jar
        patch.setTempPath(context.getCacheDir()+ File.separator+"robust"+File.separator + "patch");

        Log.d(TAG , "verifyPatch: " + patch.getName() + "   path: " + patch.getTempPath());

        try {
            copy(patch.getLocalPath(), patch.getTempPath());
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("copy source patch to local patch error, no patch execute in path "+patch.getTempPath());
        }

        return true;
    }

    //确认补丁文件是否存在
    @Override
    protected boolean ensurePatchExist(Patch patch) {
        Log.d(TAG, "ensurePatchExist " + patch.getName() + "  md5: " + patch.getMd5());
        return true;
    }

    public void copy(String srcPath,String dstPath) throws IOException {
        File src=new File(srcPath);
        if(!src.exists()){
            Log.e(TAG, srcPath + " 不存在");
            throw new RuntimeException("source patch does not exist ");
        }
        File dst=new File(dstPath);
        if(!dst.getParentFile().exists()){
            dst.getParentFile().mkdirs();
        }
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}


```

**4. 实现加载补丁的入口**，这里是点击 “加载补丁” 按钮，来加载补丁，实际项目中，应该对用户不可见

```java
private void loadPatch() {

    /*
     * 开启加载补丁的线程
     * 参数：
     * Context 上下文环境
     * PatchManipulate 加载处理补丁的逻辑实现和验证
     * RobustCallBack 补丁加载过程中的回调
     */
    new PatchExecutor(this, new PatchManipulateImp(), new MyRobustCallBack()).start();
    
}

```

**5. 打包生成 release 版本**

打包前需要配置签名和混淆

修改 app 下的 build.gradle 配置签名(签名的生成这里就不提了)：

```

//签名
signingConfigs {
    key {
        storeFile file("key.jks")
        storePassword "001002"
        keyAlias "key_alias"
        keyPassword "001002"
    }
}

buildTypes {
    release {
        //minifyEnabled false  //如果不开启混淆 那么是不会生成 mapping.txt 的
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        signingConfig signingConfigs.key
    }
}

```
配置混淆规则


```
#robust
-keep class meituan.robust.patch.**{*;}

-keep class com.google.gson.**{*;}
-keep class com.meituan.robust.**{*;}
-dontwarn
-keepattributes Signature,SourceFile,LineNumberTable
-keepattributes *Annotation*
-keeppackagenames
-ignorewarnings
-dontwarn android.support.v4.**,**CompatHoneycomb,com.tenpay.android.**
-optimizations !class/unboxing/enum,!code/simplification/arithmetic

```


打包命令如：

    gradlew clean assembleRelease --stacktrace --no-daemon

mac 和 linux 需要 ./gradlew

把生成的 release 安装包安装到手机上，可以使用命令：
    
    adb install E:\work\Study\Mine\MyRobust\app\build\outputs\apk\app-release.apk 
    
**6. 备份 mapping.txt 和 methodsMap.robust**， 打包完成后会生成如下两个重要文件（应该根据 app 对应版本进行保存，因为以后进行热更新会用到）

![robust_release](http://ong9pclk3.bkt.clouddn.com/robust_release.jpg)


**7. 修改 Bug 代码**，这里其实很简单，就是修复在第二个 avtivity 上显示的内容

修改前的部分代码：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_second);

    TextView tvReceive = (TextView) findViewById(R.id.tv_receive);

    tvReceive.setText("我是错误的代码啊........");

}
```


修改后的部分代码，**被修改的方法要加 @Modify 注解，新增的方法要加 @Add 注解**：


```
 @Override
@Modify
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_second);

    TextView tvReceive = (TextView) findViewById(R.id.tv_receive);

    tvReceive.setText("我是错误的代码啊........");
    //tvReceive.setText(getInfo());
}

@Add
public String getInfo(){
    return "我是正确的代码..... 怒发冲冠，凭栏处，潇潇雨歇，抬望眼";
}

```

吐槽：为啥修改个 setText() 还需要增加个方法...

**8. 与 src 同级新增文件夹 robust，并把备份过的 mapping.txt 和 methodsMap.robust 拷贝进来**

![拷贝](http://ong9pclk3.bkt.clouddn.com/robust_copy.jpg)

**9. 打开补丁插件**，修改 app 下的 build.gradle 打开补丁开关

```
apply plugin: 'com.android.application'
//制作补丁时才将这个打开，auto-patch-plugin 紧跟着 com.android.application
apply plugin: 'auto-patch-plugin'
apply plugin: 'robust'
```

**10. 同样的打包命令生成补丁**

    gradlew clean assembleRelease --stacktrace --no-daemon

如果打包失败，而且错误原因是： **auto patch successfully** 那么恭喜你，补丁制作成功

![patch](http://ong9pclk3.bkt.clouddn.com/robust_gradlew_patch.jpg)

生成的补丁 patch.jar 在 output 下的 robust 文件夹中

![image](http://ong9pclk3.bkt.clouddn.com/robust_patchjar.jpg)

**11. 把补丁下载到手机中，位置为 PatchManipulate 中 setLocalPath() 方法 指定的路径和名称**：

    adb push E:\work\Study\Mine\MyRobust\app\build\outputs\robust\patch.jar /sdcard/robust/patch.jar

![image](http://ong9pclk3.bkt.clouddn.com/robust_push.jpg)

**12. 加载测试补丁**

这里是点击界面上的 加载补丁 按钮进行补丁的加载和测试，如果没有意外，那么结果应该和上面效果图一致，如果有意外... 呵呵呵 那你就加油吧...

## 三、遗留问题

**1. 打包时空指针的问题：由于某些方法没有被 Robust 插入代码**，导致在开启补丁功能打包时，报错：

    something wrong when readAnnotation, patch method com.yu.app2demos.robust.BePatchedActivity.getText() haven't insert code by Robust.Cannot patch this method, method.signature  ()Ljava/lang/String;   cannot find class
     name com.yu.app2demos.robust.BePatchedActivity

从而引发空指针异常

    new add methods  list is
    
    new add classes list is
    
     patchMethodSignureSet is printed below
    
    :app:transformClassesWithAutoPatchTransformForRelease FAILED
    
    FAILURE: Build failed with an exception.
    
    * What went wrong:
    Execution failed for task ':app:transformClassesWithAutoPatchTransformForRelease'.
    > java.lang.NullPointerException (no error message)


修复的方法如下很简单:

```java

@Modify
public String getText() {
    return "ffffffffffffffffffffffffffffffffffffffffffffffffffff";
    //return "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh";
}

```

而且，我确定已经在 robust.xml 中配置可修复的包名了

```xml
<packname name="hotfixPackage">
    <name>com.yu.app2demos.robust</name>
</packname>
```

至今一脸懵逼，不知道为啥....感觉是和配置及访问域有关

**2. 打包生成补丁成功，补丁加载成功，但是使用时报被修改的方法找不到**

    ---- 这里开始是加载补丁的过程，结果是成功的：
    04-24 15:56:56.228 9162-9608/? D/PatchManipulateImp: fetchPatchList --> robustApkHash : efb3b434c06684d8b9c89a2111517a06
    04-24 15:56:56.228 9162-9608/? D/PatchManipulateImp: patch: patch_version_0.0.1   path: /storage/emulated/0/robust/patch.jar
    04-24 15:56:56.228 9162-9608/? D/PatchManipulateImp: ensurePatchExist patch_version_0.0.1  md5: null
    04-24 15:56:56.228 9162-9608/? D/PatchManipulateImp: verifyPatch: patch_version_0.0.1   path: /data/data/com.yu.myrobust/cache/robust/patch_temp.jar
    04-24 15:56:56.399 9162-9608/? D/robust: PatchsInfoImpl name:com.yu.myrobust.PatchesInfoImpl
    04-24 15:56:56.399 9162-9608/? D/robust: PatchsInfoImpl ok
    04-24 15:56:56.399 9162-9608/? D/robust: current path:com.yu.myrobust.MainActivity find:ChangeQuickRedirect com.yu.myrobust.MainActivityPatchControl
    04-24 15:56:56.399 9162-9608/? D/robust: changeQuickRedirectField set sucess com.yu.myrobust.MainActivityPatchControl
    04-24 15:56:56.419 9162-9162/? D/PatchLog: 应用补丁：patch_version_0.0.1 path: /storage/emulated/0/robust/patch.jar  result: true
    04-24 15:57:53.935 9162-9162/? D/robust: invoke method is com.yu.myrobust.MainActivityPatch.jump2SecondActivity() 
    
    ---这里开始是点击跳转按钮执行方法的时候，报方法找不到
    04-24 15:57:53.935 9162-9162/? W/System.err: java.lang.NoSuchMethodError: No direct method j()V in class Lcom/yu/myrobust/MainActivityPatch; or its super classes (declaration of 'com.yu.myrobust.MainActivityPatch' appears in /data/data/com.yu.myrobust/cache/robust/patch_temp.jar)
    04-24 15:57:53.935 9162-9162/? W/System.err:     at com.yu.myrobust.MainActivityPatch.RobustPublicjump2SecondActivity(MainActivityPatch.java)
    04-24 15:57:53.935 9162-9162/? W/System.err:     at com.yu.myrobust.MainActivityPatchControl.accessDispatch(PatchTemplate.java)
    04-24 15:57:53.935 9162-9162/? W/System.err:     at com.meituan.robust.PatchProxy.accessDispatchVoid(PatchProxy.java:46)

方法也很简单，修改从 MainActivity 跳转到 SecondActivity 时传递的参数：

```java
private void jump2SecondActivity() {
    Intent intent = new Intent(this, SecondActivity.class);
    //下面两个修改注释掉一个
    //intent.putExtra(K_MSG , "多情自古空余恨，此恨绵绵无绝期....");
    intent.putExtra(K_MSG , "怒发冲冠，凭栏处，潇潇雨歇，抬望眼");
    startActivity(intent);
}
```


## 四、完成代码

[对方戳了你一下](https://github.com/yuehongjie/MyRobust)
