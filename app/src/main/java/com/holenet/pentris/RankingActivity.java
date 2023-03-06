package com.holenet.pentris;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class RankingActivity extends AppCompatActivity {
    ListView listView;
    RankAdapter adapter;
    ProgressBar pBconn;

    int best_score;
    int last_submit_score;
    SharedPreferences pref;

    String name;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ranking);

        // Intent
        Intent intent = getIntent();
        best_score = intent.getIntExtra("best_score", 0);

        // Pref
        pref = getSharedPreferences("info", 0);
        name = pref.getString("name", "");
        last_submit_score = pref.getInt("last_submit_score", -1);

        // ListView
        listView = (ListView) findViewById(R.id.lVranking);
        adapter = new RankAdapter(this, R.layout.item_ranking, new ArrayList<RankItem>());
        pBconn = (ProgressBar) findViewById(R.id.pBconn);

        // get Data
        get();
    }

    private void refreshData (JSONArray ja) throws JSONException {
        adapter.clear();
        Log.e("refreshData", "JSONArray = "+ja.toString());

        for(int i=0; i<ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            adapter.addItem(new RankItem(jo.getInt("rank"), jo.getString("username"), jo.getInt("score"), jo.getString("submitted_date").substring(0,10)));
        }
         /*for(int i=0; i<ranks.size(); i++)
            adapter.addItem(ranks.get(i));
*/
        listView.setAdapter(adapter);
    }

    protected void onResume() {
        super.onResume();
    }

    public void pad(View v) {
        switch(v.getId()) {
            case R.id.bTsubmit:
                if(best_score>last_submit_score) {
                    showDialog();
                } else {
                    Toast.makeText(this, R.string.submit_limit, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bTclose:
                finish();
                break;
            case R.id.bTrefresh:
                get();
                break;
        }
    }

    protected void get() {
        Log.e("get","start");
        String urlStr = "http://pentris.skystarr.com/ranking/";
        GetDataThread thread = new GetDataThread(urlStr);
        pBconn.setVisibility(View.VISIBLE);
        thread.start();
    }

    protected void submit(String user_name) {
        Log.e("send","start");
        String urlStr = "http://pentris.skystarr.com/submit";
//        String urlStr = "http://httpbin.org/post";
        SendDataThread thread = new SendDataThread(urlStr, user_name);
        pBconn.setVisibility(View.VISIBLE);
        thread.start();
    }

    private void complete(int code) {
        if(code!=HttpURLConnection.HTTP_OK) {
            Toast.makeText(this, "Network Error: "+code, Toast.LENGTH_SHORT).show();
        }
        last_submit_score = best_score;
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("last_submit_score", last_submit_score);
        editor.apply();
    }

    // Thread
    class GetDataThread extends Thread {
        String urlStr;

        public GetDataThread(String inStr) {
            urlStr = inStr;
        }

        public void run() {
            String outputStr = "";
            try {
                outputStr = request(urlStr);
                Log.e("GET :", "output"+outputStr);

            } catch(Exception e) {
                e.printStackTrace();
            }
            final String output = outputStr;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONArray ja;
                        ja = new JSONArray(output);
                        refreshData(ja);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("make JSON error", e.toString());
                    }
                    pBconn.setVisibility(View.INVISIBLE);
                }
            });
        }

        private String request(String urlStr) {
            StringBuilder output = new StringBuilder();
            try {
                URL url = new URL(urlStr);

                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Log.e("request conn", ""+conn);
                if(conn!=null) {
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    int resCode = conn.getResponseCode();
                    Log.e("request", "resCode "+resCode);
                    if(resCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = null;
                        while(true) {
                            line = reader.readLine();
                            if(line==null)
                                break;
                            Log.e("line", line);
                            output.append(line);
                        }
                        reader.close();
                        conn.disconnect();
                    }
                }
            } catch(Exception e) {
                Log.e("HTTP", "Exception in processing response", e);
            }
            return output.toString();
        }
    }
    class SendDataThread extends Thread {
        String urlStr, user_name;

        public SendDataThread(String inStr, String inName) {
            urlStr = inStr;
            user_name = inName;
        }

        public void run() {
            int outputCode = 400;
            try {
/*
                JSONObject jo = new JSONObject();
                jo.put("score", Encryption.encode(best_score));
                jo.put("username", user_name);
                Log.e("data", jo.toString());
*/
                urlStr += "?score="+Encryption.encode(best_score)+"&username="+user_name;
                Log.e("urlStr", urlStr);
                outputCode = new_request(urlStr);
                //jo = new JSONObject(jo.toString());
//                Log.e("parsed", "score "+jo.getString("score"));
                Log.e("SEND :", "output "+outputCode);
            } catch(Exception e) {
                e.printStackTrace();
            }
            final int output = outputCode;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    complete(output);
                    get();
                }
            });
        }

/*
        private int request2(String urlStr, JSONObject jo) {
            int output = -1;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);

                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());


                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    readStream(in);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
*/


       /* private int new_request(String urlStr, JSONObject jo) {
            int output = -1;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(urlStr);

                Log.d("sampleHttpClient", "\nRequest using HttpClient");
                HttpResponse = client.execute(httppost);
            }
        }*/

        private int new_request(String urlStr) {
            int resCode = -1;
            try {
                URL url = new URL(urlStr);

                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Log.e("request conn", ""+conn);
                if(conn!=null) {
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    resCode = conn.getResponseCode();
                    Log.e("request", "resCode "+resCode);
                    if(resCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = null;
                        while(true) {
                            line = reader.readLine();
                            if(line==null)
                                break;
                            Log.e("line", line);
                        }
                        reader.close();
                        conn.disconnect();
                    }
                }
            } catch(Exception e) {
                Log.e("HTTP", "Exception in processing response", e);
            }
            return resCode;
        }

        private int request(String urlStr, JSONObject jo) {
            int output = 0;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Log.e("request conn", ""+conn);
                if(conn!=null) {
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//                    conn.setRequestProperty("Accept", "application/json");
//                    conn.connect();

/*
                    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
//                    out.writeUTF("data="+jo.toString());
                    out.write(jo.toString().getBytes("UTF-8"));
                    out.flush();
                    out.close();
*/
/*
                    OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
                    osw.write(jo.toString());
                    osw.flush();
                    osw.close();
*/
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.write("{\"score\":\"hTUE2YiiERHXFDfiHYZAJJTJ8kWEvczy8D3kEHCt52eF{835307G{w6IhkHRZZ6ve39yCtFTjYhyFvHR{TxW3z7[vt6e}ZzTY2HR5z[5f46}[thHF6{UEITt[297V2ETwt}IDl{V}{DRTF358CGIxckzgH}WiYFRxX9y8JZCIti39I|3wHZA34vT[vUXTtk}lG98vjDcv25liiFD{dV46EyU6C909EFH3v8zVcf9hHl4hx4d3lykZHFzFd[llEjlvC6R3kgy26WFWuk5Zx7yZVTAY9|gZw}HFBZ{6j3yIwYuU3gT6IU7eu\",\"username\":\"holenet\"}".getBytes("UTF-8"));
                    os.flush();
                    os.close();

/*                    OutputStream os = conn.getOutputStream();
                    os.write(jo.toString().getBytes("UTF-8"));
                    os.close();*/

                    int resCode = conn.getResponseCode();
                    output = resCode;
                    Log.e("request", "resCode "+resCode);
//                    if(resCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = null;
                        while(true) {
                            line = reader.readLine();
                            if(line==null)
                                break;
                            Log.e("line", line);
                        }
                        reader.close();
                        conn.disconnect();
//                    }
                }
            } catch(Exception e) {
                Log.e("HTTP", "Exception in processing response", e);
            }
            return output;
        }
    }

    void showDialog() {
        Log.e("showDialog", "Ranking_submit");
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("submit");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        SubmitFragment newFragment = SubmitFragment.newInstance(best_score, name);
        newFragment.show(ft, "submit");
    }

    // Adapter Class
    private class RankAdapter extends ArrayAdapter<RankItem> {
        private ArrayList<RankItem> items;

        public RankAdapter(Context context, int textViewResourceId, ArrayList<RankItem> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.item_ranking, null);
            }
            RankItem p = items.get(position);
            if (p != null) {
                TextView tVrank = (TextView) v.findViewById(R.id.tVrank);
                TextView tVrank_username = (TextView) v.findViewById(R.id.tVrank_username);
                TextView tVrank_score = (TextView) v.findViewById(R.id.tVrank_score);
                TextView tVrank_submitdate = (TextView) v.findViewById(R.id.tVrank_submitdate);
                if (tVrank != null)
                    tVrank.setText(p.getRank()+"");
                if (tVrank_username != null)
                    tVrank_username.setText(p.getUsername());
                if (tVrank_score != null)
                    tVrank_score.setText(p.getScore()+"");
                if (tVrank_submitdate != null)
                    tVrank_submitdate.setText(p.getSubmitdate());
            }
            return v;
        }

        // Set Items
        public void setItems(ArrayList<RankItem> items) {
            this.items = items;
        }

        // Add Items
        public boolean addItem(RankItem it) {
            items.add(it);
            return true;
        }

        // Get Item
        public RankItem getItem(int position) {
            return items.get(position);
        }

        // Remove Item
        public boolean removeItem(RankItem rankItem) {
            super.remove(rankItem);
            boolean removed = false;
            for(int i = 0; i<items.size(); i++) {
                if(items.get(i).equals(rankItem)) {
                    items.remove(i);
                    removed = true;
                }
            }
            return removed;
        }
    }
    // Item Class
    public class RankItem {
        private int rank;
        private String username;
        private int score;
        private String submitdate;
        private boolean selectable = false;

        public RankItem(int rank, String username, int score, String submitdate) {
            this.rank = rank;
            this.username = username;
            this.score = score;
            this.submitdate = submitdate;
        }

        //setter
        public void setRanking(int rank, String username, int score, String submitdate) {
            this.rank = rank;
            this.username = username;
            this.score = score;
            this.submitdate = submitdate;
        }

        //getter
        public int getRank() {
            return rank;
        }
        public String getUsername() {
            return username;
        }
        public int getScore() {
            return score;
        }
        public String getSubmitdate() {
            return submitdate;
        }

        public boolean isSelectable() {
            return selectable;
        }
    }
}