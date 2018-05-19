package com.example.shivi.viewtest;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;

public class Main2Activity extends AppCompatActivity {

    private ListView folderlistview;
    private Deque<ArrayList<String>> stack;
    private Deque<ArrayList<Long>> stack_ID;
    private ArrayList<String> pushnames;
    private ArrayList<Long> pushids;
    private ArrayAdapter currentadapter;
    private ArrayList<String> display_name = new ArrayList<>();
    private ArrayList<Long> id_list = new ArrayList<>();
    private HashMap<String,Long> map;
    private int start = 0;
    private Main3Activity main3Activity = new Main3Activity();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        folderlistview = findViewById(R.id.folder_list);
        //currentadapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,display_name);

        stack = new ArrayDeque<>();
        stack_ID = new ArrayDeque<>();

        map = (HashMap<String, Long>) getIntent().getSerializableExtra("map");
        currentadapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,display_name);
        files(getIntent().getStringExtra("path"));




        folderlistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {



                Log.i("item click",String.valueOf(start) + "   " +String.valueOf(position));
                if (display_name.get(position).contains(".")){
                    main3Activity.playnewmedia(new ArrayList<Long>( id_list.subList(start,id_list.size())),position - start);

                }
                else {
                    files(id_list.get(position));
                }
            }
        });


        //folderlistview.setAdapter(currentadapter);
    }

    @Override
    public void onBackPressed() {
        if (!stack.isEmpty()){


            Log.i("stack",stack.toString());
            display_name = stack.peekFirst();
            Log.i("peek",stack.peek().toString());
            id_list = stack_ID.peekFirst();
            stack.removeFirst();
            stack_ID.removeFirst();

            currentadapter.notifyDataSetChanged();
        }
        else {
            super.onBackPressed();
        }

    }


    private void files(String path){
        int l = path.length();
        Log.i("caleed", "true");
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        boolean musicfound = false;

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO  + " AND " + MediaStore.Files.FileColumns.DATA + " like '" + path + "%'" + ") GROUP BY (" + MediaStore.Files.FileColumns.PARENT;
        Cursor cursor = contentResolver.query(uri,null,selection,null,MediaStore.Files.FileColumns.MEDIA_TYPE + " ASC, " + MediaStore.Files.FileColumns.DATA + " ASC");

        if (cursor != null && cursor.getCount() > 0){
            display_name = new ArrayList<>();
            id_list = new ArrayList<>();
            while (cursor.moveToNext()){
                String s = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                //Log.i("info",s.substring(0,s.indexOf('/',path.length() + 1)));
                if (map.containsKey(s.substring(0,s.indexOf("/",path.length() + 1)))){

                    if (!display_name.contains(s.substring(l + 1,s.indexOf("/",l + 1)))){

                        display_name.add(s.substring(l + 1,s.indexOf("/",l + 1)));
                        id_list.add(map.get(s.substring(0,s.indexOf("/",l + 1))));
                        Log.i("search",s.substring(l + 1,s.indexOf("/",l + 1)));
                    }

                }
                else if (cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)) == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) {
                    if (!musicfound){
                        if (!display_name.isEmpty()) {
                            start = display_name.size() - 1;
                        }
                        musicfound = true;
                    }
                    display_name.add(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)));
                    id_list.add(map.get(s.substring(0,s.indexOf("/",l + 1))));
                }

            }
            cursor.close();
            currentadapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,display_name);
            folderlistview.setAdapter(currentadapter);
            pushids = new ArrayList<>();
            pushids = id_list;
            pushnames = new ArrayList<>();
            pushnames = display_name;
            stack.addFirst(pushnames);
            stack_ID.addFirst(pushids);
            Log.i("satck formed",stack.toString());
        }

    }


    private void files(long i){

        Uri uri = MediaStore.Files.getContentUri("external");
        ContentResolver contentResolver = getContentResolver();
        boolean musicfound = false;

        String selection = MediaStore.Files.FileColumns.PARENT + " = " + i;// + " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
        Cursor cursor = contentResolver.query(uri,null,selection,null,MediaStore.Files.FileColumns.MEDIA_TYPE + " ASC, " + MediaStore.Files.FileColumns.DATA + " ASC");
        if (cursor != null && cursor.getCount() > 0){
            display_name = new ArrayList<>();
            id_list = new ArrayList<>();

            while (cursor.moveToNext()){
                Log.i("nexxxxxt",cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                switch (cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))){
                    case MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO:
                        if (!musicfound){
                            if (!display_name.isEmpty()) {
                                start = display_name.size() - 1;
                            }
                            musicfound = true;
                        }
                        //Log.i("music",cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                        display_name.add(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)));
                        id_list.add(cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)));
                        break;
                    case MediaStore.Files.FileColumns.MEDIA_TYPE_NONE:
                        if (map.containsValue(cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)))){

                            String s = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                            s = s.substring(s.lastIndexOf("/") + 1,s.length());
                            //Log.i("music : folder",cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                            display_name.add(s);
                            id_list.add(cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)));
                        }
                        break;
                }

            }
            cursor.close();
            currentadapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,display_name);
            //currentadapter.notifyDataSetChanged();
            pushids = new ArrayList<>();
            pushids = id_list;
            pushnames = new ArrayList<>();
            pushnames = display_name;
            stack.addFirst(pushnames);
            stack_ID.addFirst(pushids);
            Log.i("satck formed",stack.toString());
            folderlistview.setAdapter(currentadapter);
        }
    }
}
