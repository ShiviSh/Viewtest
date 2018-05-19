package com.example.shivi.viewtest;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.shivi.viewtest.Storage.Storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,

        AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    private Uri contentUri;
    private int resumePosition;
    private AudioManager audioManager;

    private int seekposition;

    private ScheduledExecutorService worker1 = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService worker2 = Executors.newSingleThreadScheduledExecutor();
    private Runnable task1;
    private Runnable task2;

    private boolean skiponcontrol = false;
    private Storage storage;
    private boolean saved = false;

    private float left;
    private float right;
    private float left2;
    private float right2;

    public static final String ACTION_PLAY = "com.example.shivi.viewtest.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.shivi.viewtest.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.shivi.viewtest.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.shivi.viewtest.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.shivi.viewtest.ACTION_STOP";


    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private MediaControllerCompat mediaControllerCompat;
    private PlaybackStateCompat.Builder playbackStateCompat;
    private int Playstate;


    private MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    private static final int NOTIFICATION_ID = 101;


    private ArrayList<Long> medialist;
    private int playposition;

    private boolean repeat = false;
    private boolean shuffle = false;
    private boolean resumeongain = false;

    private MediaMetadataCompat mediaMetadata;
    private MediaDescriptionCompat description;


    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;






    private void initMediaSession() throws RemoteException{
        if (mediaSessionManager != null)
            return;

        Log.i("media","before initialisation");
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        Log.i("media","before initialising media session");

        mediaSession = new MediaSessionCompat(this,"MediaPlaybackService");
        transportControls = mediaSession.getController().getTransportControls();

        Log.i("media","after transport controls");
        mediaControllerCompat = mediaSession.getController();
        //mediaSessionCallback = new MediaSessionCallback();


        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );
        setSessionToken(mediaSession.getSessionToken());

        mediaSession.setActive(true);

        Log.i("media","after setting flags");


       /*playbackStateCompat =  new PlaybackStateCompat.Builder()
               .setActions(
                       PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
               PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_PAUSE)
               .setState(PlaybackStateCompat.STATE_PLAYING,PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,1);

       mediaSession.setPlaybackState(playbackStateCompat.build());*/

        Log.i("media","setting playback state");

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {

                Log.i("Info","OnPlay callback called");

                super.onPlay();
                if (requestAudioFocus()){

                    if (saved){
                        saved = false;
                    }

                    mediaSession.setActive(true);
                    callStateListener();
                    //registerNotificationReceiver();
                    registerBecomingNoisyReceiver();
                    resumeMedia();
                }

            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

                Log.i("media","media button event received ");
                return super.onMediaButtonEvent(mediaButtonEvent);


            }

            @Override
            public void onPrepare() {
                super.onPrepare();
            }

            @Override
            public void onPause() {
                Log.i("Info","OnPause callback called");


                super.onPause();
                pauseMedia();
                if (!storage.gettaskstatus()){
                    Log.i("foreground","service no longer in foreground");
                    stopForeground(false);
                }
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {

                Log.i("Info","OnskiptoNext callback called");


                super.onSkipToNext();
                skiponcontrol = true;
                skiptonext();
            }

            @Override
            public void onSkipToPrevious() {

                Log.i("Info","Onskiptoprevious callback called");

                super.onSkipToPrevious();
                skiptoprevious();
            }

            @Override
            public void onStop() {

                super.onStop();


                stopMedia();
                mediaSession.setActive(false);
            }

            @Override
            public void onSeekTo(long pos) {

                Log.i("seek to called","true " + pos);
                if (mediaPlayer != null && !saved){
                    mediaPlayer.seekTo((int) pos);
                }
                seekposition = (int) pos;
                setNewState(mediaSession.getController().getPlaybackState().getState());
                super.onSeekTo(pos);
            }
        });
    }

    private void updateMetaData(){


        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getalbumart())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)))
                .build());
    }




    private Bitmap getalbumart(){

        ContentResolver contentResolver = getContentResolver();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.albumcover);;

        byte[] art;

        try{
            art = mediaMetadataRetriever.getEmbeddedPicture();
            bitmap = BitmapFactory.decodeByteArray(art,0,art.length);

        }catch (Exception e){
            //e.printStackTrace();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String projections[] = {MediaStore.Audio.Media.ALBUM_ID};
            String selection = MediaStore.Audio.Media._ID + "=" + medialist.get(playposition);
            Cursor cursor = contentResolver.query(uri,projections,selection,null,null);
            if (cursor != null && cursor.getCount() > 0 ){
                while (cursor.moveToNext()){
                    long albumid = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                    Uri sArtworkUri = Uri
                            .parse("content://media/external/audio/albumart");
                    Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumid);
                    Log.i("albumart",albumArtUri.toString());

                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), albumArtUri);
                        //bitmap = Bitmap.createScaledBitmap(bitmap, 30, 30, true);
                    } catch (IOException f) {
                        Log.i("Bitmap error",f.getMessage());
                    }
                }
            }
        }
        return bitmap;
    }


    private BroadcastReceiver playnewmedia_Broadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundlereceived = intent.getBundleExtra("Bundle");
            medialist = (ArrayList<Long>) bundlereceived.getSerializable("medialist");
            playposition = bundlereceived.getInt("position");

            stopMedia();
            if (mediaPlayer != null){
                mediaPlayer.reset();
            }

            if (saved){
                saved = false;
            }
            storage.insertmedialist(medialist);

            setContenUri();
            initMediaPlayer();
            updateMetaData();

        }
    };

  /*  private BroadcastReceiver mediarefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean runningstate = intent.getBooleanExtra("service state",false);
            if (runningstate){
                mediaSession.setMetadata(mediaSession.getController().getMetadata());
                mediaSession.setPlaybackState(mediaSession.getController().getPlaybackState());
            }
            else {
                refreshUI();
            }
        }
    };
*/

    private void refreshUI(){

            saved = true;
            Log.i("medialist",medialist.toString());
            setContenUri();
            initMediaPlayer();
            updateMetaData();
            mediaSession.getController().getTransportControls().seekTo(resumePosition);
            setNewState(PlaybackStateCompat.STATE_PAUSED);
            buildNotification(PlaybackStatus.PAUSED);


    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i("Info","Service started");

        if (intent != null && intent.getExtras() != null){

            int i = intent.getExtras().getInt("delete");
            if (i == -9){
                stopSelf();
            }
        }




       /* try {
            //An audio file is passed to the service through putExtra();
            mediaFile = intent.getExtras().getString("media");
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaFile != null && mediaFile != "")
            initMediaPlayer();*/

        //buildNotification(PlaybackStatus.PLAYING);
       /* if (mediaSession != null)

        MediaButtonReceiver.handleIntent(mediaSession,intent);*/
        //handleIncomingActions(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (mediaPlayer != null || mediaSession != null) {
            transportControls.pause();
            transportControls.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            storage.setcurretnplayingsong(playposition);
            storage.setresumeposition(resumePosition);
            storage.insertmedialist(medialist);
            storage.setservicerunningstate(false);
            Log.i("media" ,"saved " + String.valueOf(playposition));
            mediaSession.release();
        }
        removeAudioFocus();
        removeNotification();
        unregisterReceiver(becomingNoisyReceiver);


        Log.i("Info","Service endeddddddddd");
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("Info","Service oncreate");


        if (mediaSessionManager == null) {
            try {
                Log.i("Mediasession" ,"creating ");
                initMediaSession();
                storage = new Storage(getApplicationContext());
                medialist = storage.getmedialist();
                playposition = storage.getplayposition();
                resumePosition = storage.getresumeposition();
                //Log.i("media received ",medialist.toString() + " " + playposition + " " + resumePosition);
                if ( medialist != null && playposition != -1 && resumePosition != -1) {
                    refreshUI();
                }
                else {
                    defaultui();
                }

                //initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            //buildNotification(PlaybackStatus.PLAYING);
        }

        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(playnewmedia_Broadcastreceiver,new IntentFilter(Main3Activity.LocalBroadcast_Play_new_Audio));
        //LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mediarefresh,new IntentFilter(MainActivity.Broadcast_refresh));

    }

    private void defaultui() {
        ContentResolver contentResolver = getContentResolver();

        String id = "_id";
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0){

            medialist = new ArrayList<>();
            while (cursor.moveToNext()){
                Log.i("id ecesived",String.valueOf(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID))));
                long lid = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                medialist.add(lid);
            }
        }
        cursor.close();
        resumePosition = 0;
        playposition = 0;
        refreshUI();

    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Log.i("task removed","called");
        storage.settaskstatus(false);
        super.onTaskRemoved(rootIntent);

    }

    public MediaPlaybackService() {
    }


    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.i("Info","media browser get root");
        return new BrowserRoot("root",null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.i("Info","media browser get root loadchildren");

    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (resumeongain){

                    if (mediaPlayer == null){
                        initMediaPlayer();
                    }
                    else if (!mediaPlayer.isPlaying()) {
                        mediaSession.getController().getTransportControls().play();
                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                }

                mediaPlayer.setVolume(1.0f, 1.0f);
                Log.i("Info","AUDIOFOCUS_GAIN");
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                mediaSession.getController().getTransportControls().pause();
                //removeAudioFocus();
                resumeongain = false;
                /*mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;*/
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                    if (!resumeongain) {
                        resumeongain = true;
                    }
                }
                mediaSession.getController().getTransportControls().pause();
                Log.i("Info","AUDIOFOCUS_LOSS_TRANSIENT");
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.2f, 0.2f);
                Log.i("Info","AUDIOFOCUS_LOSS_TRANSIENT");
                break;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        skiponcontrol = false;

        skiptonext();

        //stopSelf();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }

        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        Log.i("Info","Prepared!!!");

        if (!saved){

            if (requestAudioFocus()){
                //updateMetaData();
                playMedia();

            }

        }
        else {
            setNewState(PlaybackStateCompat.STATE_PAUSED);
            buildNotification(PlaybackStatus.PAUSED);
        }

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    public void setContenUri() {

        contentUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, medialist.get(playposition));

        Log.i("Info","Receiving Media " + contentUri.toString());
        /*initMediaPlayer();
        playMedia();*/
    }

    private void initMediaPlayer() {
        Log.i("media","Entered initmediaplayer");
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        Log.i("media","starting initialisation");
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        //mediaPlayer.reset();
        Log.i("media","completed initialisation");

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(MediaPlaybackService.this,contentUri);
            mediaMetadataRetriever.setDataSource(MediaPlaybackService.this,contentUri);
            storage.setcurretnplayingsong(playposition);
            storage.setresumeposition(0);
            //mediaPlayer.prepare();
            //mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("Info","Cant play file");
            mediaSession.getController().getTransportControls().stop();
        }

        mediaPlayer.prepareAsync();
    }



    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //resumeongain = true;
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }


    private void playMedia() {
        Log.i("Info","playing media");
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.setVolume(0.1f,0.1f);
            seekposition = 0;
            mediaPlayer.start();
            buildNotification(PlaybackStatus.PLAYING);
            setNewState(PlaybackStateCompat.STATE_PLAYING);
            fadein();
        }

    }

    private void fadein() {

        left = 2;
        right = left;
        Log.i("fade in","called");
        //handler1 = new Handler();
        worker1 = Executors.newSingleThreadScheduledExecutor();
        task1 = new Runnable() {
            @Override
            public void run() {
                if (left == 10){
                    //handler1.removeCallbacks(task1);
                    worker1.shutdown();
                }
                Log.i("fade in", "Doing task");
                mediaPlayer.setVolume(( left/10),(right/10));
                left += 1;
                right += 1;
                //handler1.postDelayed(this, 500);
            }
        };
        worker1.scheduleWithFixedDelay(task1,0,100,TimeUnit.MILLISECONDS);
        //handler1.post(task1);
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();

        }
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        mediaPlayer.reset();
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            worker1.shutdown();

            fadeout();
            //mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();

            seekposition = resumePosition;
            setNewState(PlaybackStateCompat.STATE_PAUSED);


        }
    }

    private void fadeout() {

        left2 = 10;
        right2 = left2;
        Log.i("fade out","called");

        worker2 = Executors.newSingleThreadScheduledExecutor();
        // handler2 = new Handler();
        task2 = new Runnable() {
            @Override
            public void run() {
                if(left2 == 1){
                    worker2.shutdown();
                    mediaPlayer.pause();
                    Log.i("thread","interuppted");
                }
                Log.i("fade out","doing task");
                mediaPlayer.setVolume((left2/10),(right2/10));
                left2--;
                right2--;
            }
        };
        worker2.scheduleWithFixedDelay(task2,0,60,TimeUnit.MILLISECONDS);
        //handler2.post(task2);
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            worker2.shutdown();
            mediaPlayer.setVolume(0.1f,0.1f);
            mediaPlayer.start();
            buildNotification(PlaybackStatus.PLAYING);
            setNewState(PlaybackStateCompat.STATE_PLAYING);
            fadein();
        }
    }





    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            mediaSession.getController().getTransportControls().pause();
            //buildNotification(PlaybackStatus.PAUSED);
        }
    };


    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }



    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaSession.getController().getTransportControls().pause();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null ) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                mediaSession.getController().getTransportControls().play();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void skiptonext(){

        if (!shuffle){
            if (playposition == medialist.size() - 1) {
                if (repeat) {
                    playposition = 0;
                }
                else {
                    if (!skiponcontrol) {
                        saved = true;
                    }
                    playposition = 0;
                }
            }
            else {
                playposition++;
            }

            seekposition = 0;
            //setNewState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
            stopMedia();
            setContenUri();
            initMediaPlayer();

        }

        Log.i("Info","skiptonext called playbackstate stopped");

        updateMetaData();


    }

    private void skiptoprevious(){

        if (playposition == 0){
            playposition = medialist.size() - 1;
        }
        else {
            playposition--;
        }

        //setNewState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
        stopMedia();

        seekposition = 0;
        setContenUri();
        initMediaPlayer();
        //setNewState(PlaybackStateCompat.STATE_STOPPED);
        Log.i("Info","skiptoprevious called playbackstate stopped");

        updateMetaData();
    }



    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_PAUSE);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_PLAY);
        }

        Bitmap largeIcon = getalbumart(); //replace with your own image


        mediaMetadata = mediaSession.getController().getMetadata();
        description = mediaMetadata.getDescription();

        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        final Intent deleteIntent = new Intent(getApplicationContext(),MediaPlaybackService.class);
        deleteIntent.putExtra("delete", -9);
        PendingIntent deletePendingIntent = PendingIntent.getService(this,
                -9,
                deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Create a new Notification

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"SS");

        notificationBuilder
                .setShowWhen(false)

                // Set the Notification style
                // Set the Notification color
                .setColor(ContextCompat.getColor(this,R.color.colorPrimary))
                // Set the large and small icons)
                //.setContentIntent(mediaControllerCompat.getSessionActivity())
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent,0))
                // Set Notification content information
                .setContentTitle(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setContentText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setSubText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .setLargeIcon(mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                .setDeleteIntent(deletePendingIntent)
                // Add playback actions
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_previous,"previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                .addAction(new NotificationCompat.Action(
                        notificationAction, "pause",
                        play_pauseAction))
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_next, "next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.

                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP))
                        .setShowActionsInCompactView(0, 1, 2)
                );

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
        if (storage.gettaskstatus()) {
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }



    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch (Playstate) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }


    private void setNewState(@PlaybackStateCompat.State int newPlayerState) {
        Playstate = newPlayerState;

        // Whether playback goes to completion, or whether it is stopped, the
        // mCurrentMediaPlayedToCompletion is set to true.
        /*if (Playstate == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true;
        }

        // Work around for MediaPlayer.getCurrentPosition() when it changes while not playing.
        final long reportPosition;
        if (mSeekWhileNotPlaying >= 0) {
            reportPosition = mSeekWhileNotPlaying;

            if (Playstate == PlaybackStateCompat.STATE_PLAYING) {
                mSeekWhileNotPlaying = -1;
            }
        } else {
            resumePosition = mediaPlayer == null ? 0 : mediaPlayer.getCurrentPosition();
        }*/

        playbackStateCompat = new PlaybackStateCompat.Builder();
        playbackStateCompat.setActions(getAvailableActions());
        playbackStateCompat.setState(Playstate,
                seekposition,
                1.0f,
                SystemClock.elapsedRealtime());
        mediaSession.setPlaybackState(playbackStateCompat.build());
    }

    public void destructmethod(){


        if (mediaPlayer != null) {
            transportControls.pause();
            transportControls.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            storage.setcurretnplayingsong(playposition);
            storage.setresumeposition(resumePosition);
            storage.insertmedialist(medialist);
            storage.setservicerunningstate(false);
            Log.i("media" ,"saved " + String.valueOf(playposition));
            mediaSession.release();
        }
        removeAudioFocus();
        unregisterReceiver(becomingNoisyReceiver);
        removeNotification();


        Log.i("Info","Service endeddddddddd successfully");
    }


}
