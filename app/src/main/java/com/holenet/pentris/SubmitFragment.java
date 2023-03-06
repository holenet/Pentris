package com.holenet.pentris;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SubmitFragment extends DialogFragment {
    int score;
    String name;
    EditText eTsubmit_name;
    TextView tBsubmit_best;
    Button bTsend;

    static SubmitFragment newInstance(int score, String name) {
        SubmitFragment f = new SubmitFragment();

        Bundle args=new Bundle();
        args.putInt("score", score);
        args.putString("name", name);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        score = getArguments().getInt("score");
        name = getArguments().getString("name");

//        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.submit, container, false);
        eTsubmit_name = (EditText) v.findViewById(R.id.eTsubmit_name);
        eTsubmit_name.setText(name);
        tBsubmit_best = (TextView) v.findViewById(R.id.tVsubmit_best);
        tBsubmit_best.setText(score+"");
        bTsend = (Button) v.findViewById(R.id.bTsend);
        bTsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = eTsubmit_name.getText().toString();
                ((RankingActivity)getActivity()).name = name;
                ((RankingActivity)getActivity()).pBconn.setVisibility(View.INVISIBLE);
                ((RankingActivity)getActivity()).submit(name);
                dismiss();
            }
        });
        return v;
    }

    private void save() {
        name = eTsubmit_name.getText().toString();
        ((RankingActivity)getActivity()).name = name;
        SharedPreferences.Editor editor = ((RankingActivity)getActivity()).pref.edit();
        editor.putString("name", name);
        editor.apply();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        save();
        super.onDismiss(dialog);
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        save();
        super.onCancel(dialog);
    }
}