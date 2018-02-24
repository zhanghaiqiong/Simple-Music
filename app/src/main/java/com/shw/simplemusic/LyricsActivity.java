package com.shw.simplemusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.yanzhenjie.sofia.Bar;
import com.yanzhenjie.sofia.Sofia;

import cn.zhaiyifan.lyric.LyricUtils;
import cn.zhaiyifan.lyric.widget.LyricView;
import me.yokeyword.fragmentation_swipeback.SwipeBackActivity;

public class LyricsActivity extends SwipeBackActivity {

    private MediaPlayerService.MediaPlayerBinder mediaPlayerBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mediaPlayerBinder = (MediaPlayerService.MediaPlayerBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private LyricView lyricView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);
        //状态栏颜色
        Bar bar= Sofia.with(this);
        bar.statusBarBackground(Color.WHITE);
        bar.statusBarDarkFont();
        //绑定服务
        Intent bindIntent = new Intent(this, MediaPlayerService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
        //获取广播实例
        localBroadcastManager=LocalBroadcastManager.getInstance(this);
        intentFilter=new IntentFilter();
        intentFilter.addAction("com.shw.simplemusic.LOCAL_BROADCAST");
        localReceiver=new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver,intentFilter);
        //控件
        lyricView=findViewById(R.id.lyrics_layout_lyricView);
        lyricView.setLyric(LyricUtils.parseLyric(getResources().openRawResource(R.raw.lyrics),"UTF-8"));
        lyricView.setLyricIndex(0);
        lyricView.play();
    }
    
    class LocalReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "哈哈哈", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        localBroadcastManager.unregisterReceiver(localReceiver);
    }
}
