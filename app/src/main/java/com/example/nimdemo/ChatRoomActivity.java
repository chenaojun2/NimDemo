package com.example.nimdemo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.example.nimdemo.adapter.ChatRoomAdapter;
import com.example.nimdemo.observer.SimpleAVChatStateObserver;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.constant.AVChatUserRole;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;

import com.netease.nimlib.sdk.avchat.model.AVChatChannelInfo;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.avchat.model.AVChatParameters;

import com.netease.nimlib.sdk.avchat.video.AVChatCameraCapturer;
import com.netease.nimlib.sdk.avchat.video.AVChatSurfaceViewRenderer;
import com.netease.nimlib.sdk.avchat.video.AVChatVideoCapturerFactory;


import java.util.ArrayList;
import java.util.List;

public class ChatRoomActivity extends Activity {

    private static final String TAG = "ChatRoomActivity";
    private static final String ROOM_NAME = "青龙学习小组";
    public static final String CREATE_ROOM = "CREATE_ROOM";
    public static final String ADD_ROOM = "ADD_ROOM";

    private RecyclerView recyclerView;
    private AVChatSurfaceViewRenderer avChatSurfaceViewRendererOwner;
//    private AVChatSurfaceViewRenderer avChatSurfaceViewRendererFriend1;
//    private AVChatSurfaceViewRenderer avChatSurfaceViewRendererFriend2;

    private String type;
    private String extraMessage = "随便吧";
    private AVChatCameraCapturer videoCapturer;

    private List<String> accountList = new ArrayList<>();
    private ChatRoomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        initData();
        registerObserves(true);
        initUI(type);
    }

    private void initData() {
        type = getIntent().getStringExtra("TYPE");
        adapter = new ChatRoomAdapter(accountList);
    }

    private void registerObserves(boolean register) {
        AVChatManager.getInstance().observeAVChatState(avchatStateObserver, register);

    }

    private void initUI(String type) {
        recyclerView = (RecyclerView) findViewById(R.id.tvr_friends);
        recyclerView.setLayoutManager (new GridLayoutManager(NimApplication.getContext(),3, GridLayoutManager.VERTICAL,false));
        recyclerView.setAdapter(adapter);
        avChatSurfaceViewRendererOwner = (AVChatSurfaceViewRenderer) findViewById(R.id.tvr_owner);
//        avChatSurfaceViewRendererFriend1 = (AVChatSurfaceViewRenderer) findViewById(R.id.tvr_friend1);
//        avChatSurfaceViewRendererOwner.setZOrderOnTop(true);
//        avChatSurfaceViewRendererFriend1.setZOrderOnTop(true);
//        avChatSurfaceViewRendererFriend2 = (AVChatSurfaceViewRenderer) findViewById(R.id.tvr_friend2);
        if(type.equals(CREATE_ROOM)){
            createRoom(ROOM_NAME,extraMessage);
        }else if(type.equals(ADD_ROOM)){
            addRoom(ROOM_NAME);
        }
    }


    public void  createRoom(final String roomName, String extraMessage) {
        AVChatManager.getInstance().createRoom(roomName, extraMessage, new AVChatCallback<AVChatChannelInfo>() {
            @Override
            public void onSuccess(AVChatChannelInfo avChatChannelInfo) {
                Log.e(TAG,"房间创建成功");
                addRoom(roomName);
            }

            @Override
            public void onFailed(int code) {
                Log.e(TAG,"房间创建失败"+code);
            }

            @Override
            public void onException(Throwable exception) {
                Log.e(TAG,"房间创建异常"+exception);
            }
        });
    }


    private void addRoom(String roomName) {
        //开启音视频引擎
        AVChatManager.getInstance().enableRtc();
        //设置场景, 如果需要高清音乐场景，设置 AVChatChannelProfile#CHANNEL_PROFILE_HIGH_QUALITY_MUSIC
        //AVChatManager.getInstance().setChannelProfile(CHANNEL_PROFILE_DEFAULT);
        //设置通话可选参数
        AVChatParameters parameters = new AVChatParameters();
        AVChatManager.getInstance().setParameters(parameters);
        /**设置角色*/
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_SESSION_MULTI_MODE_USER_ROLE, AVChatUserRole.NORMAL);
        //视频通话设置
        AVChatManager.getInstance().enableVideo();
        AVChatManager.getInstance().setupLocalVideoRender(avChatSurfaceViewRendererOwner, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
        //设置视频采集模块
        videoCapturer = AVChatVideoCapturerFactory.createCameraCapturer(true);
        AVChatManager.getInstance().setupVideoCapturer(videoCapturer);
        //设置视频质量调整策略
        //清晰优先
        //AVChatManager.getInstance().setVideoQualityStrategy(AVChatVideoQualityStrategy.PreferFrameRate);
        //开启视频预览
        AVChatManager.getInstance().startVideoPreview();
        //加入房间
        AVChatManager.getInstance().joinRoom2(roomName, AVChatType.VIDEO, new AVChatCallback<AVChatData>() {
            @Override
            public void onSuccess(AVChatData avChatData) {
                Log.d(TAG,"成功加入房间");
            }

            @Override
            public void onFailed(int code) {
                Log.e(TAG,"加入房间失败"+code);
            }

            @Override
            public void onException(Throwable exception) {
                Log.e(TAG,"加入房间异常"+exception);
            }
        });
    }



    private SimpleAVChatStateObserver avchatStateObserver = new SimpleAVChatStateObserver() {

        @Override
        public void onUserJoined(String account) {
            Log.e(TAG,"检测到"+account+"加入");
            accountList.add(account);
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onUserLeave(String account, int event) {
            super.onUserLeave(account, event);
            Log.e(TAG,"检测到"+account+"离开");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭视频预览
        AVChatManager.getInstance().stopVideoPreview();
        // 如果是视频通话，关闭视频模块
        AVChatManager.getInstance().disableVideo();
        AVChatManager.getInstance().leaveRoom2(ROOM_NAME,new AVChatCallback<Void>() {
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
        //关闭音视频引擎
        AVChatManager.getInstance().disableRtc();
    }

}
