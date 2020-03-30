// IProxyDataListener.aidl
package com.mlsd.proxse;

// Declare any non-default types here with import statements

interface IProxyDataListener {
        oneway void setProxyData(String mIP, int port);
}
