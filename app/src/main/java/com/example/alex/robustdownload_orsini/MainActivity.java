package com.example.alex.robustdownload_orsini;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends ActionBarActivity {

    Button downloadButton;
    TextView inputUrl;

    SharedPreferences preferenceManager;
    DownloadManager downloadManager;
    private long downloadReference;
    private BroadcastReceiver receiverDownloadComplete;
    private BroadcastReceiver receiverNotificationClicked;
    ArrayList<ConnectionInfo> connectionsList = new ArrayList<ConnectionInfo>();
    TextView downloadPercentage;
    ConnectivityManager cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadButton = (Button) findViewById(R.id.download_button);
        inputUrl= (TextView) findViewById(R.id.download_url);

        preferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
        downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        final ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        downloadPercentage = (TextView) findViewById(R.id.download_percentage);

        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(isConnected){
            final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            ConnectionInfo currentConnection = new ConnectionInfo(connectionInfo.toString(),new Date(System.currentTimeMillis()));
            connectionsList.add(currentConnection);
        }

        this.registerReceiver(this.myWifiReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        downloadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // starting new Async Task
                String text = inputUrl.getText().toString();
                Log.i("info: ", text);
                //new DownloadFileFromURL().execute(text);

                String[] splitUrl = text.split("/");
                String filename = splitUrl[splitUrl.length - 1];
                Log.i("filename: ", filename);

                Uri uri = Uri.parse(text);
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setDescription(filename).setTitle(filename);
                request.setDestinationInExternalFilesDir(MainActivity.this, Environment.DIRECTORY_DOWNLOADS, filename);
                request.setVisibleInDownloadsUi(true);
                //request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

                downloadReference = downloadManager.enqueue(request);
                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        boolean downloading = true;

                        while (downloading) {

                            DownloadManager.Query q = new DownloadManager.Query();
                            q.setFilterById(downloadReference);

                            Cursor cursor = downloadManager.query(q);
                            cursor.moveToFirst();
                            int bytes_downloaded = cursor.getInt(cursor
                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                                downloading = false;
                            }

                            final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    mProgressBar.setProgress((int) dl_progress);
                                    downloadPercentage.setText(dl_progress + "%");

                                }
                            });

                            //Log.d(Constants.MAIN_VIEW_ACTIVITY, statusMessage(cursor));
                            cursor.close();
                        }

                    }
                }).start();

            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        final IntentFilter filter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        receiverNotificationClicked = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extraId = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
                long[] references = intent.getLongArrayExtra(extraId);
                for(long reference : references){
                    if(reference==downloadReference){
                        //Do Something
                    }
                }
            }
        };
        registerReceiver(receiverNotificationClicked,filter);

        final IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadReference == reference) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);
                    int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                    String savedFilePath = cursor.getString(filenameIndex);
                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            //Do stuff
                            Toast.makeText(MainActivity.this, "Success!!! ", Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_FAILED:
                            Toast.makeText(MainActivity.this, "Failed " + reason, Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            Toast.makeText(MainActivity.this, "Paused " + reason, Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PENDING:
                            Toast.makeText(MainActivity.this, "Pending", Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            Toast.makeText(MainActivity.this, "Running", Toast.LENGTH_LONG).show();
                            break;
                        default:

                            break;
                    }

                }
            }

        };
        registerReceiver(receiverDownloadComplete,intentFilter);
    }

    private BroadcastReceiver myWifiReceiver
            = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            NetworkInfo networkInfo = (NetworkInfo) arg1.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                //DisplayWifiState();
                Toast.makeText(arg0,"Changed",Toast.LENGTH_SHORT).show();
            }
        }};

    @Override
    protected void onPause(){
        super.onPause();
      //  unregisterReceiver(receiverNotificationClicked);
      //  unregisterReceiver(receiverDownloadComplete);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}


