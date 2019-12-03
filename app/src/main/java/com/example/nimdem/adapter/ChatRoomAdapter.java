package com.example.nimdem.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.nimdem.NimApplication;
import com.example.nimdem.R;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;
import com.netease.nimlib.sdk.avchat.video.AVChatSurfaceViewRenderer;

import java.util.List;

/**
 * Created by 陈澳军 on 2019/11/22.
 */

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    private List<String> accountList;


    static class ViewHolder extends RecyclerView.ViewHolder{
        private AVChatSurfaceViewRenderer avChatSurfaceViewRenderer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avChatSurfaceViewRenderer = (AVChatSurfaceViewRenderer) itemView.findViewById(R.id.tvr_friend);
            avChatSurfaceViewRenderer.setZOrderOnTop(true);
        }

    }

    public ChatRoomAdapter(List<String> accountList) {
        this.accountList = accountList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(NimApplication.getContext()).inflate(R.layout.room_item,parent,false);
        ChatRoomAdapter.ViewHolder viewHolder = new ChatRoomAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        boolean a = AVChatManager.getInstance().setupRemoteVideoRender(accountList.get(position),holder.avChatSurfaceViewRenderer,false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
        Log.e("绑定画布"," "+a);
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

}
