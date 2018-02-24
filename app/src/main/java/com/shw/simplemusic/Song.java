package com.shw.simplemusic;

import android.content.Intent;
import android.text.Html;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.security.PublicKey;

/**
 * Created by 19783 on 2018/2/8.
 */

public class Song {

    private int songType;
    public static int SONG_TYPE_LOCAL = 0;
    public static int SONG_TYPE_ONLINE = 1;

    //本地音乐独有变量
    private File songFile;
    //在线音乐独有变量
    private JSONObject songInfo;

    private String fileName;
    private String fileHash;
    private String fileSize;

    private String singerName;
    private String songName;

    public Song(File songFile){
        this.songFile=songFile;
        this.songType=SONG_TYPE_LOCAL;
        this.fileName=songFile.getName();
        this.fileHash=songFile.getPath();
        this.fileSize=String.valueOf(songFile.getTotalSpace());

        this.singerName = fileName.split(" - ")[0].replace("<em>", "").replace("</em>", "");
        String andSuffix = fileName.split(" - ")[1];
        this.songName = andSuffix.split("\\.")[0].replace("<em>", "").replace("</em>", "");
    }

    public Song(JSONObject songInfo) {
        try {
            this.songInfo = songInfo;
            this.songType = SONG_TYPE_ONLINE;
            this.fileName = songInfo.getString("FileName");
            this.fileHash = songInfo.getString("FileHash");
            this.fileSize = songInfo.getString("FileSize");

            this.singerName = fileName.split(" - ")[0].replace("<em>", "").replace("</em>", "");
            this.songName = fileName.split(" - ")[1].replace("<em>", "").replace("</em>", "");
        } catch (Exception e) {
            Log.d("shw", e.toString());
        }
    }

    public JSONObject getSongInfo() {
        return songInfo;
    }

    public void setSongInfo(JSONObject songInfo) {
        this.songInfo = songInfo;
    }

    public int getSongType() {
        return songType;
    }

    public void setSongType(int songType) {
        this.songType = songType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getSingerName() {
        return singerName;
    }

    public void setSingerName(String singerName) {
        this.singerName = singerName;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }
}
