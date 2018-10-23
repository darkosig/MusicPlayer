package com.example.darko.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by darko on 1.2.2018..
 */

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInflater;

    public SongAdapter (Context context, ArrayList<Song> theSongs){
        songs = theSongs;
        songInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout songLayout = (LinearLayout) songInflater.inflate(R.layout.song_item, viewGroup, false);

        TextView tvSongArtist = songLayout.findViewById(R.id.tv_song_artist);
        TextView tvSongTitle = songLayout.findViewById(R.id.tv_song_title);

        Song currentSong = songs.get(i);

        tvSongArtist.setText((i+1) + ". " + currentSong.getArtist());
        tvSongTitle.setText(currentSong.getTitle());

        songLayout.setTag(i);

        return songLayout;
    }
}
