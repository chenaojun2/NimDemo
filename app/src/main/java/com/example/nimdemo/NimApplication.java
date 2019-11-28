package com.example.nimdemo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.util.NIMUtil;

import static android.R.attr.data;

/**
 * Created by 陈澳军 on 2019/11/18.
 */

public class NimApplication extends Application{

    private static final String KRY_CALL_CONFIG = "KEY_CALL_CONFIG";
    private static final String KEY_SOURCE = "SOURCE";

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        NIMClient.init(this, loginInfo(), options());
        if (NIMUtil.isMainProcess(this)) {
            // 用于做一些在主进程中做的事
        }
    }
    // 如果返回值为 null，则全部使用默认参数。
    private SDKOptions options() {
        return null;
    }
    // 如果已经存在用户登录信息，返回LoginInfo，否则返回null即可
    private LoginInfo loginInfo() {
        return null;
    }
    public static Context getContext(){
        return context;
    }
    /*****************************监听器****************************************/
//    private static Observer<AVChatData> inComingCallObserver = new Observer<AVChatData>() {
//        @Override
//        public void onEvent(AVChatData avChatData) {
//
//        }
//    };
}
