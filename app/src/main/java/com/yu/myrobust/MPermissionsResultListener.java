package com.yu.myrobust;

import java.util.List;

/**
 * Created by Administrator on 2017-2-16.
 * 权限申请结果回调
 */

public interface MPermissionsResultListener {
    /**
     * 同意了所有的权限
     */
    void onPermissionsAllGranted();

    /**
     * 拒绝了部分权限
     * @param deniedPermissions 被拒绝的权限列表
     */
    void onPermissionsDenied(List<String> deniedPermissions);

    /**
     * 某些权限被设置过 NeverAskAgain
     * @param neverAskedPermissions 设置过不再提示 的 权限列表
     */
    void onPermissionsNeverAskAgain(List<String> neverAskedPermissions);
}
