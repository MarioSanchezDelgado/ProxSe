// IProxyCallback.aidl
package com.mlsd.proxse;

// Declare any non-default types here with import statements

interface IProxyCallback {
    oneway void getProxyData(IBinder callback);
}
