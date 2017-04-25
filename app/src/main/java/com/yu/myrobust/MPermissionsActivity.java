package com.yu.myrobust;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017-2-16.
 * 申请权限的 Activity 基类
 */

public class MPermissionsActivity extends AppCompatActivity {

    private MPermissionsResultListener mListener;
    private int mRequestCode;//请求码
    private ArrayList<String> mNeverAskedPermissions;//被拒绝 而且点击了 NeverAskAgain
    private ArrayList<String> mDeniedPermissions;//被决绝的权限
    private ArrayList<String> mNeedRequestedPermissions;//需要请求的权限


    private void log(String msg) {
        Log.d("MPermissionsActivity" , msg);
    }

    /**
     * 其他继承自 BasePermissionActivity 的 Activity 用来申请权限的方法
     * @param permissions   要申请的权限数组
     * @param requestCode   请求码
     * @param listener      申请结果 回调监听
     */
    protected void performRequestPermissions(String[] permissions, int requestCode, MPermissionsResultListener listener){
        //权限数组判空
        if (permissions == null || permissions.length == 0) {
            return;
        }
        this.mRequestCode = requestCode;
        this.mListener = listener;
        clearPermissionsInfo();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Android Marshmallow

            log("Above Android M");

            if (checkEachSelfPermission(permissions)) {//判断是否需要申请权限

                log("需要申请权限");

                requestEachPermissions();
            }else {//已经拥有所有的权限

                log("已经拥有所有权限");

                if (mListener != null) {
                    mListener.onPermissionsAllGranted();
                }
            }
        }else {//不需要动态申请
            log("Below Android M");

            if (mListener != null) {
                mListener.onPermissionsAllGranted();
            }
        }
    }

    /**
     * 清除上次请求权限的结果
     */
    private void clearPermissionsInfo() {
        if (mDeniedPermissions != null)
            mDeniedPermissions.clear();
        if (mNeedRequestedPermissions != null)
            mNeedRequestedPermissions.clear();
        if (mNeverAskedPermissions != null)
            mNeverAskedPermissions.clear();
    }

    /**
     * 申请权限
     *
     */
    private void requestEachPermissions() {

        //申请权限,回调 onRequestPermissionsResult
        ActivityCompat.requestPermissions(this, mNeedRequestedPermissions.toArray(new String[mNeedRequestedPermissions.size()]), mRequestCode);

    }


    /**
     * 检查每个权限是否需要申请
     * @param permissions 权限数组
     * @return true 需要申请权限 ， false 已经拥有所有权限
     */
    private boolean checkEachSelfPermission(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this , permission) != PackageManager.PERMISSION_GRANTED) {
                if (mNeedRequestedPermissions == null) {
                    mNeedRequestedPermissions = new ArrayList<>(permissions.length);
                }
                //还没同意需要请求权限
                mNeedRequestedPermissions.add(permission);
            }
        }

        //如果需要申请的权限数组不为空 则返回 true 表示有权限需要申请
        return mNeedRequestedPermissions != null && !mNeedRequestedPermissions.isEmpty();
    }

    //权限申请结果回调
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (mRequestCode == requestCode && mListener != null) {

            if (grantResults.length > 0) {

                for (int index = 0; index < grantResults.length; index++) {
                    //拒绝授权
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        if (mDeniedPermissions == null) {
                            mDeniedPermissions = new ArrayList<>(grantResults.length);
                        }
                        //添加到未授权的列表
                        mDeniedPermissions.add(permissions[index]);
                    }
                }

                //同意所有授权
                if (mDeniedPermissions == null || mDeniedPermissions.size() <= 0) {

                    mListener.onPermissionsAllGranted();

                }else {//部分权限未同意

                    //回调 用户拒绝了某些权限
                    mListener.onPermissionsDenied(mDeniedPermissions);

                    //遍历未授权的权限，判断是否有 NeverAskAgain
                    for (String permission : mDeniedPermissions) {
                        if (!shouldShowRequestPermissionRationale(permission)) {
                            if (mNeverAskedPermissions == null) {
                                mNeverAskedPermissions = new ArrayList<>(mDeniedPermissions.size());
                            }
                            mNeverAskedPermissions.add(permission);
                        }
                    }

                    //某些权限被点击了 NeverAskAgain ，需要跳转到 设置 界面去开启
                    if (mNeverAskedPermissions != null && !mNeverAskedPermissions.isEmpty()) {

                        mListener.onPermissionsNeverAskAgain(mNeverAskedPermissions);

                    }
                    /*
                    else {
                        mListener.onPermissionsDenied(mDeniedPermissions);
                    }
                    */

                }
            }
        }
    }

}
