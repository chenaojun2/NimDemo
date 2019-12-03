package com.example.nimdem;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;

public class MainActivity extends AppCompatActivity {
    private static final String KRY_CALL_CONFIG = "KEY_CALL_CONFIG";
    private static final String KEY_SOURCE = "SOURCE";
    private static final String ACCOUNT = "895941516";
    private static final String[] PERMISSION = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };
    private static final int VIDEO_PERMISSIONS_CODE = 1;

    TextView tv;
    Button login_btn;
    Button call_btn;
    Button create_btn;
    Button add_btn;
    Button t_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        initUI();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NIMClient.getService(MsgService.class).setChattingAccount("895941515", SessionTypeEnum.None);
    }


    @Override
    protected void onPause() {
        super.onPause();
        NIMClient.getService(MsgService.class).setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.P2P);
    }

    private void initListener() {
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
        call_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doCall();
            }
        });
        create_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoom();
            }
        });
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRoom();
            }
        });
        t_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                startActivity(intent);
            }
        });
        //AVChatManager.getInstance().observeIncomingCall(observerInComingCall,true);

    }

    private void initUI() {
        tv = (TextView) findViewById(R.id.textView);
        login_btn = (Button) findViewById(R.id.button);
        call_btn = (Button) findViewById(R.id.button2);
        create_btn = (Button) findViewById(R.id.button3);
        add_btn = (Button) findViewById(R.id.button4);
        t_btn = findViewById(R.id.button5);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this,PERMISSION,VIDEO_PERMISSIONS_CODE);
        }
    }

    private void addRoom() {
        Intent intent = new Intent(this,ChatRoomActivity.class);
        intent.putExtra("TYPE",ChatRoomActivity.ADD_ROOM);
        startActivity(intent);
    }

    private void createRoom() {
        Intent intent = new Intent(this,ChatRoomActivity.class);
        intent.putExtra("TYPE",ChatRoomActivity.CREATE_ROOM);
        startActivity(intent);
    }

    private void doCall() {
        Intent intent = new Intent(MainActivity.this,AVChatActivity.class);
        intent.putExtra(KEY_SOURCE,AVChatActivity.FROM_INTERNAL);
        startActivity(intent);
    }


    private void doLogin() {
        LoginInfo info = new LoginInfo(ACCOUNT,"123456"); // 账号、密码
        RequestCallback<LoginInfo> callback =
                new RequestCallback<LoginInfo>() {
                    // 可以在此保存LoginInfo到本地，下次启动APP做自动登录用
                    @Override
                    public void onSuccess(LoginInfo param) {
                        tv.setText("成功登录");
                    }

                    @Override
                    public void onFailed(int code) {
                        tv.setText("登录失败");
                    }

                    @Override
                    public void onException(Throwable exception) {

                    }
                };
        NIMClient.getService(AuthService.class).login(info)
                .setCallback(callback);
    }

    private void refusedCall(AVChatData data){
        Toast.makeText(MainActivity.this, "已经拒接", Toast.LENGTH_SHORT).show();
        AVChatManager.getInstance().hangUp2(data.getChatId(), new AVChatCallback<Void>() {
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
    }

    public static void receiveCall(AVChatData data){
        Toast.makeText(NimApplication.getContext(), "建立连接", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(NimApplication.getContext(),AVChatActivity.class);
        intent.putExtra(KEY_SOURCE,AVChatActivity.FROM_BROADCASTRECEIVER);
        intent.putExtra(KRY_CALL_CONFIG,data);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        NimApplication.getContext().startActivity(intent);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case VIDEO_PERMISSIONS_CODE:
                if (grantResults.length == PERMISSION.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            //弹出对话框引导用户去设置
                            showDialog();
                            Toast.makeText(MainActivity.this, "请求权限被拒绝", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                }else{
                    Toast.makeText(MainActivity.this, "已授权", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //弹出提示框
    private void showDialog(){
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("语音聊天需要相机、录音权限，是否去设置？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        goToAppSetting();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private Observer<AVChatData> observerInComingCall = new Observer<AVChatData>() {
        @Override
        public void onEvent(final AVChatData data) {
            Log.e("接受",data.toString());
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("来电啦")//设置对话框的标题
                    .setMessage("来自"+data.getAccount())//设置对话框的内容
                    .setNegativeButton("拒接", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            refusedCall(data);
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("接听", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            receiveCall(data);
                            dialog.dismiss();
                        }
                    }).create();
            dialog.show();
        }
    };



}
