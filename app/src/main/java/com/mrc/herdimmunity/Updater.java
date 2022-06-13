package com.mrc.herdimmunity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class Updater {
    private static final byte GET_VERSION = 1;

    private final String protocol;
    private final boolean slack_ssl;
    private final String baseServer;
    private final String urlVersion;
    private final String urlAPK;
    private final int currentVer;
    private final String currentVerName;
    private String newVerName;
    private final MainActivity ma;

    Updater(String protocol, String baseServer,
            String urlVersion, String urlAPK,
            int currentVersion, String currentVersionName,
            MainActivity mainActivity, boolean imperial_ssl) {
        this.protocol = protocol;
        this.baseServer = baseServer;
        this.urlVersion = urlVersion;
        this.urlAPK = urlAPK;
        this.currentVer = currentVersion;
        this.currentVerName = currentVersionName;
        this.ma = mainActivity;
        slack_ssl = imperial_ssl;
    }

    private void fail(boolean silent, String err) {
        if (!silent) {
            AlertDialog.Builder alert = new AlertDialog.Builder(ma);
            alert.setTitle("Oops."); //Set Alert dialog title here
            alert.setMessage(err); //Message here
            alert.setPositiveButton("Oh.", (dialog, whichButton) -> dialog.cancel());
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();

        }
    }

    void tryUpdate(boolean silent) {
        boolean netCon = pingTest("8.8.8.8");
        if (!netCon) fail(silent, "I don't seem to have internet access.");
        else {
            new UpdateTask(GET_VERSION).executeOnExecutor(ma.threadPoolExecutor);
        }

    }

    private boolean pingTest(String ip) {
        Runtime runtime = Runtime.getRuntime();
        boolean ok;
        try {
            Process mIpAddrProcess = runtime.exec("/system/bin/ping -w 2 -c 1 " + ip);
            int mExitValue = mIpAddrProcess.waitFor();
            ok = (mExitValue == 0);
        } catch (Exception e) {
            ok = false;
        }
        return ok;
    }

    private final Runnable noSoftwareUpdate = new Runnable() {
        public void run() {
            AlertDialog.Builder alert = new AlertDialog.Builder(ma);
            alert.setTitle("Already up to date!"); //Set Alert dialog title here
            alert.setMessage("Version  " + currentVerName + " is the latest."); //Message here
            alert.setPositiveButton("Alright", (dialog, whichButton) -> dialog.cancel());
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        }
    };

    private final Runnable offerSoftwareUpdate = new Runnable() {
        public void run() {
            AlertDialog.Builder alert = new AlertDialog.Builder(ma);
            alert.setTitle("Software Update!"); //Set Alert dialog title here
            alert.setMessage("Update from version  " + currentVerName + " to " + newVerName); //Message here
            alert.setPositiveButton("Yes", (dialog, whichButton) -> {
                String ff = protocol+baseServer+urlAPK;
                ma.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ff)));
            });
            alert.setNegativeButton("No", (dialog, whichButton) -> dialog.cancel()); //End of alert.setNegativeButton
            alert.setCancelable(false);
            AlertDialog alertDialog = alert.create();
            alertDialog.show();
        }
    };



    private class UpdateTask extends AsyncTask<String, Void, String> {
        private final byte mode;
        private UpdateTask(byte m) {
            mode = m;

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

        @Override
        protected String doInBackground(String... params) {
            final String result;
            try {
                URL url = null;
                if (mode == GET_VERSION) {
                    url = new URL(protocol + baseServer + urlVersion);
                }

                /* Deal with Imperial's dodgy certificate */

                if (slack_ssl) {

                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }

                                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                                }

                                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                                }
                            }
                    };

                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                }

                /* Done */

                URLConnection uc;
                if (url==null) uc=null;
                else uc = url.openConnection();
                if (uc!=null) {
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    result = readStream(con.getInputStream());
                    try {
                        String[] bits = result.split("\t");
                        int newVer = Integer.parseInt(bits[0]);
                        newVerName = bits[1];
                        if (newVer != currentVer) {
                            ma.runOnUiThread(offerSoftwareUpdate);
                        } else {
                            ma.runOnUiThread(noSoftwareUpdate);
                        }

                    } catch (Exception e) {
                        Updater.this.fail(true, "Something went wrong. Sorry.");

                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }

            return "";
        }
    }
}