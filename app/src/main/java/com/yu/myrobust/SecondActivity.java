package com.yu.myrobust;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.meituan.robust.patch.annotaion.Add;
import com.meituan.robust.patch.annotaion.Modify;

public class SecondActivity extends AppCompatActivity {

    @Override
    @Modify
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        TextView tvReceive = (TextView) findViewById(R.id.tv_receive);

        //tvReceive.setText("我是错误的代码啊........");
        tvReceive.setText(getInfo());
    }
    @Add
    public String getInfo(){
        return "我是正确的代码..... 怒发冲冠，凭栏处，潇潇雨歇，抬望眼";
    }
}
