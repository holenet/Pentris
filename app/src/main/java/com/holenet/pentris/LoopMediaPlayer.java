package com.holenet.pentris;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class LoopMediaPlayer {

    public static final String TAG = LoopMediaPlayer.class.getSimpleName();

    private Context mContext = null;
    private int mResId_b = 0;
    private int mResId_l = 0;
    private int mCounter = 1;

    private MediaPlayer mCurrentPlayer = null;
    private MediaPlayer mNextPlayer = null;

    public static LoopMediaPlayer create(Context context, int resId_b, int resId_l) {
        return new LoopMediaPlayer(context, resId_b, resId_l);
    }

    private LoopMediaPlayer(Context context, int resId_b, int resId_l) {
        mContext = context;
        mResId_b = resId_b;
        mResId_l = resId_l;

        mCurrentPlayer = MediaPlayer.create(mContext, mResId_b);
        mCurrentPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mCurrentPlayer.start();
            }
        });

        createNextMediaPlayer();
    }

    private void createNextMediaPlayer() {
        mNextPlayer = MediaPlayer.create(mContext, mResId_l);
        mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
        mCurrentPlayer.setOnCompletionListener(onCompletionListener);
    }

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.release();
            mCurrentPlayer = mNextPlayer;

            createNextMediaPlayer();

            Log.d(TAG, String.format("Loop #%d", ++mCounter));
        }
    };

    public void killMedia() {
        if(mCurrentPlayer!=null) {
            mCurrentPlayer.release();
            mCurrentPlayer = null;
        }
        if(mNextPlayer!=null) {
            mNextPlayer.release();
            mNextPlayer = null;
        }
    }

    int seekTime;
    public void pauseMedia() {
        seekTime = mCurrentPlayer.getCurrentPosition();
        mCurrentPlayer.pause();
    }

    public void resumeMedia() {
        mCurrentPlayer.seekTo(seekTime);
        mCurrentPlayer.start();
    }
}