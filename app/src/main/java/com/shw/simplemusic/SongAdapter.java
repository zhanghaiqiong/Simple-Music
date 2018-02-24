package com.shw.simplemusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;

import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by 19783 on 2018/2/8.
 */

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    private Context mContext;
    private List<Song> mSongList;

    class ViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout relativeLayout;
        TextView songName;
        TextView singerName;
        TextView tools_more;

        public ViewHolder(View itemView) {
            super(itemView);
            relativeLayout=(RelativeLayout) itemView;
            songName=itemView.findViewById(R.id.item_song_textView_songName);
            singerName=itemView.findViewById(R.id.item_song_textView_singerName);
            tools_more=itemView.findViewById(R.id.item_song_tools_more);
        }
    }

    public SongAdapter(List<Song> songList){
        this.mSongList=songList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext==null){
            mContext=parent.getContext();
        }
        View view= LayoutInflater.from(mContext).inflate(R.layout.item_song,parent,false);
        final ViewHolder holder=new ViewHolder(view);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Song song=mSongList.get(holder.getAdapterPosition());
                MainActivity.playNewMusic(mSongList,holder.getAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Song song=mSongList.get(holder.getAdapterPosition());
                if (song.getSongType()==Song.SONG_TYPE_LOCAL){
                    createLocalMusicPopupMenu(holder.tools_more,mSongList,holder.getAdapterPosition());
                }else {
                    createOnlineMusicPopupMenu(holder.tools_more,mSongList,holder.getAdapterPosition());
                }
                return true;
            }
        });
        holder.tools_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Song song=mSongList.get(holder.getAdapterPosition());
                if (song.getSongType()==Song.SONG_TYPE_LOCAL){
                    createLocalMusicPopupMenu(holder.tools_more,mSongList,holder.getAdapterPosition());
                }else {
                    createOnlineMusicPopupMenu(holder.tools_more,mSongList,holder.getAdapterPosition());
                }
            }
        });
        return holder;
    }

    public void createLocalMusicPopupMenu(final View v, final List<Song> songs, final int position){
        PopupMenu popupMenu=new PopupMenu(mContext,v);
        Menu menu=popupMenu.getMenu();
        MenuInflater inflater=popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_song_local,menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.menu_song_local_play:
                        MainActivity.playNewMusic(songs, position);
                        break;
                    case R.id.menu_song_local_collection:

                        break;
                    case R.id.menu_song_local_delete:
                        File file=new File(songs.get(position).getFileHash());
                        if (file.canWrite()){
                            boolean isDelete=file.delete();
                            LocalMusicFragment.getAllMusic();
                            LocalMusicFragment.initRecyclerView();
                            Log.d("shw","delete file return is:"+isDelete);
                        }
                        break;
                    case R.id.menu_song_local_share:

                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    public void createOnlineMusicPopupMenu(final View v, final List<Song> songs, final int position){
        PopupMenu popupMenu=new PopupMenu(mContext,v);
        Menu menu=popupMenu.getMenu();
        MenuInflater inflater=popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_song_online,menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.menu_song_online_play:
                        MainActivity.playNewMusic(songs, position);
                        break;
                    case R.id.menu_song_online_collection:

                        break;
                    case R.id.menu_song_online_download:
                        AsyncTask asyncTask=new DownloadMusicTask(songs.get(position));
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                            asyncTask.execute();
                        } else {
                            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                        break;
                    case R.id.menu_song_online_info:
                        JSONObject songInfo=songs.get(position).getSongInfo();
                        Iterator<String> keys=songInfo.keys();
                        String infoToShow="";
                        while (keys.hasNext()){
                            try {
                                String key=keys.next();
                                String value = songInfo.getString(key);
                                infoToShow=infoToShow+key+" : "+value+"\n";
                                //Log.d("shw",key+" : "+value);
                            }catch (Exception e){
                                Log.d("shw","error on song online info:"+e.toString());
                            }
                        }
                        //显示歌曲详细信息dialog
                        AlertDialog.Builder builder=new AlertDialog.Builder(mContext)
                                .setTitle("信息流")
                                .setMessage(infoToShow)
                                .setCancelable(true)
                                .setPositiveButton("无脑流", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        AlertDialog.Builder builder1=new AlertDialog.Builder(mContext)
                                                .setTitle("无脑流")
                                                .setMessage("无脑流？不存在的老铁！\n信息量太大啦要我逐个翻译嘛？\n翻译是不可能翻译的，这辈子都不可能翻译的，英文又看不懂，就是只能靠假装看得懂英文将就将就的来维持一下生活这样子")
                                                .setCancelable(true);
                                        builder1.show();
                                    }
                                })
                                .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                        builder.show();
                        break;
                    case R.id.menu_song_online_share:

                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    class DownloadMusicTask extends AsyncTask {

        private Song song;
        private String songUrl;

        public DownloadMusicTask(Song song) {
            this.song = song;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=" + song.getFileHash() + "&from=mkugou")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                JSONObject jsonObject = new JSONObject(responseData);
                songUrl = jsonObject.getString("url");
            } catch (Exception e) {
                Log.d("shw", "error on play online music:" + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            try {
                FileDownloader.setup(mContext);
                FileDownloader.getImpl()
                        .create(songUrl)
                        .setPath("/sdcard/kgmusic/download/"+song.getFileName().replace("<em>","").replace("</em>","")+".mp3")
                        .setListener(new FileDownloadLargeFileListener() {
                            @Override
                            protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                                Log.d("shw","download is pending");
                            }

                            @Override
                            protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                                Log.d("shw","download is pregross");
                            }

                            @Override
                            protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                                Log.d("shw","download is pause");
                            }

                            @Override
                            protected void completed(BaseDownloadTask task) {
                                Log.d("shw","download is completed");
                                LocalMusicFragment.getAllMusic();
                                LocalMusicFragment.initRecyclerView();
                            }

                            @Override
                            protected void error(BaseDownloadTask task, Throwable e) {
                                Log.d("shw","download is error");
                            }

                            @Override
                            protected void warn(BaseDownloadTask task) {
                                Log.d("shw","download is warn");
                            }
                        }).start();
            } catch (Exception e) {
                Log.d("shw", "error on play online music 2 :" + e.toString());
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song=mSongList.get(position);
        holder.songName.setText(song.getSongName());
        holder.singerName.setText(song.getSingerName());
        Typeface iconfont=Typeface.createFromAsset(mContext.getAssets(),"iconfont/iconfont.ttf");
        holder.tools_more.setTypeface(iconfont);
    }

    @Override
    public int getItemCount() {
        return mSongList.size();
    }
}
