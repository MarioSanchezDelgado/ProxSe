package com.mlsd.proxse;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import android.util.Log;
import android.net.*;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.lang.reflect.*;
import java.util.List;

public class ProxSeActivity extends AppCompatActivity {
    private final static String TAG = "ProxSeActivity";
    private TextView mInfo;
    private boolean isStarted = false;
    WifiManager wifii;
    DhcpInfo d;

    public String   s_dns1 ;
    public String   s_dns2;
    public String   s_gateway;
    public String   s_ipAddress;
    public String   s_leaseDuration;
    public String   s_netmask;
    public String   s_serverAddress;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInfo = (TextView)findViewById(R.id.info);

        wifii= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Log.d(TAG, "Enabled: " + wifii.isWifiEnabled());
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assert connectivityManager != null;
            Network activeNetwork[] = connectivityManager.getAllNetworks();
            Log.d(TAG, "Length network: " + activeNetwork.length);
            for(int i = 0; i < activeNetwork.length; i++){
                LinkProperties lp =  connectivityManager.getLinkProperties(activeNetwork[i]);
                Log.d(TAG, "lp1: " + lp.getInterfaceName());

                Log.d(TAG, "lp2: " + lp.getLinkAddresses().toString());
            }

        }

        if(wifii == null)
            Log.d(TAG, "Wifi Service not found");

        WifiInfo aa = wifii.getConnectionInfo();
        Log.d(TAG, " aa: " + intToIp(aa.getIpAddress()));

                d=wifii.getDhcpInfo();
        WifiInfo als = wifii.getConnectionInfo();
        Log.d(TAG, "SSID: " + als.getSSID());
        Log.d(TAG, "getIpAddress: " + als.getIpAddress());


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {

                Log.d("permission", "permission denied to SEND_SMS - requesting it");
                String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

                requestPermissions(permissions, 1);

            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            turnOnHotspot();
        }

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if(list.isEmpty())
        {
            Log.e("Connection Setup","Empty list returned");
        }

        for( WifiConfiguration i : list ) {
            Log.d(TAG, "hay algo: " + i.networkId);
            Log.d(TAG, "hay algo: " + i.SSID);
            Log.d(TAG, "hay algo: " + i.BSSID);
            Log.d(TAG, "hay algo: " + i.status);
        }


Log.d(TAG, "CON LA FUNCION " + getIpAddress());
        try {
            reflex();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        //   Log.d(TAG, "CON LA FUNCION2 " +      getipAddress2());
        getipAddress2();
        if(d != null){
            Log.d(TAG, "d not null");
            s_dns1="DNS 1: "+ String.valueOf(d.dns1);
            s_dns2="DNS 2: "+String.valueOf(d.dns2);
            s_gateway="Default Gateway: "+ String.valueOf(d.gateway);
            s_ipAddress="IP Address: "+ String.valueOf(d.ipAddress);
            s_leaseDuration="Lease Time: "+String.valueOf(d.leaseDuration);
            s_netmask="Subnet Mask: "+ String.valueOf(d.netmask);
            s_serverAddress="Server IP: "+ String.valueOf(d.serverAddress);

            Log.d(TAG, "Network Info\n"+s_dns1+"\n"+s_dns2+"\n"+s_gateway+"\n"+s_ipAddress+"\n"+s_leaseDuration+"\n"+s_netmask+"\n"+s_serverAddress);
        }
        else
            Log.d(TAG, "Dhcp info not found");

        Log.d(TAG, " ssid current: " + getCurrentSsid(getApplicationContext()));

        /*Intent intent = new Intent(this, ProxyService.class);
        startService(intent);
        bindService(intent, mProxyConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);*/
    }

    public int getIpAddress() {
        int ipAddress = 0;
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
       // wifiManager.isWifiAwareSupported();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null || wifiInfo.equals("")) {
            return ipAddress;
        } else {
            ipAddress = wifiInfo.getIpAddress();
        }
        return ipAddress;
    }

public void reflex() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    Method getConfigMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
    WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(wifiManager);
    Log.d(TAG, "Haber si funciona: " + wifiConfig.BSSID);
    Log.d(TAG, "Haber si funciona: " + wifiConfig.SSID);
    Log.d(TAG, "Haber si funciona: " + wifiConfig.networkId);
}


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOnHotspot(){
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
       // WifiManager.LocalOnlyHotspotReservation jaja = new WifiManager.LocalOnlyHotspotReservation();

        manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback(){

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.d(TAG, "Wifi Hotspot is on now");
                Log.d(TAG, "RESERV1: " + reservation.getWifiConfiguration().BSSID);
                Log.d(TAG, "RESERV1: " + reservation.getWifiConfiguration().status);
                Log.d(TAG, "RESERV1: " + reservation.getWifiConfiguration().SSID);
                Log.d(TAG, "RESERV1: " + reservation.getWifiConfiguration().networkId);
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.d(TAG, "onStopped: ");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.d(TAG, "onFailed: ");
            }
        },new Handler());
    }

    public static String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null ) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }

    public void getipAddress2(){
        try {
            short i = 0;
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                Log.d(TAG, "Name interface: " + intf.getName());

             //   Log.d(TAG, "Name interface: " + intf.getDisplayName());
                if(intf.getName().equals("wlan0")){
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                    {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                            Log.d(TAG, "HOST NAME DE ESTA: " + inetAddress.getHostAddress() + " | " + i);

                            i++;
                        }
                        //return inetAddress.getHostAddress();
                    }
                }

            }

        }
        catch (SocketException ex)
        {
            Log.e("ServerActivity", ex.toString());
        }
        //return null;
    }
/*
    public void getipAddress2() {
        try {
            List<WifiConfiguration>
        } catch (Exception e) {
            Log.e(TAG, "ERROR: " + e.toString());
        }
    }
    */
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

    public String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ( (i >> 24 ) & 0xFF) ;
        /*return ((i >> 24 ) & 0xFF ) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ( i & 0xFF) ;*/
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
                                Log.d(TAG, "Local proxy is bound on " + port + " | " + mIP);
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
        changeStateService();
    };

    public void changeStateService()
    {
        if(!isStarted){
            Intent intent = new Intent(this, ProxyService.class);
            intent.setAction(ProxyService.ACTION_START_FOREGROUND_SERVICE);
            isStarted = true;
            startService(intent);
            bindService(intent, mProxyConnection,
                    Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
        } else {
            Intent intent = new Intent(this, ProxyService.class);
            intent.setAction(ProxyService.ACTION_STOP_FOREGROUND_SERVICE);
            isStarted = false;
            startService(intent);
            if (mBound) {
                unbindService(mProxyConnection);
            }
            mInfo.setText(null);
        }
    }

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
                        if(isStarted)
                            changeStateService();

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
