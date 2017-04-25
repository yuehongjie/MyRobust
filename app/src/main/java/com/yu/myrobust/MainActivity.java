package com.yu.myrobust;

import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.meituan.robust.Patch;
import com.meituan.robust.PatchExecutor;
import com.meituan.robust.RobustCallBack;
import com.meituan.robust.patch.annotaion.Modify;

import java.util.List;

public class MainActivity extends MPermissionsActivity implements View.OnClickListener {

    public static final String K_MSG = "MSG_RECEIVE";
    private TextView tvLog;
    private String[] requestPermissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_jump).setOnClickListener(this);
        findViewById(R.id.btn_patch).setOnClickListener(this);
        tvLog = (TextView) findViewById(R.id.tv_patch_log);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_jump) {
            jump2SecondActivity();
        }else if (view.getId() == R.id.btn_patch){
            performRequestPermissions(requestPermissions, 123, new MPermissionsResultListener() {
                @Override
                public void onPermissionsAllGranted() {
                    loadPatch();
                }

                @Override
                public void onPermissionsDenied(List<String> deniedPermissions) {
                    print("没有读写 SD 卡的权限");
                }

                @Override
                public void onPermissionsNeverAskAgain(List<String> neverAskedPermissions) {
                    print("拒绝 SD 卡读写权限的再申请");
                }
            });

        }
    }

    private void loadPatch() {
        tvLog.setText("开启补丁");
        new PatchExecutor(this, new PatchManipulateImp(), new RobustCallBack() {
            @Override
            public void onPatchListFetched(boolean result, boolean isNet) {
                print("\n获取补丁列表: " + result +" isNet: "+ isNet);
            }

            @Override
            public void onPatchFetched(boolean result, boolean isNet, Patch patch) {
                print("\n获取补丁：" + patch.getName() + " path: " + patch.getLocalPath() + "  result: "+result);
            }

            @Override
            public void onPatchApplied(boolean result, Patch patch) {
                print("\n应用补丁：" + patch.getName() + " path: " + patch.getLocalPath() + "  result: "+result);
            }

            @Override
            public void logNotify(String log, String where) {
                print("\n补丁日志：" + log + "  at: " + where);
            }

            @Override
            public void exceptionNotify(Throwable throwable, String where) {
                print("\n异常：" + throwable.getMessage() + "  at: " + where);
            }
        }).start();
    }



    private void jump2SecondActivity() {
        Intent intent = new Intent(this, SecondActivity.class);
        //intent.putExtra(K_MSG , "多情自古空余恨，此恨绵绵无绝期....");
        //intent.putExtra(K_MSG , "怒发冲冠，凭栏处，潇潇雨歇，抬望眼");
        startActivity(intent);
    }

    private void print(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLog.append(msg);
                Log.d("PatchLog", msg);
            }
        });
    }
}
