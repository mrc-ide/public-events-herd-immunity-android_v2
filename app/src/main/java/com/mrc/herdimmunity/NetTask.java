package com.mrc.herdimmunity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetTask extends AsyncTask<String, Void, String> {
    //private Exception exception;

    private static final Handler progressCheckHandler = new Handler();

    // There are warnings about leaked context here, but I can't immediately
    // see how to run things on the UI thread from within the doInBackground
    // call without tracking the view or parent reference like this.

    // Also, AsyncTask appears to be both recommended and deprecated in the
    // docs in favour of Callable. Migrate at some point...

    @SuppressLint("StaticFieldLeak")
    private final MainActivity parent;

    public NetTask(MainActivity p) {
        parent = p;
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    public void check_status() {
        parent.runOnUiThread(() -> progressCheckHandler.postDelayed(() -> new NetTask(parent).executeOnExecutor(parent.threadPoolExecutor,
                parent.serverName + "herd.php?cmd=get_status"), 1000));
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            System.out.println("DO IN BACKGROUND - params[0] = "+params[0]);
            final String result;
            URL url = new URL(params[0]);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            result = readStream(con.getInputStream());
            if (params[0].endsWith("get_status")) {
                switch (result) {
                    case "WAIT": {
                        parent.readyToGo();
                        break;
                    }
                    case "DEMO": {
                        ImageView go = parent.findViewById(R.id.go_button);
                        check_status();
                        parent.readyToQueue();
                        break;
                    }
                    case "RUN": {
                        check_status();
                        parent.readyToQueue();
                        break;
                    }
                }
            } else check_status();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}