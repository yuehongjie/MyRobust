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
        //设置补丁文件存储的路径和文件名 会自动加上 .jar 后缀 ... sdcard/robust/patch.jar
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
