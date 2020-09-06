package com.atom.clond.companion.activity;

import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.atom.clond.companion.R;
import com.atom.clond.companion.bean.OrganizationBean;
import com.atom.clond.companion.common.Constants;
import com.atom.clond.companion.common.GlobalValue;
import com.atom.clond.companion.http.HttpCallback;
import com.atom.clond.companion.http.HttpManager;
import com.atom.clond.companion.req.HttpRequest;
import com.atom.clond.companion.utils.SPUtil;
import com.atom.clond.companion.utils.ToastUtils;

import java.util.List;

/**
 * 启动页
 *
 * @author bohan.chen
 */
public class SplashActivity extends AppCompatActivity {

    private EditText etToken;
    private Button btnLogin;
    private String token;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initView();
    }

    private void initView() {
        etToken = findViewById(R.id.etToken);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                token = getString(R.string.Token);
                if (TextUtils.isEmpty(token)) {
                    ToastUtils.showShort("token不能为空");
                    v.setEnabled(true);
                } else {
                    ToastUtils.showShort("正在登录中");
                    HttpManager.getInstance().addToken2Header(token);
                    initData();
                }

            }
        });
        token = getString(R.string.Token);
        HttpManager.getInstance().addToken2Header(token);
        initData();
        token = (String) SPUtil.get(Constants.SP_TOKEN, "");
        etToken.setText(token);

    }

    private void initData() {
        HttpRequest.getOrganizationList(new HttpCallback<List<OrganizationBean>>() {
            @Override
            public void onSuccess(List<OrganizationBean> data) {
                Constants.HTTP_TOKEN = token;
                SPUtil.put(Constants.SP_TOKEN, token);
                if (data.size() > 0) {
                    OrganizationBean itemData = data.get(0);
                    GlobalValue.orgId = itemData.getId();
                    Intent intent = new Intent(getApplicationContext(), DeviceGroupActivity.class);
                    intent.putExtra(ActivityKey.TITLE, itemData.getName());
                    startActivity(intent);
                    finish();
                } else {
                    ToastUtils.showShort("获取不到机构，点击重试");
                }
            }

            @Override
            public void onError(int code, String errMsg) {
                ToastUtils.showShort(errMsg);
            }

            @Override
            public void onFinish() {
                btnLogin.setEnabled(true);
            }
        });
    }

}
