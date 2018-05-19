package com.example.shivi.viewtest;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class Main3Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ListView listView;
    private ListView secondListView;
    private ListView thirdListView;
    private int selectednavigationitem;

    private ArrayList<Long> listitem_id = new ArrayList<>();
    private ArrayList<Long> secondlistitemid = new ArrayList<>();
    private ArrayList<Long> thirdlistitemid = new ArrayList<>();

    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList<String> secondArrayList = new ArrayList<>();
    private ArrayList<String> thirdArrayList = new ArrayList<>();


    private MediaPlayer mMediaPlayer;

    private ArrayAdapter<String> arrayAdapter;
    private ArrayAdapter<String> secondArrayAdapter;
    private ArrayAdapter<String> thirdArrayAdapter;

    private String list_type="";
    private ArrayList<String> parent_id;
    private ArrayList<String> parent_path = new ArrayList<>();
    private HashMap<String,Long> map = new HashMap<>();

    public static String LocalBroadcast_Play_new_Audio = "com.example.shivi.viewtest.PLAY_NEW_AUDIO";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Window window = getWindow();
        window.setBackgroundDrawableResource(R.drawable.bg_nav_activity);


        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/


        secondListView = (ListView) findViewById(R.id.secondlistview);
        thirdListView = (ListView) findViewById(R.id.thirdlistview);

        listView = (ListView) findViewById(R.id.listview);
        arrayList.add("Shivam");
        arrayList.add("Satyam");
        arrayList.add("Mommy");
        arrayList.add("Papa");


        secondArrayAdapter = new ArrayAdapter<String>(getApplication(),android.R.layout.simple_list_item_1,secondArrayList);
        secondListView.setAdapter(secondArrayAdapter);

        thirdArrayAdapter = new ArrayAdapter<String>(getApplication(),android.R.layout.simple_list_item_1,thirdArrayList);
        thirdListView.setAdapter(thirdArrayAdapter);

        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,arrayList);
        listView.setAdapter(arrayAdapter);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        selectednavigationitem = R.id.nav_song;
        navigationView.setCheckedItem(selectednavigationitem);
        onNavigationItemSelected(navigationView.getMenu().findItem(selectednavigationitem));


        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                checkfolder();
            }
        });



        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (list_type.equals("Media")){

                    playnewmedia(listitem_id,position);
                }
                else if (list_type.equals("Folders")){
                    Intent intent = new Intent(Main3Activity.this,Main2Activity.class);
                    intent.putExtra("map",map);
                    intent.putExtra("name",arrayList.get(position));
                    intent.putExtra("path",parent_path.get(position));

                    startActivity(intent);
                }
                else {

                    listView.setVisibility(View.INVISIBLE);
                    preparesecondlist(arrayList.get(position),list_type,listitem_id.get(position));
                    secondListView.setVisibility(View.VISIBLE);

                    Log.i("Info","Id pressed : " + listitem_id.get(position).toString());
                }
            }
        });



        secondListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if ( list_type.equals("Artists") ) {

                secondListView.setVisibility(View.INVISIBLE);
                preparethirdlist(secondArrayList.get(position),secondlistitemid.get(position));
                thirdListView.setVisibility(View.VISIBLE);


                    Log.i("Info","Id pressed : " + secondlistitemid.get(position).toString());
                }

                else {
                    Log.i("media","trying to send media");
                    playnewmedia(secondlistitemid,position);
                }

            }
        });


        thirdListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                /*long song_id = thirdlistitemid.get(position);

                Uri contentUri = ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song_id);

                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    mMediaPlayer.setDataSource(getApplicationContext(), contentUri);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/


                Log.i("media","trying to send media");
                playnewmedia(thirdlistitemid,position);

            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            if ( thirdListView.getVisibility() == View.VISIBLE ){
                thirdListView.setVisibility(View.INVISIBLE);
                thirdArrayAdapter.clear();
                secondListView.setVisibility(View.VISIBLE);
            }
            else if ( secondListView.getVisibility() == View.VISIBLE ){
                secondListView.setVisibility(View.INVISIBLE);
                secondArrayAdapter.clear();
                listView.setVisibility(View.VISIBLE);
            }
            else
                super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main3, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.

        String query = null;

        /*else if ( listView.getVisibility() == View.VISIBLE ){
            arrayAdapter.notifyDataSetInvalidated();
        }*/

        int id = item.getItemId();

        if (id == R.id.nav_artist) {
            list_type = "Artists";
            // Handle the camera action
        } else if (id == R.id.nav_album) {
            list_type = "Albums";

        } else if (id == R.id.nav_song) {
            list_type = "Media";
            query = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        } else if (id == R.id.nav_genre) {
            list_type = "Genres";

        } else if (id == R.id.nav_playlist) {
            list_type = "Playlists";

        }else if ( id == R.id.nav_folder){
            list_type = "Folders";

        } else if (id == R.id.nav_settings) {
            Toast.makeText(getApplicationContext(),"Settings",Toast.LENGTH_LONG).show();

        } else if (id == R.id.nav_exit){
            Toast.makeText(getApplicationContext(),"Exit",Toast.LENGTH_LONG).show();
        }
        preparelist(list_type,query);
        //Toast.makeText(getApplicationContext(),listtype,Toast.LENGTH_LONG).show();

        if (secondListView.getVisibility() == View.VISIBLE || thirdListView.getVisibility() == View.VISIBLE){


            listView.setVisibility(View.VISIBLE);
            listView.setSelection(0);
            secondListView.setVisibility(View.INVISIBLE);
            thirdListView.setVisibility(View.INVISIBLE);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    public void preparethirdlist(String selectedItemName,long itemselected_id){

        Log.i("Info","third method called!!!");
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection_query = MediaStore.Audio.Media.IS_MUSIC + "!=0" + " and album_id = " + itemselected_id;
        String sortorder = "track";
        String name = "title";
        Cursor cursor = contentResolver.query(uri,null,selection_query,null,sortorder);

        if ( cursor != null && cursor.getCount() > 0 ){
            thirdArrayList.clear();
            thirdlistitemid.clear();
            while ( cursor.moveToNext() ){
                thirdArrayList.add(cursor.getString(cursor.getColumnIndex(name)));
                thirdlistitemid.add(cursor.getLong(cursor.getColumnIndex("_id")));
            }
        }

        thirdArrayAdapter.notifyDataSetInvalidated();
        cursor.close();
    }


    public void preparesecondlist(String selectedItemName,String selectedlistType,long itemselected_id){

        ContentResolver contentResolver = getContentResolver();
        String uri_string = "android.provider.MediaStore$Audio$";
        String query = null;
        Uri uri;
        String sortOrder = null;
        Cursor cursor = null;
        Class class1;
        String name = "";
        String id = "_id";
        String nextlisttype = null;


        if (selectedlistType.equals("Albums")){

            uri_string = "android.provider.MediaStore$Audio$Media";
            sortOrder = "track";
            name = "title";
            query = MediaStore.Audio.Media.IS_MUSIC + "!= 0" + " and album_id = " + itemselected_id;;
            try {
                class1 = Class.forName(uri_string);
                Object obj = class1.newInstance();
                Log.i("Info",class1.getName());
                Method method = class1.getMethod("getContentUri",new Class[]{String.class});
                Log.i("Info",method.getName());
                uri = (Uri) method.invoke(obj,"external");
                Log.i("Info",uri.toString());

                cursor = contentResolver.query(uri,null,query,null,sortOrder);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        else {
            if (selectedlistType.equals("Artists")) {
                nextlisttype = "Albums";
                sortOrder = "album";
                name = sortOrder;
                //id = MediaStore.Audio.Artists.Albums.ALBUM_ID;
            } else if (selectedlistType.equals("Playlists") || selectedlistType.equals("Genres")) {
                nextlisttype = "Members";
                if (selectedlistType.equals("Genres")){
                    sortOrder = "title";
                }
                name = "title";
                id = MediaStore.Audio.Playlists.Members.AUDIO_ID;

            }
            uri_string = uri_string + selectedlistType + "$" + nextlisttype;

            try {
                class1 = Class.forName(uri_string);
                Object object = class1.newInstance();
                Log.i("Info",class1.getName());
                Method method = class1.getMethod("getContentUri",new Class[]{String.class,long.class});
                Log.i("Info",method.getName());
                uri = (Uri) method.invoke(object,new Object[]{"external",itemselected_id});
                Log.i("Info",uri.toString());
                cursor = contentResolver.query(uri,null,null,null,sortOrder);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if ( cursor != null && cursor.getCount() > 0 ){

            secondArrayList.clear();
            secondlistitemid.clear();
            while ( cursor.moveToNext()){
                secondArrayList.add(cursor.getString(cursor.getColumnIndex(name)));
                secondlistitemid.add(cursor.getLong(cursor.getColumnIndex(id)));
            }

            secondArrayAdapter.notifyDataSetInvalidated();

            cursor.close();
        }

    }



    public void preparelist(final String listtype, String selection_query){
        if (listtype.equals("Folders")){

            arrayList.clear();
            arrayList.addAll(parent_id);
            Log.i("parent",arrayList.toString());
            arrayAdapter.notifyDataSetInvalidated();
        }
        else {

            ContentResolver contentResolver = getContentResolver();
            Uri uri;
            Cursor cursor;
            String sortOrder = null;
            String id = "_id";
            String mainString;

            if (listtype.equals("Artists") || listtype.equals("Albums")) {

                StringBuilder stringBuilder = new StringBuilder(listtype);
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                mainString = String.valueOf(stringBuilder).toLowerCase();
                if (listtype.equals("Albums")) {
                    sortOrder = "album";
                }
            } else if (listtype.equals("Genres") || listtype.equals("Playlists")) {

                mainString = "name";
            } else {
                mainString = "title";
                sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            }

            String uri_s = "android.provider.MediaStore$Audio$" + listtype;


            Class class1;
            try {
                class1 = Class.forName(uri_s);
                Object obj = class1.newInstance();
                Log.i("Info", class1.getName());
                Method method = class1.getMethod("getContentUri", new Class[]{String.class});
                Log.i("Info", method.getName());
                uri = (Uri) method.invoke(obj, "external");
                Log.i("Info", uri.toString());
                cursor = contentResolver.query(uri, null, selection_query, null, sortOrder);


                if (cursor != null && cursor.getCount() > 0) {

                    //Toast.makeText(getApplication(),String.valueOf(cursor.getCount()),Toast.LENGTH_LONG).show();
                    arrayList.clear();
                    listitem_id.clear();
                    while (cursor.moveToNext()) {
                        arrayList.add(cursor.getString(cursor.getColumnIndex(mainString)));
                        listitem_id.add(cursor.getLong(cursor.getColumnIndex(id)));
                    }


                }


                arrayAdapter.notifyDataSetInvalidated();
                cursor.close();

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }


    }


    public void playnewmedia(ArrayList<Long> mediaList,int position){

        Intent intent = new Intent(LocalBroadcast_Play_new_Audio);
        Bundle bundle = new Bundle();
        bundle.putSerializable("medialist",mediaList);
        bundle.putInt("position",position);
        intent.putExtra("Bundle",bundle);

        LocalBroadcastManager.getInstance(Main3Activity.this).sendBroadcast(intent);
        Log.i("media","media sent");
        finish();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectednavigationitem = savedInstanceState.getInt("selected navigation item");
        listView.onRestoreInstanceState(savedInstanceState.getParcelable("listview.state"));
        secondListView.onRestoreInstanceState(savedInstanceState.getParcelable("secondlistview.state"));
        thirdListView.onRestoreInstanceState(savedInstanceState.getParcelable("thirdlistview.state"));
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected navigation item",selectednavigationitem);
        outState.putParcelable("listview.state",listView.onSaveInstanceState());
        outState.putParcelable("secondlistview.state",secondListView.onSaveInstanceState());
        outState.putParcelable("thirdlistview.state",thirdListView.onSaveInstanceState());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }



    private void folderpath(String path,long id){
        if (id == 0){
            if (!parent_id.contains(path.substring(path.lastIndexOf("/") + 1,path.length()))) {
                parent_id.add(path.substring(path.lastIndexOf("/") + 1, path.length()));
                parent_path.add(path);
            }
        }
        else {

            map.put(path,id);
            //Log.i("caleed", "true");
            ContentResolver contentResolver = getContentResolver();
            Uri uri = MediaStore.Files.getContentUri("external");

            String selection = MediaStore.Files.FileColumns._ID + " = " + id;
            Cursor cursor = contentResolver.query(uri, null, selection, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                //Log.i("caleed", "true");
                while (cursor.moveToNext()) {
                    Log.i("found", cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    long i = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT));

                    if (!map.containsValue(i)) {
                        String s = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                        //map.put(i,s);
                        if (s.contains("/")) {
                            s = s.substring(0, s.lastIndexOf("/"));
                            //Log.i("key", s + " :" + i);
                            folderpath(s, i);


                        }

                    }
                }
                cursor.close();

            }
        }



    }


    private void checkfolder(){


        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO + ") GROUP BY (" + MediaStore.Files.FileColumns.PARENT;
        Cursor cursor = contentResolver.query(uri, null, selection, null, MediaStore.Files.FileColumns.DATA + " ASC");
        if ( cursor != null && cursor.getCount() > 0 ){
            //list = new ArrayList<>();
            parent_id = new ArrayList<>();
            //id = new ArrayList<>();

            while (cursor.moveToNext()){
                String s = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                if(s.contains("/")) {
                    s = s.substring(0, s.lastIndexOf("/"));
                }
                //list.add(s);
                long item_id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.PARENT));
                //id.add(item_id);
                //Log.i("folders",s + item_id);
                //map.put(item_id,s);
                //s = s.substring(0,s.lastIndexOf("/"));
                //Log.i("name",s);
                if (s.contains("/")) {
                    folderpath(s, item_id);
                    //Log.i("putting",s.substring(0,s.lastIndexOf("/")));
                    //s = s.substring(0,s.lastIndexOf("/"));
                    //Log.i("s",s);
                }
                //break;
            }
            //Storage//
            cursor.close();

        }
        //Log.i("tttttttt",map.toString());

        //map.put((long) 0,"SSSS");
        Log.i("id", parent_id.toString());
    }
}
