package com.shw.simplemusic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MediaPlayerService extends Service {

    private String lyricsDir = Environment.getExternalStorageDirectory().getPath()+"/kgmusic/download/lyrics";

    private MediaPlayer mediaPlayer;
    private MediaPlayerBinder mediaPlayerBinder = new MediaPlayerBinder();
    private List<Song> songList;
    private int playingPosition;
    MediaPlayerBinder playerBinder;

    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    public MediaPlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playerBinder = new MediaPlayerBinder();
        //广播实例
        localBroadcastManager = LocalBroadcastManager.getInstance(getBaseContext());
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.shw.simplemusic.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter);
        //创建播放器实例
        newMediaPlayer();
        //通知栏
        initNotification();
        //开启更新进度条任务
        AsyncTask asyncTask = new UpdateMisicPlayProgressTask();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
            asyncTask.execute();
        } else {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void newMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                MainActivity.progressBar.setSecondaryProgress(mediaPlayer.getDuration() / 100 * i);
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //mediaPlayer.reset();
                localBroadcastManager.sendBroadcast(getIntent(false));
                playerBinder.playNewSong(songList, playingPosition + 1);
                //MainActivity.progressBar.setMax(0);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                mediaPlayer.reset();
                localBroadcastManager.sendBroadcast(getIntent(false));
                MainActivity.progressBar.setMax(0);
                return true;
            }
        });
    }

    //更新进度条
    class UpdateMisicPlayProgressTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            while (true) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        publishProgress(objects);
                    } else {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d("shw", "error on sleep" + e.toString());
                }
            }
            //return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            try {
                MainActivity.progressBar.setMax(mediaPlayer.getDuration());
                MainActivity.progressBar.setProgress(mediaPlayer.getCurrentPosition());
                if (LyricsActivity.lyricView!=null){
                    LyricsActivity.lyricView.updateTime(mediaPlayer.getCurrentPosition());
                }
                //MainActivity.progressBar.setSecondaryProgress(bufferingProgress);
            } catch (Exception e) {
                Log.d("shw", "更新进度条错误");
                MainActivity.progressBar.setMax(0);
                MainActivity.progressBar.setProgress(0);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String instruction = intent.getStringExtra("data");
        if (instruction.equals("skip_previous")) {
            playerBinder.previousPiece();
        } else if (instruction.equals("play")) {
            if (mediaPlayerBinder.isPlaying()) {
                playerBinder.pausePlayback();
            } else {
                playerBinder.startPlaying();
            }
        } else if (instruction.equals("skip_next")) {
            playerBinder.nextTrack();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    //初始化通知栏
    private void initNotification() {
        Notification notification = getNotification("Simple Music", "歌也不比酷狗少噢", R.drawable.ic_notification_play);
        startForeground(1, notification);
    }

    //获取通知栏管理器
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    //创建通知栏实例
    private Notification getNotification(String songName, String singerName, int playButtonSrc) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent intent_skipPrevious = PendingIntent.getService(this, 1, new Intent(this, MediaPlayerService.class).putExtra("data", "skip_previous"), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent intent_play = PendingIntent.getService(this, 2, new Intent(this, MediaPlayerService.class).putExtra("data", "play"), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent intent_skipNext = PendingIntent.getService(this, 3, new Intent(this, MediaPlayerService.class).putExtra("data", "skip_next"), PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_music);
        remoteViews.setTextViewText(R.id.notification_text_songName, songName);
        remoteViews.setTextViewText(R.id.notification_text_singerName, singerName);
        remoteViews.setImageViewBitmap(R.id.notification_play, BitmapFactory.decodeResource(getResources(), playButtonSrc));
        remoteViews.setOnClickPendingIntent(R.id.notification_skipPrevious, intent_skipPrevious);
        remoteViews.setOnClickPendingIntent(R.id.notification_play, intent_play);
        remoteViews.setOnClickPendingIntent(R.id.notification_skipNext, intent_skipNext);

        Notification notification = new NotificationCompat.Builder(this)
                .setContent(remoteViews)
                .setSmallIcon(R.drawable.ic_action_music)
                .setContentIntent(pendingIntent)
                .build();
        return notification;
    }

    //创建广播实例
    private Intent getIntent(Boolean isPlaying) {
        Intent intent = new Intent("com.shw.simplemusic.LOCAL_BROADCAST");
        intent.putExtra("songName", songList.get(playingPosition).getSongName());
        intent.putExtra("singerName", songList.get(playingPosition).getSingerName());
        intent.putExtra("isPlaying", isPlaying);
        return intent;
    }

    //收到广播
    class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayerBinder.isPlaying()) {
                getNotificationManager().notify(1, getNotification(intent.getStringExtra("songName"), intent.getStringExtra("singerName"), R.drawable.ic_notification_pause));
            } else {
                getNotificationManager().notify(1, getNotification(intent.getStringExtra("songName"), intent.getStringExtra("singerName"), R.drawable.ic_notification_play));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }

    class MediaPlayerBinder extends Binder {

        public void playNewSong(List<Song> songs, int position) {
            songList = songs;
            playingPosition = position;
            if (playingPosition > songs.size() - 1) {
                playingPosition = 0;
            }
            if (playingPosition < 0) {
                playingPosition = songs.size() - 1;
            }
            Song song = songList.get(playingPosition);
            try {
                mediaPlayer.release();
                MainActivity.progressBar.setMax(0);
                MainActivity.progressBar.setProgress(0);
                MainActivity.progressBar.setSecondaryProgress(0);
                if (song.getSongType() == Song.SONG_TYPE_LOCAL) {
                    try {
                        mediaPlayer.isPlaying();
                    } catch (IllegalStateException e) {
                        mediaPlayer = null;
                        newMediaPlayer();
                    }
                    mediaPlayer.setDataSource(song.getFileHash());
                    mediaPlayer.prepare();
                    startPlaying();
                } else {
                    AsyncTask asyncTask = new PlayOnlineMusicTask(song);
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        asyncTask.execute();
                    } else {
                        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
                String lyricsPath = lyricsDir + "/" + MainActivity.getPlayingSong().getFileName() + ".lrc";
                if (LyricsActivity.lyricView!=null) {
                    LyricsActivity.lyricView.loadLrc(new File(lyricsPath));
                }
            } catch (Exception e) {
                Log.d("shw", "error on play new song:" + e.toString());
            }
        }

        //播放在线音乐
        class PlayOnlineMusicTask extends AsyncTask {

            private Song song;
            private String songUrl;
            private String lyrics;

            public PlayOnlineMusicTask(Song song) {
                this.song = song;
            }

            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            //.url("http://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=" + song.getFileHash() + "&from=mkugou")
                            .url("http://www.kugou.com/yy/index.php?r=play/getdata&hash=" + song.getFileHash() + "&album_id=" + song.getSongInfo().getString("AlbumID"))
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData).getJSONObject("data");
                    songUrl = jsonObject.getString("play_url");
                    lyrics = jsonObject.getString("lyrics");
                } catch (Exception e) {
                    Log.d("shw", "error on play online music:" + e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                try {
                    //歌词
                    String lyricsPath = lyricsDir + "/" + song.getFileName() + ".lrc";
                    Log.d("shw",lyricsPath);
                    File lrc = new File(lyricsPath);
                    if (!lrc.exists()) {
                        File file = new File(lyricsDir);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        lrc.createNewFile();
                    }
                    try {
                        BufferedWriter bfw = new BufferedWriter(new FileWriter(lrc, false));
                        bfw.write(lyrics);
                        bfw.newLine();
                        bfw.flush();
                        bfw.close();
                    } catch (IOException e) {
                        Log.d("shw","创建文件错误");
                    }

                    try {
                        mediaPlayer.isPlaying();
                    } catch (IllegalStateException e) {
                        mediaPlayer = null;
                        newMediaPlayer();
                    }
                    mediaPlayer.setDataSource(songUrl);
                    mediaPlayer.prepare();
                    startPlaying();
                    //开启更新进度条任务
                } catch (Exception e) {
                    Log.d("shw", "error on play online music 2 :" + e.toString());
                }
            }
        }

        public void startPlaying() {
            if (songList != null) {
                mediaPlayer.start();
                if (mediaPlayer.isPlaying()) {
                    localBroadcastManager.sendBroadcast(getIntent(true));
                }
            }
        }

        public void pausePlayback() {
            mediaPlayer.pause();
            localBroadcastManager.sendBroadcast(getIntent(false));
        }

        public void nextTrack() {
            if (songList != null) {
                this.playNewSong(songList, playingPosition + 1);
            }
        }

        public void previousPiece() {
            if (songList != null) {
                this.playNewSong(songList, playingPosition - 1);
            }
        }

        public Song getPlayingSong() {
            if (songList != null) {
                Song song = songList.get(playingPosition);
                return song;
            } else {
                return null;
            }
        }

        //播放器是否正在播放
        public boolean isPlaying() {
            try {
                return mediaPlayer.isPlaying();
            } catch (Exception e) {
                return false;
            }
        }

        //获取当前播放的音乐的长度
        public int getPlayingLength() {
            return mediaPlayer.getDuration();
        }

        //获取播放进度
        public int getPlaybackProgress() {
            return mediaPlayer.getCurrentPosition();
        }

        public void setPlaybackProgress(long posotion){
            mediaPlayer.seekTo((int)posotion);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mediaPlayerBinder;
    }
}
