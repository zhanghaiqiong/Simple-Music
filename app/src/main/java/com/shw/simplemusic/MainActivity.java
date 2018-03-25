package com.shw.simplemusic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.sofia.Bar;
import com.yanzhenjie.sofia.Sofia;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.yokeyword.fragmentation.SupportActivity;

public class MainActivity extends SupportActivity {

    private boolean isExit = false;

    public static ConstraintLayout statuBar;
    public static ProgressBar progressBar;
    public static TextView tv_songName;
    public static TextView tv_lyrics;
    public static TextView btn_previousPiece;
    public static TextView btn_play;
    public static TextView btn_nextTrack;

    private static MediaPlayerService.MediaPlayerBinder mediaPlayerBinder;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //状态栏颜色
        Bar bar= Sofia.with(this);
        bar.statusBarBackground(getResources().getColor(R.color.colorPrimary));
        //获取广播实例
        localBroadcastManager=LocalBroadcastManager.getInstance(this);
        intentFilter=new IntentFilter();
        intentFilter.addAction("com.shw.simplemusic.LOCAL_BROADCAST");
        localReceiver=new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver,intentFilter);
        //初始化控件实例
        initView();
        //加载主碎片
        if (findFragment(LocalMusicFragment.class) == null) {
            loadRootFragment(R.id.main_layout_frameLayout, new LocalMusicFragment());
        }
        //绑定服务
        Intent bindIntent = new Intent(this, MediaPlayerService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }

    //收到广播
    class LocalReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            tv_songName.setText(intent.getStringExtra("songName"));
            tv_lyrics.setText(intent.getStringExtra("singerName"));
            if (intent.getBooleanExtra("isPlaying",false)){
                //播放状态
                MainActivity.btn_play.setText(R.string.iconfont_pause);
            }else {
                //停止状态
                MainActivity.btn_play.setText(R.string.iconfont_play);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        localBroadcastManager.unregisterReceiver(localReceiver);
    }

    private void initView() {
        Typeface iconfont = Typeface.createFromAsset(getAssets(), "iconfont/iconfont.ttf");
        statuBar=findViewById(R.id.main_statuBar);
        statuBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,LyricsActivity.class);
                Song song=mediaPlayerBinder.getPlayingSong();
                if (song==null){
                    Toast.makeText(MainActivity.this, "当前没有正在播放的歌曲。", Toast.LENGTH_SHORT).show();
                }else {
                    startActivity(intent);
                }
            }
        });
        progressBar = findViewById(R.id.main_statuBar_progressBar);
        tv_songName = findViewById(R.id.main_statuBar_songName);
        tv_lyrics = findViewById(R.id.main_statuBar_lyrics);
        btn_previousPiece = findViewById(R.id.main_statuBar_previousPiece);
        btn_previousPiece.setTypeface(iconfont);
        btn_previousPiece.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerBinder.previousPiece();
            }
        });
        btn_play = findViewById(R.id.main_statuBar_play);
        btn_play.setTypeface(iconfont);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayerBinder.isPlaying()) {
                    mediaPlayerBinder.pausePlayback();
                } else {
                    mediaPlayerBinder.startPlaying();
                }
            }
        });
        btn_nextTrack = findViewById(R.id.main_statuBar_nextTrack);
        btn_nextTrack.setTypeface(iconfont);
        btn_nextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayerBinder.nextTrack();
            }
        });
    }

    public static void playNewMusic(List<Song> songs,int position) {
        mediaPlayerBinder.playNewSong(songs, position);
    }

    public static Song getPlayingSong(){
        return mediaPlayerBinder.getPlayingSong();
    }

    public static void setPlaybackProgress(long position){
        mediaPlayerBinder.setPlaybackProgress(position);
    }

    @Override
    public void onBackPressedSupport() {
        //super.onBackPressedSupport();
        if (getSupportFragmentManager().getBackStackEntryCount() > 1){
            super.onBackPressedSupport();
        }else {
            Timer tExit = null;
            if (isExit) {
                isExit = false;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            } else {
                isExit = true;
                Toast.makeText(MainActivity.this, "再按一次返回桌面", Toast.LENGTH_SHORT).show();
                tExit = new Timer();
                tExit.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isExit = false;//取消退出
                    }
                }, 2000);// 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务
            }
        }
    }
}
