package com.example.darko.musicplayer;

import android.graphics.Bitmap;

/**
 * Created by darko on 1.2.2018..
 */

public class Song {

    private long id;
    private String title;
    private String artist;
    private long albumArt;


    public Song (long id, String title, String artist, long albumArt) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumArt = albumArt;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getAlbumArt(){
        return albumArt;
    }
}
