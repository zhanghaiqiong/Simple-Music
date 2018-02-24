package com.shw.simplemusic;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import me.yokeyword.fragmentation.ISupportFragment;
import me.yokeyword.fragmentation_swipeback.SwipeBackFragment;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by 19783 on 2018/2/12.
 */

public class OnlineMusicFragment extends SwipeBackFragment implements ISupportFragment {

    private View view;
    private Context context;

    private AVLoadingIndicatorView loadingIndicatorView;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private List<Song> songList=new ArrayList<>();

    private TextView btn_back;
    private EditText edt_searchText;
    private PopupMenu popupMenu;
    private Menu menu;
    private TextView btn_search;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.fragment_onlinemusic,container,false);
        context=getContext();
        initView();
        AsyncTask asyncTask=new SearchMusicTask("");
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
            asyncTask.execute();
        } else {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return attachToSwipeBack(view);
    }

    private void initView(){
        Typeface iconfont=Typeface.createFromAsset(context.getAssets(),"iconfont/iconfont.ttf");
        btn_back=view.findViewById(R.id.fragment_onlineMusic_appBar_back);
        btn_back.setTypeface(iconfont);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pop();
            }
        });
        edt_searchText=view.findViewById(R.id.fragment_onlineMusic_appBar_searchText);
        //自动联想菜单
        popupMenu=new PopupMenu(context,edt_searchText);
        menu=popupMenu.getMenu();
        //编辑框文本改变事件监听
        edt_searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(final Editable editable) {
                if (!editable.toString().equals("")) {

                }
            }
        });
        //编辑框回车事件监听
        edt_searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH||keyEvent.getKeyCode() == KeyEvent.ACTION_DOWN) {
                    // Do something
                    String keyword=edt_searchText.getText().toString();
                    AsyncTask asyncTask=new SearchMusicTask(keyword);
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        asyncTask.execute();
                    } else {
                        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    return true;
                }
                return false;
            }
        });
        btn_search=view.findViewById(R.id.fragment_onlineMusic_appBar_search);
        btn_search.setTypeface(iconfont);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String keyword=edt_searchText.getText().toString();
                AsyncTask asyncTask=new SearchMusicTask(keyword);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    asyncTask.execute();
                } else {
                    asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
        loadingIndicatorView=view.findViewById(R.id.fragment_onlineMusic_layout_loadingView);
        recyclerView=view.findViewById(R.id.fragment_onlineMusic_layout_recyclerView);
    }

    class SearchMusicTask extends AsyncTask {

        private String keyword;

        public SearchMusicTask(String keyword){
            this.keyword=keyword;
        }

        @Override
        protected void onPreExecute() {
            recyclerView.setVisibility(View.GONE);
            loadingIndicatorView.smoothToShow();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            searchMusic(keyword);
            return true;
        }

        @Override
        protected void onPostExecute(Object o) {
            loadingIndicatorView.smoothToHide();
            recyclerView.setVisibility(View.VISIBLE);
            initRecyclerView();
        }

        private void searchMusic(final String keyword) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://songsearch.kugou.com/song_search_v2?callback=jQuery11240885872284855965_1518081532102&keyword=" + URLEncoder.encode(keyword, "utf-8") + "&page=1&pagesize=30&userid=-1&clientver=&platform=WebFilter&tag=em&filter=2&iscorrection=1&privilege_filter=0&_=1518081532104")
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                String jsonData = responseData.split("\\(", 2)[1];
                jsonData = jsonData.substring(0, jsonData.length() - 2);
                showJsonToRecyclerView(jsonData);
            } catch (Exception e) {
                Log.d("shw", "error on search music:"+e.toString());
            }
        }

        private void showJsonToRecyclerView(String jsonData) {
            try {
                JSONObject jsonObject = new JSONObject(jsonData);
                JSONArray lists = jsonObject.getJSONObject("data").getJSONArray("lists");
                songList.clear();
                for (int i = 0; i < lists.length(); i++) {
                    JSONObject songInfo = lists.getJSONObject(i);
                    Song song = new Song(songInfo);
                    songList.add(song);
                }
            } catch (Exception e) {
                Log.d("shw", "error on show json to recyclerView"+e.toString());
            }
        }
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SongAdapter(songList);
        recyclerView.setAdapter(adapter);
    }
}
