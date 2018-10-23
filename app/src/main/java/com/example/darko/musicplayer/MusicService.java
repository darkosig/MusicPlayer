package com.example.darko.musicplayer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by darko on 4.2.2018..
 */

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private final IBinder binder = new MusicBinder();
    private int currentSong;
    private boolean repeat = false;
    private boolean shuffle = false;

    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        initMusicPlayer();
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnPreparedListener(this);
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public int getPosition() {
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public void setRepeat(boolean b){
        repeat = b;
    }

    public void setShuffle(boolean b){
        shuffle = b;
    }

    public void seekTo(int i) {
        player.seekTo(i);
    }

    public void play(int song) {
        player.stop();
        player.reset();

        currentSong = song;

        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(song).getId());
        try {
            player.setDataSource(getApplicationContext(), songUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
    }

    public void pause() {
        player.pause();
    }

    public void unPause() {
        player.start();
    }

    public void stop() {
        player.stop();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        stopSelf();
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        // play next song in playlist, stop at the end

        if(repeat){
            play(currentSong);
        } else if (shuffle){
            currentSong = getRandomSong();
            play(currentSong);
            sendBroadcast(currentSong, true);
        } else {
            currentSong += 1;
            if (currentSong >= songs.size()) {
                player.stop();
                // send broadcast to MainActivity that the playback has stopped
                sendBroadcast(currentSong - 1, false);
            } else {
                play(currentSong);
                sendBroadcast(currentSong, true);
            }
        }

    }

    private void sendBroadcast(int currentSong, boolean isPlaying){
        Intent intent = new Intent("PLAYBACK_CHANGED");
        intent.putExtra("CURRENT_SONG", currentSong);
        intent.putExtra("IS_PLAYING", isPlaying);
        sendBroadcast(intent);
    }

    public int getRandomSong(){
        Random rand = new Random();
        int x = rand.nextInt(songs.size());
        return x;
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        player.start();
    }

}
