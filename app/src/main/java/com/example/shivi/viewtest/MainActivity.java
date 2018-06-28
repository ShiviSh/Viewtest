package com.example.shivi.viewtest;

import android.*;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shivi.viewtest.Storage.Storage;
import com.savantech.seekarc.SeekArc;


import java.io.ByteArrayOutputStream;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


import static android.media.session.PlaybackState.STATE_PAUSED;
import static android.media.session.PlaybackState.STATE_PLAYING;

public class MainActivity extends AppCompatActivity  {

    private ImageButton nextButton;
    private ImageButton previousButton;
    private ImageButton playpauseButton;
    private ImageButton mediaselectionButton;
    private ImageButton QueueButton;
    private CircleImageView circleImageView;
    private TextView titletext;
    private TextView artisttext;
    private TextView albumtext;
    private ImageView albumartimageview;

    private Handler handler;
    private Runnable task;
    private byte[] bytes;
    private Bitmap savedbitmap;
    private String savedtitletext;
    private String savedartisttext;
    private String savedalbumtext;
    private boolean mIsTracking = false;

    private Storage storage;

    private boolean granted = true;

    private boolean seekallowed;
    private int storage_perm;
    private int phonestate_perm;
    private List<String> permissions_needed;


    private ArrayList<Long> medialist;

    ConstraintLayout constraintLayout;

    boolean serviceBound = false;

    private SeekArc circularSeekBar;

    private boolean servicestarted = false;
    private int currentState;

    private MediaControllerCompat mediaControllerCompat;

    private MediaMetadataCompat mediaMetadataCompat;
    private PlaybackStateCompat playbackStateCompat;

    private MediaBrowserCompat mediaBrowserCompat;

    private MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnectionFailed() {
            if (!servicestarted && !serviceBound){
                Intent servcieStartIntent = new Intent(getApplicationContext(),MediaPlaybackService.class);
                startService(servcieStartIntent);
                bindService(servcieStartIntent, serviceConnection,0);
                servicestarted = true;
                serviceBound = true;
                Log.i("Info","on create binding service");
            }

            mediaBrowserCompat.connect();

            Log.i("media browser","trying to reconnect");

            super.onConnectionFailed();
        }

        @Override
        public void onConnectionSuspended() {


            if (mediaBrowserCompat.isConnected()){
                mediaBrowserCompat.disconnect();
                mediaControllerCompat.unregisterCallback(mediaControllerCompatCallback);
                MediaPlaybackService mediaPlaybackService = new MediaPlaybackService();
                mediaPlaybackService.destructmethod();
            }

            super.onConnectionSuspended();
        }

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mediaControllerCompat = new MediaControllerCompat(MainActivity.this, mediaBrowserCompat.getSessionToken());
                mediaControllerCompat.registerCallback(mediaControllerCompatCallback);

                /*Intent intent = new Intent(Broadcast_refresh);
                intent.putExtra("service state",servicestarted);

                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                servicestarted = true;*/

                mediaMetadataCompat = mediaControllerCompat.getMetadata();
                playbackStateCompat = mediaControllerCompat.getPlaybackState();
                currentState = playbackStateCompat.getState();
                updateplaybutton(currentState);
                updateUI(mediaMetadataCompat);


            } catch( RemoteException e ) {
                e.printStackTrace();
            }
            Log.i("media connection","connection callback " + servicestarted);
        }
    };


    private MediaControllerCompat.Callback mediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            updateUI(metadata);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            super.onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            super.onShuffleModeChanged(shuffleMode);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            updateplaybutton(state.getState());

            Log.i("media control connection","playback state updated");
        }
    };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound " + serviceBound, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            Log.i("info","Service disconnected");

            serviceBound = false;

        }
    };


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        servicestarted = savedInstanceState.getBoolean("ServicerunningState");
        /*savedtitletext = savedInstanceState.getString("saved title");
        savedartisttext = savedInstanceState.getString("saved artist");
        savedalbumtext = savedInstanceState.getString("saved album");
        currentState = savedInstanceState.getInt("saved playback state");
        bytes = Base64.decode(savedInstanceState.getString("saved bitmap"),0);
        if(bytes != null) {
            savedbitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            updateUI();
        }*/
        Log.i("Info","onRestore Bound " + serviceBound);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("ServiceState", serviceBound);
        outState.putBoolean("ServicerunningState", servicestarted);
        /*outState.putString("saved title",savedtitletext);
        outState.putString("saved artist",savedartisttext);
        outState.putString("saved album",savedalbumtext);
        outState.putInt("saved playback state",currentState);
        if ( savedbitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            savedbitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            bytes = baos.toByteArray();
            outState.putString("saved bitmap", Base64.encodeToString(bytes, Base64.DEFAULT));
        }*/
        Log.i("Info","onsave Bound " + serviceBound);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onDestroy() {

        Log.i("Info","On destroy " + serviceBound);


        if (mediaBrowserCompat.isConnected()){
                mediaBrowserCompat.disconnect();
        }


        storage.setservicerunningstate(servicestarted);
        storage.settaskstatus(false);

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        constraintLayout = (ConstraintLayout) findViewById(R.id.constraintlayout);

        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimary));




        titletext = (TextView) findViewById(R.id.titleText);
        artisttext = (TextView) findViewById(R.id.artisttext);
        albumtext = (TextView) findViewById(R.id.albumtext);

        albumartimageview = (ImageView) findViewById(R.id.albumartimageview);
        circleImageView = (CircleImageView) findViewById(R.id.circularimageview);


        circularSeekBar = (SeekArc) findViewById(R.id.seekbar);
        circularSeekBar.setMaxProgress(100);
        circularSeekBar.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, float progress) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekBar) {

                Log.i("progress","changed to " + seekBar.getProgress());
                //mediaControllerCompat.getTransportControls().seekTo((long)progress * 1000);
                //mediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().seekTo((long)progress * 1000);


                //updateseekbar(seekBar.getProgress());
                mediaControllerCompat.getTransportControls().seekTo((long) seekBar.getProgress());
                //updateseekbarposition();
                mIsTracking = false;

            }

            @Override
            public void onStartTrackingTouch(SeekArc seekBar) {
                mIsTracking = true;

            }
        });

        previousButton = (ImageButton) findViewById(R.id.prevButton);
        playpauseButton = (ImageButton) findViewById(R.id.playpauseButton);
        nextButton = (ImageButton) findViewById(R.id.nextButton);


        QueueButton = (ImageButton) findViewById(R.id.QueueButton);
        mediaselectionButton = (ImageButton) findViewById(R.id.mediaselectionbutton);

        storage_perm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        phonestate_perm = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE);
        permissions_needed = new ArrayList<>();

        if (storage_perm != PackageManager.PERMISSION_GRANTED){
            permissions_needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            granted = false;
        }
        if (phonestate_perm != PackageManager.PERMISSION_GRANTED){
            permissions_needed.add(Manifest.permission.READ_PHONE_STATE);
            granted = false;
        }
        if (!permissions_needed.isEmpty())
        {
            ActivityCompat.requestPermissions(this,permissions_needed.toArray
                    (new String[permissions_needed.size()]),1);
        }
        else {

            ongranted();

        }

    }


    private void ongranted(){

        if (granted) {
            mediaselectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, Main3Activity.class));
                }
            });


            storage = new Storage(getApplicationContext());
            servicestarted = storage.getservicerunningstate();
            storage.settaskstatus(true);

            Intent servcieStartIntent = new Intent(getApplicationContext(), MediaPlaybackService.class);
            startService(servcieStartIntent);
            Log.i("Info", "on create binding service");


            mediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MediaPlaybackService.class),
                    connectionCallback, null);
            Log.i("media browser", "trying to connect");
            mediaBrowserCompat.connect();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    for ( int grant : grantResults){
                        if (grant != PackageManager.PERMISSION_GRANTED){
                        exitting();
                        break;
                        }
                    }
                    granted = true;
                    ongranted();
                }
                else {
                exitting();
                }
                break;
            default:
                Log.i("pemisssion result", "denied" );
                exitting();
        }
    }

    private void updateseekbar(float progress){

        circularSeekBar.setProgress(progress);
        //Log.i("media", String.valueOf(circularSeekBar.getProgress() + " " + mediaControllerCompat.getPlaybackState().getPosition()));
        //mediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().seekTo((long)progress * 1000);
    }

    public void previousaction(View view) {

        mediaControllerCompat.getTransportControls().skipToPrevious();

    }

    public void nextaction(View view) {

        mediaControllerCompat.getTransportControls().skipToNext();
    }

    public void playpauseaction(View view) {

        if (currentState == PlaybackStateCompat.STATE_PLAYING){
            mediaControllerCompat.getTransportControls().pause();
        }
        else if (currentState == PlaybackStateCompat.STATE_PAUSED){
            mediaControllerCompat.getTransportControls().play();
        }
    }


    public void updateseekbarposition(){


        handler = new Handler();
        task = new Runnable() {
            @Override
            public void run() {
                Log.d("seek update", "Doing task");
                if (!mIsTracking){

                    updateseekbar(mediaControllerCompat.getPlaybackState().getPosition());
                }
                if (seekallowed) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(task);

    }


    public void updateplaybutton(int State) {

        switch (State) {
            case PlaybackStateCompat.STATE_PLAYING: {
                currentState = STATE_PLAYING;
                playpauseButton.setImageDrawable(getDrawable(R.drawable.ic_pause_black_48dp));


                seekallowed = true;
                updateseekbarposition();

                break;
            }
            case PlaybackStateCompat.STATE_PAUSED: {
                currentState = STATE_PAUSED;
                playpauseButton.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_48dp));
                if (handler != null){
                    handler.removeCallbacks(task);
                }
                seekallowed = false;

                break;
            }

        }


    }


    public void updateUI(MediaMetadataCompat metadata){

        savedtitletext = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        savedartisttext = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        savedalbumtext = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        savedbitmap = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);



        titletext.setText(savedtitletext);
        artisttext.setText(savedartisttext);
        albumtext.setText(savedalbumtext);

        albumartimageview.setImageBitmap(savedbitmap);
        circleImageView.setImageBitmap(savedbitmap);
        circularSeekBar.setMaxProgress(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        //Log.i("max progress","set " + circularSeekBar.g());
        updateseekbar(mediaControllerCompat.getPlaybackState().getPosition());

        Log.i("test","vcs");

    }




    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.main_exit:
                exitting();
                break;
            default:
                Log.i("menu","cant exit");
        }
        return super.onOptionsItemSelected(item);
    }


    private void exitting(){
        //mediaBrowserCompat.disconnect();
        servicestarted = false;
        storage.setservicerunningstate(servicestarted);
        mediaControllerCompat.unregisterCallback(mediaControllerCompatCallback);
        //getApplicationContext().unbindService(serviceConnection);
        stopService(new Intent(getApplicationContext(),MediaPlaybackService.class));
        finish();

    }
}
