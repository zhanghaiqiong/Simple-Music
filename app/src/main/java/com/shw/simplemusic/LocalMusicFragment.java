package com.shw.simplemusic;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.yokeyword.fragmentation.SupportFragment;

/**
 * Created by 19783 on 2018/2/12.
 */

public class LocalMusicFragment extends SupportFragment {

    private View view;
    private static Context context;

    private TextView btn_search;
    private static AVLoadingIndicatorView loadingIndicatorView;

    private static RecyclerView recyclerView;
    private static SongAdapter adapter;
    private static List<Song> songList=new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.fragment_localmusic,container,false);
        context=getContext();
        initView();
        //获取文件读取和写入的权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(),new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        getAllMusic();
        initRecyclerView();
        return view;
    }

    private void initView(){
        loadingIndicatorView=view.findViewById(R.id.fragment_localMusic_layout_loadingView);
        recyclerView=view.findViewById(R.id.fragment_localmusic_recyclerView);
        Typeface iconfont=Typeface.createFromAsset(context.getAssets(),"iconfont/iconfont.ttf");
        btn_search=view.findViewById(R.id.fragment_localMusic_appBar_search);
        btn_search.setTypeface(iconfont);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start(new OnlineMusicFragment());
            }
        });
    }

    public static void getAllMusic(){
        loadingIndicatorView.smoothToShow();
        File[] files=getAllDownloadMusic();
        songList.clear();
        for (File file:files){
            if (file.isFile()){
                songList.add(new Song(file));
            }
        }
        loadingIndicatorView.smoothToHide();
    }

    private static File[] getAllDownloadMusic(){
        File workDir=new File("/sdcard/kgmusic/download/");
        if (!workDir.exists()){
            workDir.mkdirs();
        }
        File[] files=workDir.listFiles();
        return files;
    }

    public static void initRecyclerView(){
        LinearLayoutManager layoutManager=new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        adapter=new SongAdapter(songList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getAllMusic();
        initRecyclerView();
    }
}
