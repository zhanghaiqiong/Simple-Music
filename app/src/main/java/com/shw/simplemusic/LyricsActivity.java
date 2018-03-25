package com.shw.simplemusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.sofia.Bar;
import com.yanzhenjie.sofia.Sofia;

import java.io.File;

import me.wcy.lrcview.LrcUtils;
import me.wcy.lrcview.LrcView;
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

    public static LrcView lyricView;
    private TextView tv_songName;
    private TextView tv_singerName;

    private String lyricsDir = Environment.getExternalStorageDirectory().getPath()+"/kgmusic/download/lyrics";

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
        tv_songName=findViewById(R.id.lyrics_layout_songName);
        tv_singerName=findViewById(R.id.lyrics_layout_singerName);
        lyricView=findViewById(R.id.lyrics_layout_lyricView);
        lyricView.setOnPlayClickListener(new LrcView.OnPlayClickListener() {
            @Override
            public boolean onPlayClick(long time) {
                MainActivity.setPlaybackProgress(time);
                return true;
            }
        });
        //Log.d("shw",MainActivity.getPlayingSong().getFileHash());
        //String path="/sdcard/kgmusic/download/lyrics/"+MainActivity.getPlayingSong().getFileName()+".lrc";
        //lyricView.loadLrc(new File(path));
        Song song=MainActivity.getPlayingSong();
        tv_songName.setText(song.getSongName());
        tv_singerName.setText(song.getSingerName());
        //加载歌词
        String lyricsPath = lyricsDir + "/" + song.getFileName() + ".lrc";
        Log.d("shw",lyricsPath);
        LyricsActivity.lyricView.loadLrc(new File(lyricsPath));
    }
    
    class LocalReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            tv_songName.setText(intent.getStringExtra("songName"));
            tv_singerName.setText(intent.getStringExtra("singerName"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        localBroadcastManager.unregisterReceiver(localReceiver);
    }
}
