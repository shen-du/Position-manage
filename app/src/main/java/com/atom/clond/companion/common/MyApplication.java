package com.atom.clond.companion.common;

import android.app.Application;
import android.content.Context;

import com.atom.clond.companion.http.HttpManager;
import com.atom.clond.companion.utils.LogUtils;

/**
 * 全局Application
 *
 * @author bohan.chen
 */
public class MyApplication extends Application {

    private static Application mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        LogUtils.init(Constants.IS_DEBUG);
        initHttp(Constants.IS_DEBUG);
        new CrashHandler().init();
    }

    /**
     * 初始化http
     */
    private void initHttp(boolean isDebug) {
        HttpManager.getInstance().initHttp(isDebug, Constants.SERVER_ADDRESS);
    }

    public static Context getAppContext() {
        return mContext;
    }

}
