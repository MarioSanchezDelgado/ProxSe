package com.mlsd.proxse;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import android.util.Log;

public class ProxSeActivity extends AppCompatActivity {
    private final static String TAG = "ProxSeActivity";
    private TextView mInfo;
    private boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInfo = (TextView)findViewById(R.id.info);

        /*Intent intent = new Intent(this, ProxyService.class);
        startService(intent);*/
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, ProxyService.class);
        bindService(intent, mProxyConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBound) {
            unbindService(mProxyConnection);
        }
    }

    private boolean mBound;
    private ServiceConnection mProxyConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName component) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName component, IBinder binder) {
            IProxyCallback callbackService = IProxyCallback.Stub.asInterface(binder);
            if (callbackService != null) {
                try {
                    callbackService.getProxyData(new IProxyDataListener.Stub() {
                        @Override
                        public void setProxyData(final String mIP, final int port) throws RemoteException {
                            if (port != -1) {
                                Log.d(TAG, "Local proxy is bound on " + port);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //mInfo.setText("http://127.0.0.1:" + port);
                                        mInfo.setText(mIP + " : " + port);
                                    }
                                });
                            } else {
                                Log.e(TAG, "Received invalid port from Local Proxy,"
                                        + " PAC will not be operational");
                            }
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mBound = true;
        }
    };

    public void startStopHandler(View view) {
        if(!isStarted){
            Intent intent = new Intent(this, ProxyService.class);
            intent.setAction(ProxyService.ACTION_START_FOREGROUND_SERVICE);
            isStarted = true;
            startService(intent);
        } else {
            Intent intent = new Intent(this, ProxyService.class);
            intent.setAction(ProxyService.ACTION_STOP_FOREGROUND_SERVICE);
            isStarted = false;
            startService(intent);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                Intent intent = new Intent(this, ProxyService.class);
                intent.setAction(ProxyService.ACTION_STOP_FOREGROUND_SERVICE);
                isStarted = false;
                startService(intent);

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Exit");
                builder.setMessage("Do you want to exit?");
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i){
                        dialogInterface.dismiss();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i){
                        finishAndRemoveTask();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }
}
