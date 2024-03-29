package com.example.nimdem;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.nimdem.observer.SimpleAVChatStateObserver;


import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatEventType;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;
import com.netease.nimlib.sdk.avchat.model.AVChatCalleeAckEvent;
import com.netease.nimlib.sdk.avchat.model.AVChatCommonEvent;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.avchat.model.AVChatNotifyOption;
import com.netease.nimlib.sdk.avchat.model.AVChatParameters;
import com.netease.nimlib.sdk.avchat.video.AVChatCameraCapturer;
import com.netease.nimlib.sdk.avchat.video.AVChatSurfaceViewRenderer;
import com.netease.nimlib.sdk.avchat.video.AVChatVideoCapturerFactory;

import java.lang.ref.WeakReference;


public class AVChatActivity extends Activity {


    public static final int FROM_BROADCASTRECEIVER = 0; // 来自广播
    public static final int FROM_INTERNAL = 1; // 来自发起方

    private final String TAG = "AVChatActivity";
    private final String KRY_CALL_CONFIG = "KEY_CALL_CONFIG";
    private static final String KEY_SOURCE = "SOURCE";
    private static final String ACCOUNT = "895941515";

    private AVChatSurfaceViewRenderer mtextureViewRenderer;
    private AVChatSurfaceViewRenderer textureViewRendererf;
    private Button btnHangup;
    private Button btnSwitch;
    private Button btnCloseVideo;
    private Button btnCloseAudio;
    private TextView state;

    private CallStateEnum callingState = CallStateEnum.UNKNOWN;
    private AVChatCameraCapturer mVideoCapturer;
    private AVChatData avChatData;
    private boolean destroyRTC = false;
    private ViewControlHanlder mHandler;
    private CountTimeThread countTimeThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avchat);
        initUI();
        registerObserves(true);


    }

    private void initUI() {
        mtextureViewRenderer = (AVChatSurfaceViewRenderer) findViewById(R.id.tvr);
        textureViewRendererf = (AVChatSurfaceViewRenderer) findViewById(R.id.tvr_friendd);
        btnHangup = (Button) findViewById(R.id.hangup);
        btnSwitch = findViewById(R.id.switc);
        btnCloseVideo = findViewById(R.id.video_close);
        btnCloseAudio = findViewById(R.id.audio_close);
        state = findViewById(R.id.state);
        textureViewRendererf.setZOrderOnTop(true);

        switch (getIntent().getIntExtra(KEY_SOURCE,-1)){
            case FROM_BROADCASTRECEIVER:
                receiveInComingCall();
                break;
            case FROM_INTERNAL:
                outGoingCalling(ACCOUNT, AVChatType.VIDEO);
                break;
            default:
                break;
        }
    }

    //注册事件监听
    private void registerObserves(boolean register) {
        AVChatManager.getInstance().observeCalleeAckNotification(ackbserver,register);
        AVChatManager.getInstance().observeAVChatState(avchatStateObserver, register);
        AVChatManager.getInstance().observeHangUpNotification(callHangupObserver, register);
        btnHangup.setOnClickListener(clickListener);
        btnCloseAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMute();
            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        btnCloseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AVChatManager.getInstance().isLocalVideoMuted()) {
                    AVChatManager.getInstance().muteLocalVideo(true);
                }else{
                    AVChatManager.getInstance().muteLocalVideo(false);
                }
            }
        });
    }

    private void outGoingCalling(String account, final AVChatType callTypeEnum) {
        AVChatNotifyOption notifyOption = new AVChatNotifyOption();
        //附加字段
        notifyOption.extendMessage = "没有";
        //是否兼容WebRTC模式
        //notifyOption.
        //默认forceKeepCalling为true，开发者如果不需要离线持续呼叫功能可以将forceKeepCalling设为false
        notifyOption.forceKeepCalling = false;
        //打开Rtc模块
        AVChatManager.getInstance().enableRtc();
        this.callingState = (callTypeEnum == AVChatType.VIDEO ? CallStateEnum.VIDEO : CallStateEnum.AUDIO);
        //设置自己需要的可选参数
        AVChatParameters avChatParameters = new AVChatParameters();
        AVChatManager.getInstance().setParameters(avChatParameters);

        //视频通话
        if (callTypeEnum == AVChatType.VIDEO) {
            //打开视频模块
            AVChatManager.getInstance().enableVideo();
            //创建视频采集模块并且设置到系统中
            if (mVideoCapturer == null) {
                mVideoCapturer = AVChatVideoCapturerFactory.createCameraCapturer(true);
                AVChatManager.getInstance().setupVideoCapturer(mVideoCapturer);
            }

            //设置自定义音频
            AVChatManager.getInstance().setExternalAudioSource(true);
            int bs = AudioRecord.getMinBufferSize(8000,
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT ,bs);
            int state = ar.getState();
            Log.e("麦克风","麦克风状态"+state);
            ar.startRecording();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        byte[] data = new byte[bs];
                        ar.read(data, 0, bs);
                        int code = AVChatManager.getInstance().pushExternalAudioData(data, data.length / 2, 8000, 1,
                                AudioFormat.ENCODING_PCM_16BIT, true);
                        Log.e("麦克风","推流返回码"+code);
                    }

                }
            }).start();

            //设置本地预览画布
            AVChatManager.getInstance().setupLocalVideoRender(mtextureViewRenderer, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
            //开始视频预览
            AVChatManager.getInstance().startVideoPreview();
        }

        //呼叫
        AVChatManager.getInstance().call2(account, callTypeEnum, notifyOption, new AVChatCallback<AVChatData>() {
            @Override
            public void onSuccess(AVChatData data) {
                //avChatData = data;
                //发起会话成功
                avChatData = data;
                Log.e("AVChatData",data.toString());
            }

            @Override
            public void onFailed(int code) {
                Log.e(TAG,"呼叫失败");
                callingState = CallStateEnum.UNKNOWN;
                if (code == ResponseCode.RES_FORBIDDEN) {
                    Toast.makeText(AVChatActivity.this, "缺少权限", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AVChatActivity.this, "呼叫失败", Toast.LENGTH_SHORT).show();
                }
                closeSessions();
            }

            @Override
            public void onException(Throwable exception) {
            }
        });

    }

    private void receiveInComingCall() {

        callingState = CallStateEnum.VIDEO_CONNECTING;
        avChatData = (AVChatData) getIntent().getSerializableExtra(KRY_CALL_CONFIG);
        AVChatManager.getInstance().enableRtc();
        if (mVideoCapturer == null) {
            mVideoCapturer = AVChatVideoCapturerFactory.createCameraCapturer(true);
            AVChatManager.getInstance().setupVideoCapturer(mVideoCapturer);
        }
        //设置参数
        //AVChatManager.getInstance().setParameters(avChatParameters);
        if (callingState == CallStateEnum.VIDEO_CONNECTING) {
            AVChatManager.getInstance().enableVideo();
            AVChatManager.getInstance().setupLocalVideoRender(mtextureViewRenderer, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
            AVChatManager.getInstance().startVideoPreview();
        }

        AVChatManager.getInstance().accept2(avChatData.getChatId(), new AVChatCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e(TAG, "accept success");
//                isCallEstablish.set(true);
//                canSwitchCamera = true;
            }

            @Override
            public void onFailed(int code) {
                if (code == -1) {
                    Toast.makeText(AVChatActivity.this, "本地音视频启动失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AVChatActivity.this, "建立连接失败", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "accept onFailed->" + code);
                handleAcceptFailed();
            }

            @Override
            public void onException(Throwable exception) {
                Log.d(TAG, "accept exception->" + exception);
                handleAcceptFailed();
            }
        });
    }

    //挂电话
    private void hangUp() {
        // 如果是视频通话，关闭视频模块
        AVChatManager.getInstance().disableVideo();
        // 如果是视频通话，需要先关闭本地预览
        AVChatManager.getInstance().stopVideoPreview();
        //挂断
        AVChatManager.getInstance().hangUp2(avChatData.getChatId(), new AVChatCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }

            @Override
            public void onFailed(int code) {

            }

            @Override
            public void onException(Throwable exception) {

            }
        });
        //销毁音视频引擎和释放资源
        AVChatManager.getInstance().disableRtc();
        finish();
    }

    private void handleAcceptFailed() {
        if (destroyRTC) {
            return;
        }
        if (callingState == CallStateEnum.VIDEO_CONNECTING) {
            AVChatManager.getInstance().stopVideoPreview();
            AVChatManager.getInstance().disableVideo();
        }
        AVChatManager.getInstance().disableRtc();
        destroyRTC = true;
        closeSessions();

    }

    private void closeSessions() {
        AVChatManager.getInstance().stopVideoPreview();
        AVChatManager.getInstance().disableVideo();
        AVChatManager.getInstance().disableRtc();
        finish();
    }

    public void toggleMute() {
        if (!AVChatManager.getInstance().isLocalAudioMuted()) { // isMute是否处于静音状态
            // 关闭音频
            AVChatManager.getInstance().muteLocalAudio(true);
        } else {
            // 打开音频
            AVChatManager.getInstance().muteLocalAudio(false);
        }
    }

    public void switchCamera() {
        mVideoCapturer.switchCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(callingState == CallStateEnum.UNKNOWN)
            return;
        hangUp();

    }

    /************************通话过程状态监听器******************************/


    private Observer<AVChatCalleeAckEvent> ackbserver = new Observer<AVChatCalleeAckEvent>() {
        @Override
        public void onEvent(AVChatCalleeAckEvent ackInfo) {
            if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_BUSY) {
                hangUp();
                // 对方正在忙
            } else if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_REJECT) {
                Log.e(TAG,"拒绝接听");
                hangUp();
                // 对方拒绝接听
            } else if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_AGREE) {
                // 对方同意接听
                Log.e(TAG,"同意接听");
            }
        }
    };

    //挂断
    private Observer<AVChatCommonEvent> callHangupObserver = new Observer<AVChatCommonEvent>() {
        @Override
        public void onEvent(AVChatCommonEvent avChatCommonEvent) {
            AVChatManager.getInstance().stopVideoPreview();
            AVChatManager.getInstance().disableVideo();
            AVChatManager.getInstance().disableRtc();
            finish();
        }
    };

    //对通话状态进行监听
    private SimpleAVChatStateObserver avchatStateObserver = new SimpleAVChatStateObserver() {

        @Override
        public void onUserJoined(String account) {
            Log.e(TAG,account+"加入");
            AVChatManager.getInstance().setupLocalVideoRender(textureViewRendererf,false,AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
            AVChatManager.getInstance().setupRemoteVideoRender(account,mtextureViewRenderer,false,AVChatVideoScalingType.SCALE_ASPECT_BALANCED);

        }

        @Override
        public void onCallEstablished() {
            Log.e(TAG,"建立稳定链接");
            btnHangup.setVisibility(View.VISIBLE);
            mHandler = new ViewControlHanlder(AVChatActivity.this);
            countTimeThread = new CountTimeThread(5);
            countTimeThread.start();

        }

    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hangUp();
        }
    };


    /*****************无操作控件消失********************/
    class ViewControlHanlder extends Handler{

        private final int MSG_HIDE= 0x001;

        private WeakReference<AVChatActivity> weakRef;

        ViewControlHanlder(AVChatActivity avChatActivity){
            weakRef = new WeakReference<>(avChatActivity);
        }

        @Override
        public void handleMessage(Message msg) {

            AVChatActivity avChatActivity = weakRef.get();

            super.handleMessage(msg);
            switch (msg.what){
                case MSG_HIDE:
                    avChatActivity.hinde();
                    break;
            }
        }

        public void sendHideControllMessage(){

            obtainMessage(MSG_HIDE).sendToTarget();

        }
    }

    private void hinde() {
        btnCloseAudio.setVisibility(View.GONE);
        btnCloseVideo.setVisibility(View.GONE);
        btnSwitch.setVisibility(View.GONE);
        btnHangup.setVisibility(View.GONE);
        state.setVisibility(View.GONE);
    }

    class CountTimeThread extends Thread{

        private long maxVisibleTime;
        private long startVisibleTime;

        public CountTimeThread(int second){
            maxVisibleTime = second * 1000;//换算为毫秒
            setDaemon(true);//设置为后台进程
        }

        private void reset() {
            startVisibleTime = System.currentTimeMillis();
        }

        public void run() {

            startVisibleTime = System.currentTimeMillis();//初始化开始时间
            while (true) {
                if (startVisibleTime + maxVisibleTime < System.currentTimeMillis()) {
                    mHandler.sendHideControllMessage();
                    startVisibleTime = System.currentTimeMillis();
                }
            }
        }
    }

    //触碰事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN){
            boolean isVisable = (btnHangup.getVisibility()==View.VISIBLE);
            if(!isVisable){
                btnCloseAudio.setVisibility(View.VISIBLE);
                btnCloseVideo.setVisibility(View.VISIBLE);
                btnSwitch.setVisibility(View.VISIBLE);
                btnHangup.setVisibility(View.VISIBLE);
                state.setVisibility(View.VISIBLE);
            }
        }
        return super.onTouchEvent(event);
    }
}
