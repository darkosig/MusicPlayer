package com.example.darko.musicplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Song> songList;
    private MusicService musicsrv;
    private SongAdapter songAdapter;
    boolean musicBound = false;
    private int currentSong = 0;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean receiverRegistered = false;
    public boolean shuffle = false;
    private BroadcastReceiver receiver;
    public ServiceConnection musicConnection;
    final public static Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");


    @BindView(R.id.lv_songList)
    ListView lvSongList;

    @BindView(R.id.btn_next)
    Button btnNext;

    @BindView(R.id.btn_prev)
    Button btnPrevious;

    @BindView(R.id.btn_play)
    Button btnPlay;

    @BindView(R.id.btn_stop)
    Button btnStop;

    @BindView(R.id.seekBar)
    SeekBar seekBar;

    @BindView(R.id.tv_nowPlayingArtist)
    TextView tvNowPlayingArtist;

    @BindView(R.id.tv_nowPlayingTitle)
    TextView tvNowPlayingTitle;

    @BindView(R.id.tv_elapsed)
    TextView tvElapsed;

    @BindView(R.id.tv_remain)
    TextView tvRemain;

    @BindView(R.id.iv_AlbumArt)
    ImageView ivAlbumArt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                showWarningDialog();

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK}, 0);


                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {

            setSongAdapterAndLoadSongs();

            initializeBroadcastReceiver();

            registerBroadcastReceiver();

            initializeMusicConnection();

            bindService();

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_repeat) {
            if (item.isChecked()){
                item.setChecked(false);
                musicsrv.setRepeat(false);
            } else {
                item.setChecked(true);
                musicsrv.setRepeat(true);
            }

            return true;
        }

        if (id == R.id.action_shuffle){
            if (item.isChecked()){
                item.setChecked(false);
                shuffle = false;
                musicsrv.setShuffle(false);
            } else {
                item.setChecked(true);
                shuffle = true;
                musicsrv.setShuffle(true);
            }

            return true;
        }

        if(id == R.id.action_exit){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void bindService() {
        bindService(new Intent(getApplicationContext(), MusicService.class), musicConnection, Context.BIND_AUTO_CREATE);
    }

    public void setSongAdapterAndLoadSongs() {
        songList = new ArrayList<>();
        getSongList();
        songAdapter = new SongAdapter(this, songList);
        lvSongList.setAdapter(songAdapter);
        lvSongList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                currentSong = i;
                musicsrv.play(currentSong);
                isPlaying = true;
                btnPlay.setBackground(getDrawable(R.drawable.pause));
                updateNowPlayingInfo();
            }
        });
        initializeSeekBar();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    if (isPlaying || isPaused) {
                        musicsrv.seekTo(i * 1000);
                    }
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void initializeBroadcastReceiver() {

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                isPlaying = arg1.getBooleanExtra("IS_PLAYING", false);
                currentSong = arg1.getIntExtra("CURRENT_SONG",-1);
                btnPlay.setBackground(getDrawable(R.drawable.play));
                if(!isPlaying){
                    Toast.makeText(arg0, "Reached end of the playlist", Toast.LENGTH_SHORT).show();
                }
                updateNowPlayingInfo();
            }
        };

    }

    public void registerBroadcastReceiver() {
        registerReceiver(receiver, new IntentFilter("PLAYBACK_CHANGED"));
        receiverRegistered = true;
    }

    public void unregisterBroadcastReceiver() {
        unregisterReceiver(receiver);
        receiverRegistered = false;
    }

    public void initializeMusicConnection() {

        if (musicConnection == null) {

            musicConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
                    musicsrv = binder.getService();
                    musicsrv.setList(songList);
                    musicBound = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    musicBound = false;
                }
            };
        }

    }

    public void initializeSeekBar() {

        final Handler mHandler = new Handler();

        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (isPlaying == true) {
                    int mCurrentPosition = musicsrv.getPosition() / 1000;
                    seekBar.setMax(musicsrv.getDuration() / 1000);
                    seekBar.setProgress(mCurrentPosition);

                    String elapsed = String.format("%01d" + ":" + "%02d", TimeUnit.MILLISECONDS.toMinutes(musicsrv.getPosition()),
                            TimeUnit.MILLISECONDS.toSeconds(musicsrv.getPosition()) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(musicsrv.getPosition())));

                    String remaining = String.format("-" + "%01d" + ":" + "%02d", TimeUnit.MILLISECONDS.toMinutes(musicsrv.getDuration() - musicsrv.getPosition()),
                            TimeUnit.MILLISECONDS.toSeconds(musicsrv.getDuration() - musicsrv.getPosition()) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(musicsrv.getDuration() - musicsrv.getPosition())));

                    tvElapsed.setText(elapsed);
                    tvRemain.setText(remaining);

                }
                mHandler.postDelayed(this, 1000);
            }
        });

    }

    public void showWarningDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning!");
        builder.setMessage("This application needs a permission to read external storage to function properly. Please enable permission in system settings.");
        builder.setCancelable(false);
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // task you need to do.

                    setSongAdapterAndLoadSongs();

                    initializeBroadcastReceiver();

                    registerBroadcastReceiver();

                    initializeMusicConnection();

                    bindService();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    showWarningDialog();
                }

                return;
            }

        }
    }

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = musicResolver.query(musicUri, null, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            do {
                int songId = cursor.getInt(idColumn);
                String songTitle = cursor.getString(titleColumn);
                String songArtist = cursor.getString(artistColumn);
                long albumID = cursor.getLong(albumIdColumn);

                songList.add(new Song(songId, songTitle, songArtist, albumID));

            } while (cursor.moveToNext());

        } else {
            // No songs found on the device
            tvNowPlayingTitle.setText("No songs found.");
            disableButtons();
        }
    }

    public void disableButtons() {
        btnPrevious.setClickable(false);
        btnNext.setClickable(false);
        btnPlay.setClickable(false);
        btnStop.setClickable(false);
        seekBar.setClickable(false);
    }

    public void updateNowPlayingInfo() {
        if(isPlaying) {
            tvNowPlayingArtist.setText(songList.get(currentSong).getArtist().toString());
            tvNowPlayingTitle.setText(songList.get(currentSong).getTitle().toString());
            ivAlbumArt.setImageBitmap(getAlbumBitmap());
        } else if (!isPlaying && !isPaused){
            tvNowPlayingArtist.setText("");
            tvNowPlayingTitle.setText("No song playing");
            ivAlbumArt.setImageDrawable(getDrawable(R.drawable.album));
        }
    }

    public Bitmap getAlbumBitmap() {
        Bitmap albumArtBitMap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();

        Uri uri = ContentUris.withAppendedId(albumArtUri, songList.get(currentSong).getAlbumArt());
        try {
            ParcelFileDescriptor pfd = getApplicationContext().getContentResolver().openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                albumArtBitMap = BitmapFactory.decodeFileDescriptor(fd, null,
                        options);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return albumArtBitMap;
    }

    @OnClick(R.id.btn_prev)
    public void btnPrevious() {
        currentSong -= 1;
        if (currentSong < 0) {
            currentSong = songList.size() - 1;
        }
        musicsrv.play(currentSong);
        isPlaying = true;
        btnPlay.setBackground(getDrawable(R.drawable.pause));
        updateNowPlayingInfo();
    }

    @OnClick(R.id.btn_next)
    public void btnNext() {
        if(shuffle){
            currentSong = musicsrv.getRandomSong();
        } else {
            currentSong += 1;
            if (currentSong > songList.size() - 1) {
                currentSong = 0;
            }
        }

        musicsrv.play(currentSong);
        isPlaying = true;
        btnPlay.setBackground(getDrawable(R.drawable.pause));
        updateNowPlayingInfo();
        }


    @OnClick(R.id.btn_play)
    public void btnPlay() {
        if (isPlaying) {
            musicsrv.pause();
            btnPlay.setBackground(getDrawable(R.drawable.play));
            isPlaying = false;
            isPaused = true;
            updateNowPlayingInfo();
        } else {
            if (isPaused) {
                musicsrv.unPause();
                isPlaying = true;
                isPaused = false;
                btnPlay.setBackground(getDrawable(R.drawable.pause));
                updateNowPlayingInfo();
            } else {
                musicsrv.play(currentSong);
                btnPlay.setBackground(getDrawable(R.drawable.pause));
                isPlaying = true;
                updateNowPlayingInfo();
            }
        }
    }

    @OnClick(R.id.btn_stop)
    public void btnStop() {
        if (isPlaying || isPaused) {
            musicsrv.stop();
            seekBar.setProgress(0);
            isPlaying = false;
            isPaused = false;
            btnPlay.setBackground(getDrawable(R.drawable.play));
        }
    }

    @Override
    protected void onDestroy() {
        isPlaying = false;
        if(receiverRegistered){
            unregisterBroadcastReceiver();
        }
        if(musicBound){
            unbindService(musicConnection);
        }
        super.onDestroy();
    }
}
