package com.mlsd.proxse;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.util.Log;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @hide
 */
public class ProxyServer extends Thread {

    private static final String CONNECT = "CONNECT";
    private static final String HTTP_OK = "HTTP/1.1 200 OK\n";

    private static final String TAG = "ProxyServer";

    // HTTP Headers
    private static final String HEADER_CONNECTION = "connection";
    private static final String HEADER_PROXY_CONNECTION = "proxy-connection";

    private ExecutorService threadExecutor;

    public boolean mIsRunning = false;

    private ServerSocket serverSocket;
    private int mPort;
    private String mIP;
    private IProxyDataListener mCallback;
    private Socket clientSocket;

    private class ProxyConnection implements Runnable {
        private Socket connection;


        private ProxyConnection(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                //Log.d(TAG, "se te cae el socket1");
                String requestLine = getLine(connection.getInputStream());
                //Log.d(TAG, "se te cae el socket2");
                String[] splitLine = requestLine.split(" ");
                if (splitLine.length < 3) {
                    //Log.d(TAG, "se te cae el socket23");
                    connection.close();
                    return;
                }

                Log.v(TAG, " -> REQUEST: " + requestLine);

                String requestType = splitLine[0];
                String urlString = splitLine[1];
                String httpVersion = splitLine[2];

                URI url = null;
                String host;
                int port;

                if (requestType.equals(CONNECT)) {
                    String[] hostPortSplit = urlString.split(":");
                    host = hostPortSplit[0];
                    // Use default SSL port if not specified. Parse it otherwise
                    if (hostPortSplit.length < 2) {
                        port = 443;
                    } else {
                        try {
                            port = Integer.parseInt(hostPortSplit[1]);
                        } catch (NumberFormatException nfe) {
                            //Log.d(TAG, "se te cae el socket3");
                            connection.close();
                            return;
                        }
                    }
                    urlString = "Https://" + host + ":" + port;
                } else {
                    try {
                        url = new URI(urlString);
                        host = url.getHost();
                        port = url.getPort();
                        if (port < 0) {
                            port = 80;
                        }
                    } catch (URISyntaxException e) {
                        //Log.d(TAG, "se te cae el socket4");
                        connection.close();
                        return;
                    }
                }

                List<Proxy> list = new ArrayList<>();
                try {
                    Log.d(TAG, "urlString: " + urlString);
                    list = ProxySelector.getDefault().select(new URI(urlString));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                Socket server = null;
                for (Proxy proxy : list) {
                    try {
                        boolean forward = false;
                        if (!proxy.equals(Proxy.NO_PROXY)) {
                            // Only Inets created by PacProxySelector.
                            InetSocketAddress inetSocketAddress =
                                    (InetSocketAddress)proxy.address();
                            server = new Socket(inetSocketAddress.getHostName(),
                                    inetSocketAddress.getPort());
                            if (!"127.0.0.1".equals(InetAddress.getByName(
                                    inetSocketAddress.getHostName()).getHostAddress()) ||
                                    inetSocketAddress.getPort() != ProxyService.PORT) {
                                server = new Socket(inetSocketAddress.getHostName(),
                                        inetSocketAddress.getPort());
                                sendLine(server, requestLine);
                                forward = true;
                            }
                        }
                        if (!forward) {
                            server = new Socket(host, port);
                            if (requestType.equals(CONNECT)) {
                                //Log.v(TAG, " -> CONNECT: " + host + ":" + port);
                                skipToRequestBody(connection);
                                //Log.d(TAG, "se te cae el socket7");
                                // No proxy to respond so we must.
                                sendLine(connection, HTTP_OK);
                                //Log.d(TAG, "se te cae el socket8");

                            } else {
                                // Proxying the request directly to the origin server.
                                Log.v(TAG, " -> DIRECT: " + host + ":" + port);
                                sendAugmentedRequestToHost(connection, server,
                                        requestType, url, httpVersion);
                                //Log.d(TAG, "se te cae el socket9");

                            }
                        }
                    } catch (IOException ioe) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "Unable to connect to proxy " + proxy, ioe);
                        }
                    }
                    if (server != null) {
                        break;
                    }
                }
                if (list.isEmpty()) {
                    server = new Socket(host, port);
                    if (requestType.equals(CONNECT)) {
                        //Log.d(TAG, "se te cae el socket10");

                        skipToRequestBody(connection);
                        //Log.d(TAG, "se te cae el socket11");
                        // No proxy to respond so we must.
                        sendLine(connection, HTTP_OK);
                        //Log.d(TAG, "se te cae el socket12");
                    } else {
                        // Proxying the request directly to the origin server.
                        //Log.d(TAG, "se te cae el socket13");
                        sendAugmentedRequestToHost(connection, server,
                                requestType, url, httpVersion);
                        //Log.d(TAG, "se te cae el socket14");
                    }
                }
                // Pass data back and forth until complete.
                if (server != null) {
                    //Log.d(TAG, "se te cae el socket15");
                    SocketConnect.connect(connection, server);
                    //Log.d(TAG, "se te cae el socket16");
                }
            } catch (Exception e) {
                Log.d(TAG, "Problem Proxying", e);
            }
            try {
                connection.close();
            } catch (IOException ioe) {
                // Do nothing
            }
        }

        /**
         * Sends HTTP request-line (i.e. the first line in the request)
         * that contains absolute path of a given absolute URI.
         *
         * @param server server to send the request to.
         * @param requestType type of the request, a.k.a. HTTP method.
         * @param absoluteUri absolute URI which absolute path should be extracted.
         * @param httpVersion version of HTTP, e.g. HTTP/1.1.
         * @throws IOException if the request-line cannot be sent.
         */
        private void sendRequestLineWithPath(Socket server, String requestType,
                                             URI absoluteUri, String httpVersion) throws IOException {

            String absolutePath = getAbsolutePathFromAbsoluteURI(absoluteUri);
            String outgoingRequestLine = String.format("%s %s %s",
                    requestType, absolutePath, httpVersion);
            sendLine(server, outgoingRequestLine);
        }

        /**
         * Extracts absolute path form a given URI. E.g., passing
         * <code>http://google.com:80/execute?query=cat#top</code>
         * will result in <code>/execute?query=cat#top</code>.
         *
         * @param uri URI which absolute path has to be extracted,
         * @return the absolute path of the URI,
         */
        private String getAbsolutePathFromAbsoluteURI(URI uri) {
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            String rawFragment = uri.getRawFragment();
            StringBuilder absolutePath = new StringBuilder();

            if (rawPath != null) {
                absolutePath.append(rawPath);
            } else {
                absolutePath.append("/");
            }
            if (rawQuery != null) {
                absolutePath.append("?").append(rawQuery);
            }
            if (rawFragment != null) {
                absolutePath.append("#").append(rawFragment);
            }
            return absolutePath.toString();
        }

        private String getLine(InputStream inputStream) throws IOException {
            StringBuilder buffer = new StringBuilder();
            //Log.d(TAG, "Se caera getline 2");
            int byteBuffer = inputStream.read();
            //Log.d(TAG, "Se caera getline 3");
            if (byteBuffer < 0) return "";
            //Log.d(TAG, "Se caera getline 4");
            do {
                //Log.d(TAG, "Se caera getline do 1");
                if (byteBuffer != '\r') {
                    //Log.d(TAG, "Se caera getline do 2");
                    buffer.append((char)byteBuffer);
                }
                //Log.d(TAG, "Se caera getline do 3");
                byteBuffer = inputStream.read();
                //Log.d(TAG, "Se caera getline do 4");
            } while ((byteBuffer != '\n') && (byteBuffer >= 0));

            //Log.d(TAG, "Se caera getline return" + buffer.toString());
            return buffer.toString();
        }

        private void sendLine(Socket socket, String line) throws IOException {
            //Log.d(TAG, "Se caera sendLine " + line);
            OutputStream os = socket.getOutputStream();
            os.write(line.getBytes());
            os.write('\r');
            os.write('\n');
            os.flush();
        }

        /**
         * Reads from socket until an empty line is read which indicates the end of HTTP headers.
         *
         * @param socket socket to read from.
         * @throws IOException if an exception took place during the socket read.
         */
        private void skipToRequestBody(Socket socket) throws IOException {
            while (getLine(socket.getInputStream()).length() != 0);
        }

        /**
         * Sends an augmented request to the final host (DIRECT connection).
         *
         * @param src socket to read HTTP headers from.The socket current position should point
         *            to the beginning of the HTTP header section.
         * @param dst socket to write the augmented request to.
         * @param httpMethod original request http method.
         * @param uri original request absolute URI.
         * @param httpVersion original request http version.
         * @throws IOException if an exception took place during socket reads or writes.
         */
        private void sendAugmentedRequestToHost(Socket src, Socket dst,
                                                String httpMethod, URI uri, String httpVersion) throws IOException {

            sendRequestLineWithPath(dst, httpMethod, uri, httpVersion);
            filterAndForwardRequestHeaders(src, dst);

            // Currently the proxy does not support keep-alive connections; therefore,
            // the proxy has to request the destination server to close the connection
            // after the destination server sent the response.
            sendLine(dst, "Connection: close");

            // Sends and empty line that indicates termination of the header section.
            sendLine(dst, "");
        }

        /**
         * Forwards original request headers filtering out the ones that have to be removed.
         *
         * @param src source socket that contains original request headers.
         * @param dst destination socket to send the filtered headers to.
         * @throws IOException if the data cannot be read from or written to the sockets.
         */
        private void filterAndForwardRequestHeaders(Socket src, Socket dst) throws IOException {
            String line;
            do {
                line = getLine(src.getInputStream());
                if (line.length() > 0 && !shouldRemoveHeaderLine(line)) {
                    sendLine(dst, line);
                }
            } while (line.length() > 0);
        }

        /**
         * Returns true if a given header line has to be removed from the original request.
         *
         * @param line header line that should be analysed.
         * @return true if the header line should be removed and not forwarded to the destination.
         */
        private boolean shouldRemoveHeaderLine(String line) {
            int colIndex = line.indexOf(":");
            if (colIndex != -1) {
                String headerName = line.substring(0, colIndex).trim();
                if (headerName.regionMatches(true, 0, HEADER_CONNECTION, 0,
                        HEADER_CONNECTION.length())
                        || headerName.regionMatches(true, 0, HEADER_PROXY_CONNECTION,
                        0, HEADER_PROXY_CONNECTION.length())) {
                    return true;
                }
            }
            return false;
        }
    }

    public ProxyServer() {
        threadExecutor = Executors.newCachedThreadPool();
        mPort = -1;
        mCallback = null;
    }

    @Override
    public void run() {
        try {
            //InetAddress addr = InetAddress.getByName("127.0.0.1");
            //InetAddress addr = InetAddress.getByName("192.168.43.1");
            //InetAddress addr = InetAddress.getByName("192.168.0.1");
            //serverSocket = new ServerSocket(ProxyService.PORT, 100, addr);
            serverSocket = new ServerSocket(ProxyService.PORT);
            int newport = serverSocket.getLocalPort();
            InetAddress mInetAddress = serverSocket.getInetAddress();


            Log.d(TAG, "queport: " + newport + " " + mIsRunning);
            Log.d(TAG, "1: " + serverSocket.getLocalSocketAddress().toString());
            //Log.d(TAG, "1: " + serverSocket.getLocalSocketAddress().toString());

            setPort( mInetAddress.getHostAddress(), newport);

            while (mIsRunning) {
                try {
                    clientSocket = serverSocket.accept();
                    // Only receive local connections.
                    //if (socket.getInetAddress().isLoopbackAddress()) {
                    Log.d(TAG, "Conecto clientsocket?");
                    if (clientSocket != null) {
                        Log.d(TAG, "Connected to: " + clientSocket.getInetAddress().getHostAddress());
                        //int timeout = clientSocket.getSoTimeout();
                        //Log.d(TAG, "Por entrar a proxy connection" + timeout);
                        ProxyConnection parser = new ProxyConnection(clientSocket);
                        //Log.d(TAG, "Salio a proxy connection");
                        threadExecutor.execute(parser);
                        //Log.d(TAG, "Ejecuto parser");
                    } else {
                        Log.d(TAG, "No contecto");
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "ERROR RUNNING: ", e);
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to start proxy server", e);
        } catch (IOException e1) {
            Log.e(TAG, "Failed to start proxy server", e1);
        }

        mIsRunning = false;
    }

    public synchronized void setPort(String ipAddress, int port) {
        if (mCallback != null) {
            try {
                Log.d(TAG, "Reporting " + ipAddress.toString() + " | " + port);
                mCallback.setProxyData(ipAddress, port);
            } catch (RemoteException e) {
                Log.w(TAG, "Proxy failed to report port to PacManager", e);
            }
        }
        mIP = ipAddress;
        mPort = port;
    }

    public synchronized void setCallback(IProxyDataListener callback) {
        if (mPort != -1 && mIP != null) {
            try {
                callback.setProxyData(mIP, mPort);
            } catch (RemoteException e) {
                Log.w(TAG, "Proxy failed to report port to PacManager", e);
            }
        }
        mCallback = callback;
    }

    public synchronized void startServer() {
        Log.d(TAG, "startServer");
        mIsRunning = true;
        start();
    }

    public synchronized void stopServer() {
        mIsRunning = false;
        if (clientSocket != null) {
            try {
                Log.w(TAG, "Intentando cerrar client socket");
                clientSocket.close();
                clientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverSocket != null) {
            try {
                Log.w(TAG, "Intentando cerrar socket");
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isBound() {
        return (mPort != -1);
    }

    public int getPort() {
        return mPort;
    }
}

