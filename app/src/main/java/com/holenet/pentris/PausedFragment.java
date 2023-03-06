package com.holenet.pentris;

import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class PausedFragment extends DialogFragment {
    boolean hint, pull, music;
    Button bTresume, bTrestart, bThelp, bTexitgame;
    ToggleButton tBhint/*, tBpull*/, tBmusic;

    static PausedFragment newInstance(boolean h, boolean p, boolean m) {
        PausedFragment f=new PausedFragment();

        Bundle args=new Bundle();
        args.putBoolean("hint", h);
        args.putBoolean("pull", p);
        args.putBoolean("music", m);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hint = getArguments().getBoolean("hint");
        pull = getArguments().getBoolean("pull");
        music = getArguments().getBoolean("music");

//        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_DialogWhenLarge_NoActionBar);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.paused, container, false);
        bTresume = (Button) v.findViewById(R.id.bTresume);
        bTrestart = (Button) v.findViewById(R.id.bTrestart);
        bThelp = (Button) v.findViewById(R.id.bThelp);
        bTexitgame = (Button) v.findViewById(R.id.bTexitgame);
        tBhint = (ToggleButton) v.findViewById(R.id.tBhint);
//        tBpull = (ToggleButton) v.findViewById(R.id.tBpull);
        tBmusic = (ToggleButton) v.findViewById(R.id.tBmusic);
        tBhint.setChecked(hint);
//        tBpull.setChecked(pull);
        tBmusic.setChecked(music);
        tBmusic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    ((GameActivity)getActivity()).start_audio();
                else
                    ((GameActivity)getActivity()).kill_audio();
            }
        });

        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.e("onDismiss", ""+((GameActivity)getActivity()).mode);
        setting();
        ((GameActivity)getActivity()).post_render = true;
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log.e("onCancel", ""+((GameActivity)getActivity()).mode);
        bTresume.callOnClick();
        setting();
        ((GameActivity)getActivity()).post_render = true;
        super.onCancel(dialog);
    }

    private void setting() {
        ((GameActivity)getActivity()).hint_toggle = tBhint.isChecked();
        ((GameActivity)getActivity()).music_toggle = tBmusic.isChecked();
    }
}
