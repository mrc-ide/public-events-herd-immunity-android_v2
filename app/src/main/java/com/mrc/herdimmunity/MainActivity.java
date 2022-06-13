package com.mrc.herdimmunity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "herd_immunity";
    public static final String version = "2.0";
    public static int internal_version = 2;

    int corePoolSize = 60;
    int maximumPoolSize = 80;
    int keepAliveTime = 10;

    String serverName;

    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maximumPoolSize);

    Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
            keepAliveTime, TimeUnit.SECONDS, workQueue);

    //////////////////////////////////////////////////////////////
    // Send all the state to the java client
    //////////////////////////////////////////////////////////////

    String state_r0 = "5";
    int state_r0_progress = 5;
    String state_vacc = "50 %";
    int state_vacc_progress = 5;

    //////////////////////////////////////////////////////////////
    // The popup menu - Check for Updates, Set Server, or About //
    //////////////////////////////////////////////////////////////

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
/*
        if (item.getItemId() == R.id.update_check_menu) {
            //U.tryUpdate(false);
            return true;
        }
*/
        if (item.getItemId() == R.id.server_url_menu) {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle("Set Server URL"); //Set Alert dialog title here
            alert.setMessage("Server"); //Message here
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            input.setText(serverName);
            alert.setView(input);
            // End of onClick(DialogInterface dialog, int whichButton)
            alert.setPositiveButton("OK", (dialog, whichButton) -> {
                serverName = input.getEditableText().toString();
                if (!serverName.toUpperCase().startsWith("HTTP://")) serverName="http://"+serverName;
                if (!serverName.endsWith("/")) serverName+="/";
                readyToGo();
            }); //End of alert.setPositiveButton

            alert.setNegativeButton("CANCEL", (dialog, whichButton) -> dialog.cancel()); //End of alert.setNegativeButton
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
            return true;
        }

        if (item.getItemId() == R.id.about_menu) {
            AlertDialog.Builder ver = new AlertDialog.Builder(MainActivity.this);
            ver.setTitle("Herd Immunity Overlord");
            ver.setMessage("Version: "+version );
            ver.setCancelable(false);
            ver.setPositiveButton("OK", (dialog, whichButton) -> dialog.cancel());
            AlertDialog ad = ver.create();
            ad.show();
            return true;
        }

        if (item.getItemId() == R.id.demo_mode) {
            enterDemoMode();
        }

        return true;
    }

    public void enterDemoMode() {
      new NetTask(this).executeOnExecutor(threadPoolExecutor,serverName + "herd.php?cmd=req&vacc=-1&r0=-1");
      readyToGo();
    }

    public void readyToGo() {
        ImageView go = findViewById(R.id.go_button);
        assert go != null;
        go.setImageResource(R.drawable.run_red);
        go.setEnabled(true);
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        View background = findViewById(R.id.mainlayout);
        background.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard(view);
                return false;
            }
        });

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.herd_app_yellow);
        actionBar.setTitle(Html.fromHtml("&nbsp;&nbsp;&nbsp;<font color='#003e74'>Herd Immunity Simulator</font>"));

        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#ffdd00"));
        actionBar.setBackgroundDrawable(colorDrawable);

        // Load preferences

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        serverName = settings.getString("server", "http://192.168.1.42/epi");

        // Move the R0 seekbar

        SeekBar r0_seek = findViewById(R.id.r0_seek);
        TextView r0_val = findViewById(R.id.r0_value);

        assert r0_val != null;
        assert r0_seek != null;

        r0_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                state_r0 = String.valueOf(r0_val.getText());
                state_r0_progress = r0_seek.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                r0_val.setText(String.valueOf(progress));
            }
        });

        // Move the T_inf seekbar

        SeekBar vacc_seek = findViewById(R.id.vacc_seek);
        TextView vacc_val = findViewById(R.id.vacc_value);

        assert vacc_seek != null;
        assert vacc_val != null;

        vacc_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                System.out.println("ON STOP VACC");
                state_vacc = String.valueOf(vacc_val.getText());
                state_vacc_progress = vacc_seek.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                vacc_val.setText(String.valueOf(progress * 10)+" %");
            }
        });
        ImageView go = findViewById(R.id.go_button);
        assert go != null;

        EditText et = findViewById(R.id.data_name);

        MainActivity ma = this;
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = et.getText().toString();
                if (s.length()==0) s = " ";
                byte[] name = null;
                try {
                    name = et.getText().toString().getBytes("UTF-8");
                } catch (Exception e) {}

                go.setImageResource(R.drawable.run_grey);
                go.setEnabled(false);
                new NetTask(ma).executeOnExecutor(threadPoolExecutor,
                        serverName + "herd.php?cmd=req"+
                                "&r0="+state_r0_progress+
                                "&vacc="+(10 * state_vacc_progress)+
                                "&name="+ Base64.encodeToString(name, Base64.DEFAULT));

            }
        });

        // First-time - get status
/*
        Timer netTimer = new Timer();
        netTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateNetStatus();
            }
        }, 0, 1000);
*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Save preferences on stopping
    @Override
    protected void onStop() {
        super.onStop();
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("server", serverName);
        // Commit the edits!
        editor.apply();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}
