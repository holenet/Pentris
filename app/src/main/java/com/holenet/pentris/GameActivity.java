package com.holenet.pentris;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class GameActivity extends AppCompatActivity {
    // data
    static int colors[] = {
        0xFF555599,   //0 DKGRAY
        0xFF00FFFF,   //1 CYAN
        0xFFFFFF00,   //2 YELLOW
        0xFFFF00FF,   //3 MAGENTA
        0xFFFF0000,   //4 RED
        0xFF00FF00,   //5 GREEN
        0xFF0000FF,   //6 BLUE
        0xFF000055,   //7 BLACK
        0xFF323250,   //8 GRAY
    };

    // const
    final static float R3 = 1.732050f;
    final static int DELAY_ANIM = 5;
    final static int INIT_MODE = 1000;
    final static int END_MODE = 1001;
    final static int PLAY_MODE = 1002;
    final static int PAUSE_MODE = 1003;
    final static int BLACK_MODE = 1004;
    final static String BEST_SCORE_KEY = "best001";

    // size
    float winw, winh;
    int W = 24;
    int H = 17;
    float scale;
    float ndx, ndy;

    // pref
    SharedPreferences pref;
    int best_score;
    Random random;

    // game
    long last, start_time;
    int anim_last;
    int mode = PAUSE_MODE;
    boolean play_state = false;
    int score;
    int cons;
    boolean hint_toggle, pull_toggle, music_toggle;

    EventManager eventManager;

    // Running
    Thread timeThread;
    TimeHandler timeHandler;
    boolean post_render = false;

    // Piece
    Piece current = new Piece(), next = new Piece(), hint = new Piece(), tmp = new Piece(), tmp2 = new Piece();
    int map[][] = new int[10][10];

    // View
    RelativeLayout rLsurface, rLnext;
    TextView tVbest, tVscore;
    ImageButton bTccw, bTcw, bTleft, bTright, bTpull, bTdown;
    Button bTmenu;
    MapSurface mS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // findViewById
        rLsurface = (RelativeLayout) findViewById(R.id.rLsurface);
        rLnext = (RelativeLayout) findViewById(R.id.rLnext);
        tVbest = (TextView) findViewById(R.id.tVbest);
        tVscore = (TextView) findViewById(R.id.tVscore);
        bTccw = (ImageButton) findViewById(R.id.bTccw);
        bTccw.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    if(mode==PLAY_MODE) {
                        rotate_piece(current, +1, true);
                        update_hint();
                    }
                }
                return false;
            }
        });
        bTcw = (ImageButton) findViewById(R.id.bTcw);
        bTcw.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    if(mode==PLAY_MODE) {
                        rotate_piece(current, -1, true);
                        update_hint();
                    }
                }
                return false;
            }
        });
        bTleft  = (ImageButton) findViewById(R.id.bTleft);
        bTleft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    if(mode==PLAY_MODE) {
                        trans_piece(current, -1, true);
                        update_hint();
                    }
                }
                return false;
            }
        });
        bTright = (ImageButton) findViewById(R.id.bTright);
        bTright.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    if(mode==PLAY_MODE) {
                        trans_piece(current, +1, true);
                        update_hint();
                    }
                }
                return false;
            }
        });
        bTpull = (ImageButton) findViewById(R.id.bTpull);
        bTpull.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    if(pull_toggle && mode==PLAY_MODE) {
                        while (!trans_piece(current, 0, true)) ;
                        last = System.currentTimeMillis();
                        update_hint();
                    }
                }
                return false;
            }
        });
        bTdown = (ImageButton) findViewById(R.id.bTdown);
        bTdown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    if(mode==PLAY_MODE) {
                        trans_piece(current, 0, true);
                        last = System.currentTimeMillis();
                        update_hint();
                    }
                }
                return false;
            }
        });
        bTmenu = (Button) findViewById(R.id.bTmenu);

        // score
        random = new Random();
        random.setSeed(System.currentTimeMillis());
        pref = getSharedPreferences("info", 0);
        String str = pref.getString(BEST_SCORE_KEY, Encryption.encode(0));
//        Log.e("pref", str);
//        Log.e("pref default", Encryption.encode(0));
        best_score = Encryption.decode(str);
        if(best_score<0)
            best_score = 0;
        hint_toggle = pref.getBoolean("hint", true);
        pull_toggle = pref.getBoolean("pull", true);
        music_toggle = pref.getBoolean("music", true);
        if(music_toggle)
            start_audio();

        // Handler
        timeHandler = new TimeHandler();
        eventManager = new EventManager();

        // start
        mS= new MapSurface(this);
        rLsurface.addView(mS);
        mS.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("surface", mS.getWidth()+" "+mS.getHeight());

                // size
                winw = mS.getWidth()/4.0f*3.0f;
                winh = mS.getHeight()/5.0f*4.0f;
                scale = 4.0f*winw/W/R3;
                Log.e("scale", scale+"");
                H = (int)(winh/scale);
                mS.offY = (winh-scale*(H+0.5f))/2.0f;
                if(mS.offY<0)
                    mS.offY = 0;
                else
                    mS.offY *= -1.0f;
                map = new int[W+2][H+1];
                ndx = winw/6.0f*7.0f-scale*R3*2.0f;
                ndy = scale*1.5f-winh/10.0f*4.5f;

                mS.pnt = new Paint(/*Paint.ANTI_ALIAS_FLAG*/);
                mS.pnt.setStrokeWidth(5);
                mS.pnt.setStrokeCap(Paint.Cap.ROUND);
                mS.pnt.setStrokeJoin(Paint.Join.ROUND);
                mS.path = new Path();
                mS.path.moveTo(0.0f, 0.0f);
                mS.path.lineTo(scale*R3/2.0f, -scale/2.0f);
                mS.path.lineTo(scale*R3/2.0f, scale/2.0f);
                mS.path.lineTo(0.0f, 0.0f);
                mS.path2 = new Path();
                mS.path2.moveTo(scale/R3/2, 0.0f);
                mS.path2.lineTo(scale/R3/4.0f*5.0f, -scale/4.0f);
                mS.path2.lineTo(scale/R3/4.0f*5.0f, scale/4.0f);
                mS.path2.lineTo(scale/R3/2.0f, 0.0f);

                play_state = true;

                Log.e("surface", W+" "+H+" : "+winw+" "+winh);

                init_game();
            }
        }, 50);
    }

    // check
    private boolean check_range(Piece a) {
        for(int i=0; i<5; i++)
            if(a.x+a.tri[i][0]<2 || a.x+a.tri[i][0]>=W+2 || a.y+a.tri[i][1]<1)
                return true;
        return false;
    }
    private boolean check_upper(Piece a) {
        for(int i=0; i<5; i++)
            if(a.y+a.tri[i][1]>=H+1)
                return true;
        return false;
    }
    private boolean check_block(Piece a) {
        for(int i=0; i<5; i++) {
            if(a.y+a.tri[i][1]>=H+1)
                continue;
            if(1<=map[a.x+a.tri[i][0]][a.y+a.tri[i][1]] && map[a.x+a.tri[i][0]][a.y+a.tri[i][1]]<=6)
                return true;
        }
        return false;
    }

    // mid
    private Piece new_piece() {
        Piece a = new Piece(W/4/2*4, H, 1+random.nextInt(6), random.nextInt(6));
//        Log.e("new_piece", "x:"+a.x+" y:"+a.y);
        return a;
    }
    private void destroy_block() {
        int i,j;
        boolean flag = false;
        for(i=1; i<=H; i++) {
            for(j=2; j<=W+1; j++)
                if(map[j][i]==0)
                    break;
            if(j==W+2) {
                flag = true;
                for(int k=i+1; k<=H; k++)
                    for(int l=2; l<=W+1; l++)
                        map[l][k-1] = map[l][k];
                for(int l=2; l<=W+1; l++)
                    map[l][H] = 0;
                i--;
                score += ++cons*10;
            }
        }
        cons = flag?cons:0;
        post_render = true;

        if(best_score<score) {
            best_score = score;
        }
    }
    private void piece_to_block(Piece a) {
        for(int i=0; i<5; i++)
            map[a.x+a.tri[i][0]][a.y+a.tri[i][1]] = a.color;
        score++;
        post_render = true;
    }
    private boolean set_current() {
        while(check_upper(current)) current.y--;
        while(check_block(current)) current.y++;
        return check_upper(current);
    }

    // form
    private boolean trans_piece(Piece a, int d, boolean r) {
//        Log.e("trans", "x:"+a.x+" y:"+a.y);
        tmp.copy(a);
        tmp.trans(d);
        if(d%2==0) {
            if(check_range(tmp) || check_block(tmp)) {
                if(!r) return true;
                if(check_upper(tmp)) {
                    mode = END_MODE;
                    start_time = System.currentTimeMillis();
                } else {
                    piece_to_block(a);
                    destroy_block();
                    current = next;
                    next = new_piece();
                    if(set_current()) {
                        mode = END_MODE;
                        start_time = System.currentTimeMillis();
                    }
                    update_hint();
                }
                return true;
            } else {
                a.copy(tmp);
            }
        } else {
            if(!r || (!check_range(tmp) && !check_block(tmp)))
                a.copy(tmp);
        }
        post_render = true;
        return false;
    }
    private void rotate_piece(Piece a, int h, boolean r) {
//        Log.e("rotate", "x:"+a.x+" y:"+a.y);
        tmp.copy(a);
        tmp.rotate(h);
        if(r && (check_range(tmp)||check_block(tmp))) {
            for(int k=-1; k<=1; k+=2) {
                tmp2.copy(tmp);
                tmp2.y += tmp2.x%4-1;
                tmp2.x += k*2;
                for(int i=0; i<5; i++) {
                    if((tmp2.tri[i][0]+3)%4<2)
                        tmp2.tri[i][1] += tmp2.x%4-1;
                }
                if(!check_range(tmp2) && !check_block(tmp2)) {
                    a.copy(tmp2);
                    break;
                }
            }
        } else
            a.copy(tmp);
        post_render = true;
    }
    private void update_hint() {
        hint.copy(current);
        while(!trans_piece(hint, 0, false));
        post_render = true;
    }

    // init
    public void init_game() {
        current = new_piece();
        next = new_piece();
        set_current();
        update_hint();
        for(int i=2; i<=W+1; i++)
            for(int j=1; j<=H; j++)
                map[i][j] = 7;
        anim_last = 0;
        start_time = System.currentTimeMillis();
        last = System.currentTimeMillis();
        mode = INIT_MODE;
        score = 0;
        cons = 0;
        post_render = true;
    }

    // pad
    long off;
    int last_mode;
    boolean post_exit = false;
    public void pad(View v) {
//        Log.e("pad", v.toString());
        if(mode==BLACK_MODE) {
            init_game();
            return;
        }
        switch(v.getId()) {
            case R.id.bTmenu:
                if(mode!=PLAY_MODE) {
                    off=System.currentTimeMillis()-start_time;
                    last_mode = mode;
                } else
                    off = -1;
                mode = PAUSE_MODE;
                showDialog();
                play_state = false;
                break;
/*            case R.id.bTccw:
                if(mode==PLAY_MODE)
                    rotate_piece(current, +1, true);
                break;
            case R.id.bTcw:
                if(mode==PLAY_MODE)
                    rotate_piece(current, -1, true);
                break;
            case R.id.bTleft:
                if(mode==PLAY_MODE)
                    trans_piece(current, -1, true);
                break;
            case R.id.bTright:
                if(mode==PLAY_MODE)
                    trans_piece(current, +1, true);
                break;
            case R.id.bTpull:
                if(pull_toggle && mode==PLAY_MODE) {
                    while (!trans_piece(current, 0, true)) ;
                    last = System.currentTimeMillis();
                }
                break;
            case R.id.bTdown:
                if(mode==PLAY_MODE) {
                    trans_piece(current, 0, true);
                    last = System.currentTimeMillis();
                }
                break;*/
            case R.id.bTresume:
                if(off>=0) {
                    start_time = System.currentTimeMillis()-off;
                    mode = last_mode;
                } else {
                    mode = PLAY_MODE;
                }
                dismiss_dialog();
                break;
            case R.id.bTrestart:
                dismiss_dialog();
                init_game();
                break;
            case R.id.bThelp:
                Toast.makeText(this, R.string.help_content, Toast.LENGTH_SHORT).show();
                break;
            case R.id.bTranking:
                Intent intent = new Intent(getApplicationContext(), RankingActivity.class);
                intent.putExtra("best_score", best_score);
                startActivity(intent);
                break;
            case R.id.bTexitgame:
                post_exit = true;
                dismiss_dialog();
                finish();
                break;
        }
//        update_hint();
//        Log.e("mode", mode+"");
    }
    public void onBackPressed() {
        Log.e("onBackPressed();"," "+mode);
        if(mode!=PAUSE_MODE)
            bTmenu.callOnClick();
    }
    private void dismiss_dialog() {
        DialogFragment df = (DialogFragment)(getFragmentManager().findFragmentByTag("paused"));
        if(df!=null)
            df.dismiss();
        play_state = true;
    }

    // Thread Handler
    protected void onStart() {
        super.onStart();
        if (timeThread == null) {
            timeThread = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(71);
                            Message msg = timeHandler.obtainMessage();
                            timeHandler.sendMessage(msg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            timeThread.start();
        }
    }
    protected void onStop() {
        timeThread = null;
        SharedPreferences.Editor editor = pref.edit();
        if(best_score<score)
            best_score = score;
        editor.putString(BEST_SCORE_KEY, Encryption.encode(best_score));
        editor.putBoolean("hint", hint_toggle);
        editor.putBoolean("pull", pull_toggle);
        editor.putBoolean("music", music_toggle);
        editor.apply();
        super.onStop();
    }
    protected void onPause() {
        if(!post_exit && mode!=PAUSE_MODE)
            bTmenu.callOnClick();
        if(loopMediaPlayer!=null)
            loopMediaPlayer.pauseMedia();
        super.onPause();
    }
    protected void onResume() {
        if(loopMediaPlayer!=null)
            loopMediaPlayer.resumeMedia();
        super.onResume();
    }

    private class TimeHandler extends Handler {
        public void handleMessage(Message msg) {
            tVbest.setText(String.format("%d", best_score));
            tVscore.setText(String.format("%d", score));
        }
    }

    // Surface
    boolean isProcessing = false;
    private class MapSurface extends SurfaceView implements SurfaceHolder.Callback {
        TThread thread;
        Paint pnt;
        Path path, path2;
        float offY;
        int backColor;
        Drawable drawableBackground;
        Drawable[] drawablePieces = new Drawable[7];
        Resources res;
        int[][] size = new int[7][4];
        float scale_d;

        class TThread extends Thread {
            SurfaceHolder mSurfaceHolder;
            boolean exit;
            public TThread(SurfaceHolder surfaceHolder) {
                mSurfaceHolder = surfaceHolder;
                if(pnt==null)
                    pnt = new Paint();
                if(path==null)
                    path = new Path();
                if(path2==null)
                    path2 = new Path();
                res = getResources();
                scale_d = Float.valueOf(res.getString(R.string.scale));
                backColor = res.getColor(R.color.colorBackground);
                drawableBackground = res.getDrawable(R.drawable.background_play);
                drawablePieces[1] = res.getDrawable(R.drawable.piece_i);
                drawablePieces[2] = res.getDrawable(R.drawable.piece_c);
                drawablePieces[3] = res.getDrawable(R.drawable.piece_l);
                drawablePieces[4] = res.getDrawable(R.drawable.piece_l_);
                drawablePieces[5] = res.getDrawable(R.drawable.piece_p);
                drawablePieces[6] = res.getDrawable(R.drawable.piece_p_);

                TypedArray ta = res.obtainTypedArray(R.array.piece_array);
                int n = ta.length();
                for(int i=0; i<n; i++) {
                    int id = ta.getResourceId(i, 0);
                    if(id>0) {
                        size[i] = res.getIntArray(id);
                    } else {
                        Log.e("Wrong", "xml parsing");
                    }
//                    Log.e("ta", array[i][0]+" "+array[i][1]+" "+array[i][2]+" "+array[i][3]);
                }
                ta.recycle();;
            }
            public void run() {
                while (!exit) {
                    synchronized (mSurfaceHolder) {
                        // process
                        if (!isProcessing) {
                            isProcessing=true;

                            if (play_state) {
                                switch(mode) {
                                    case INIT_MODE:
                                        if(eventManager.currentState==EventManager.ANIM_NONE)
                                            eventManager.startAnim(EventManager.ANIM_BEGIN);
                                        break;
                                    case END_MODE:
                                        if(eventManager.currentState==EventManager.ANIM_NONE)
                                            eventManager.startAnim(EventManager.ANIM_END);
                                        break;
                                    case PLAY_MODE:
                                        if (last < System.currentTimeMillis()-1000*(Math.exp(-score/1000.0)+0.4)) {
                                            last=System.currentTimeMillis();
                                            trans_piece(current, 0, true);
                                        }
                                        break;
                                }
                            }

                            // render
                            if (post_render) {
                                post_render = false;
                                if(!draw())
                                    post_render = true;
                            }

                            isProcessing = false;
                        }
                    }
                    try {
                        sleep(32);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    Log.e("process", "----"+System.currentTimeMillis());
                }
            }

            int count = 0;
            boolean isDrawing;
            protected boolean draw() {
                Log.e("SurfaceView", "draw: "+count++);
                Canvas canvas = null;
                // lock
                canvas=mSurfaceHolder.lockCanvas();
                if(canvas==null) {
                    Log.e("SurfaceView", "Drawing Error: cannot load canvas");
                    return true;
                }

                // draw
                canvas.drawColor(backColor);
                canvas.translate(0, offY);

                for (int i=2; i <= W+1; i++)
                    for (int j=1; j <= H; j++)
                        drawTriangle(canvas, i, j, map[i][j], map[i][j] == 0 && j%2 == 1);
                if (mode == PLAY_MODE) {
                    if (hint_toggle && mode == PLAY_MODE)
                        for (int i=0; i < 5; i++) {
                            drawTriangle(canvas, hint.x+hint.tri[i][0], hint.y+hint.tri[i][1], 0, false);
                            drawTriangle(canvas, hint.x+hint.tri[i][0], hint.y+hint.tri[i][1], hint.color, true);
                        }
                    for (int i=0; i < 5; i++)
                        drawTriangle(canvas, current.x+current.tri[i][0], current.y+current.tri[i][1], current.color, false);

                    canvas.translate(0, -offY);
                    canvas.translate(ndx-next.center*scale*R3/4.0f, ndy);
                    for (int i=0; i < 5; i++)
                        drawTriangle(canvas, 8+next.tri[i][0], 2+next.tri[i][1], next.color, false);
                    canvas.translate(-ndx+next.center*scale*R3/4.0f, -ndy);
                }

                // unlock
                mSurfaceHolder.unlockCanvasAndPost(canvas);

                return true;
            }

            protected boolean draw2() {
                if(isDrawing)
                    return false;
                isDrawing = true;
                Log.e("SurfaceView", "draw: "+count++);
                Canvas canvas = null;
                // lock
                canvas=mSurfaceHolder.lockCanvas();
                if(canvas==null) {
                    Log.e("SurfaceView", "Drawing Error: cannot load canvas");
                    return true;
                }

                // draw
                canvas.drawColor(backColor);
                //                            canvas.drawColor(Color.rgb(50, 50, 50));
//                canvas.translate(0, offY);

                drawableBackground.setBounds(0, (int)(winh-(H+0.5)*scale), (int)(W/2*scale/2*R3*size[0][2]/size[0][0]), (int)(winh-(H+0.5)*scale+H*scale*size[0][3]/size[0][1]));
                drawableBackground.draw(canvas);

                for (int i=2; i <= W+1; i++)
                    for (int j=1; j <= H; j++)
                        if(map[i][j]!=0)
                            drawTriangle(canvas, i, j, map[i][j], map[i][j] == 0 && j%2 == 1);
                if (mode == PLAY_MODE) {
                    if (hint_toggle && mode == PLAY_MODE)
                        for (int i=0; i < 5; i++) {
                            drawTriangle(canvas, hint.x+hint.tri[i][0], hint.y+hint.tri[i][1], 0, false);
                            drawTriangle(canvas, hint.x+hint.tri[i][0], hint.y+hint.tri[i][1], hint.color, true);
                        }
//                    for (int i=0; i < 5; i++)
//                        drawTriangle(canvas, current.x+current.tri[i][0], current.y+current.tri[i][1], current.color, false);
                    // drawable Test

                    canvas.translate((current.x/2)*scale*R3/2.0f, winh-current.y*scale-(current.x%4/2-1)*scale/2);
                    canvas.rotate(-current.theta*60);
//                    Log.e("piece_c", drawable.getIntrinsicHeight()+" "+drawable.getIntrinsicWidth());
                    int[] bound = new int[4];
                    for(int i=0; i<4; i++)
                        bound[i] = (int) (scale*size[current.color][i]/scale_d);
//                    Log.e("bound", scale_d+" "+scale+"   "+ll+" "+tt+" "+rr+" "+bb);
                    drawablePieces[current.color].setBounds(-bound[0], -bound[1], bound[2], bound[3]);
                    drawablePieces[current.color].draw(canvas);
                    canvas.rotate(current.theta*60);
                    canvas.translate(-(current.x/2)*scale*R3/2.0f, -winh+current.y*scale+(current.x%4/2-1)*scale/2);

                    // next
                    canvas.translate(0, -offY);
                    canvas.translate(ndx-next.center*scale*R3/4.0f+2*scale*R3, ndy+winh-2*scale);
//                    for (int i=0; i < 5; i++)
//                        drawTriangle(canvas, 8+next.tri[i][0], 2+next.tri[i][1], next.color, false);
                    canvas.rotate(-next.theta*60);
                    for(int i=0; i<4; i++)
                        bound[i] = (int) (scale*size[next.color][i]/scale_d);
                    drawablePieces[next.color].setBounds(-bound[0], -bound[1], bound[2], bound[3]);
                    drawablePieces[next.color].draw(canvas);
                    canvas.rotate(next.theta*60);
                    canvas.translate(-ndx+next.center*scale*R3/4.0f-2*scale*R3, -ndy-winh+2*scale);
                }

                // unlock
                mSurfaceHolder.unlockCanvasAndPost(canvas);
                isDrawing = false;

                return true;
            }

            protected boolean draw3() {
                if(isDrawing)
                    return false;
                isDrawing = true;
                Log.e("SurfaceView", "draw: "+count++);
                Canvas canvas = null;
                // lock
                canvas=mSurfaceHolder.lockCanvas();
                if(canvas==null) {
                    Log.e("SurfaceView", "Drawing Error: cannot load canvas");
                    return true;
                }

                // draw
                canvas.drawColor(backColor);
                canvas.translate(0, offY);

                // Bitmap Test
//                Bitmap mBitmap = BitmapFactory.decodeResource(res, R.drawable.background_play);
//                mBitmap = Bitmap.createScaledBitmap(mBitmap, (int)(W/2*scale/2*R3), (int)((H+0.5)*scale), true);
                BitmapDrawable bd = (BitmapDrawable) res.getDrawable(R.drawable.background_play);
                bd.setBounds(0, (int)(winh-(H+0.5)*scale), (int)(W/2*scale/2*R3*size[0][2]/size[0][0]), (int)(winh-(H+0.5)*scale+H*scale*size[0][3]/size[0][1]));
                bd.draw(canvas);
//                Bitmap mBitmap = bd.getBitmap();
//                canvas.drawBitmap(mBitmap, 0, 0, pnt);

                canvas.scale(1, 0.5f);
                for (int i=2; i <= W+1; i++)
                    for (int j=1; j <= H; j++)
                        drawTriangle(canvas, i, j, map[i][j], map[i][j] == 0 && j%2 == 1);
                if (mode == PLAY_MODE) {
                    if (hint_toggle && mode == PLAY_MODE)
                        for (int i=0; i < 5; i++) {
                            drawTriangle(canvas, hint.x+hint.tri[i][0], hint.y+hint.tri[i][1], 0, false);
                            drawTriangle(canvas, hint.x+hint.tri[i][0], hint.y+hint.tri[i][1], hint.color, true);
                        }
                    for (int i=0; i < 5; i++)
                        drawTriangle(canvas, current.x+current.tri[i][0], current.y+current.tri[i][1], current.color, false);

                    canvas.translate(0, -offY);
                    canvas.translate(ndx-next.center*scale*R3/4.0f, ndy);
                    for (int i=0; i < 5; i++)
                        drawTriangle(canvas, 8+next.tri[i][0], 2+next.tri[i][1], next.color, false);
                    canvas.translate(-ndx+next.center*scale*R3/4.0f, -ndy);
                }

                // unlock
                mSurfaceHolder.unlockCanvasAndPost(canvas);
                isDrawing = false;

                return true;
            }

            private void drawTriangle(Canvas canvas, int x, int y, int c, boolean h) {
                x -= 2;
                canvas.translate((x/4*2+1)*scale*R3/2.0f, winh-y*scale);
                canvas.rotate((3-x%4)*60);

                pnt.setColor(colors[c]);
                pnt.setStyle(Paint.Style.FILL);
                canvas.drawPath(path, pnt);

                pnt.setColor(Color.BLACK);
                pnt.setAlpha(40);
                canvas.drawPath(path, pnt);

                if(c!=8) {
                    pnt.setColor(colors[c]);
                    canvas.drawPath(path2, pnt);

                    pnt.setColor(Color.BLACK);
                    if(h) {
                        pnt.setAlpha(c==0?30:60);
                        canvas.drawPath(path, pnt);
                    }

                    pnt.setAlpha(255);
                    pnt.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, pnt);
                }


                canvas.rotate(-(3-x%4)*60);
                canvas.translate(-(x/4*2+1)*scale*R3/2.0f, -winh+y*scale);
            }
        }

        // etc.
        public MapSurface(Context context) {
            super(context);

            SurfaceHolder holder = getHolder();
            holder.addCallback(this);
            thread = new TThread(holder);
            setFocusable(true);
        }
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
        public void surfaceCreated(SurfaceHolder holder) {
            thread = new TThread(holder);
            thread.start();
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            thread.exit = true;
            while(true) {
                try {
                    thread.join();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class EventManager {
        final static int ANIM_NONE = 999;
        final static int ANIM_BEGIN = 998;
        final static int ANIM_END = 997;

        int currentState;
        EventThread lastThread;

        EventManager() {
            currentState = ANIM_NONE;
        }

        public void startAnim(int state) {
            Log.e("startAnim", "start "+state);
            switch(state) {
                case ANIM_BEGIN:
                case ANIM_END:
                    currentState = state;
                    if(lastThread!=null) {
                        lastThread.cancel = true;
                    }
                    lastThread = new EventThread(state, 2000);
                    lastThread.start();
                    break;
                default:
                    Log.e("startAnim", "Invalid State!!");
            }
        }

        class EventThread extends Thread {
            int state;
            int time;
            boolean cancel;
            EventThread(int state, int time) {
                this.state = state;
                this.time = time;
                cancel = false;
            }
            public void run() {
                for(int i=0; i<H; i++) {
                    if(cancel)
                        return;
//                    while(post_render);
                    for(int j=0; j<W; j++)
                        map[j+2][state==ANIM_BEGIN?i+1:H-i] = state==ANIM_BEGIN?0:7;
                    post_render = true;
                    try {
                        do {
                            Thread.sleep(time/H);
                        }  while(!play_state);
                    } catch(Exception e) {
                        Log.e("AnimSleep", e.getMessage());
                    }
                }
                post_render = true;
                mode = state==ANIM_BEGIN?PLAY_MODE:BLACK_MODE;
                currentState = ANIM_NONE;
            }
        }
    }

    // dialog
    void showDialog() {
        Log.e("showDialog", "");
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("paused");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = PausedFragment.newInstance(hint_toggle, pull_toggle, music_toggle);
        newFragment.show(ft, "paused");
    }

    // audio
    private LoopMediaPlayer loopMediaPlayer;
    public void start_audio() {
        kill_audio();
//        loopMediaPlayer = LoopMediaPlayer.create(this, R.raw.sample_begin, R.raw.sample_loop);
    }
    public void kill_audio() {
        if(loopMediaPlayer!=null) {
            loopMediaPlayer.killMedia();
            loopMediaPlayer=null;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        kill_audio();
    }
}