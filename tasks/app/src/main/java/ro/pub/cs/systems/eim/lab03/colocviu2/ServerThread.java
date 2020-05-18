package ro.pub.cs.systems.eim.lab03.colocviu2;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.*;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class ServerThread extends Thread {

    private int port = 0;
    private ServerSocket serverSocket = null;

    private Double eur = null;
    private Double usd = null;

    public ServerThread(int port) {
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        }
        this.eur = 0.0;
        this.usd = 0.0;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void sync() throws JSONException, IOException {
        Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
        HttpClient httpClient = new DefaultHttpClient();
        String pageSourceCode = "";
        HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS);
        HttpResponse httpGetResponse = httpClient.execute(httpGet);
        HttpEntity httpGetEntity = httpGetResponse.getEntity();
        if (httpGetEntity != null) {
            pageSourceCode = EntityUtils.toString(httpGetEntity);
        }

        if (pageSourceCode == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
            return;
        } else
            Log.i(Constants.TAG, pageSourceCode);

            JSONObject content = new JSONObject(pageSourceCode);

            JSONObject bpi = content.getJSONObject("bpi");

            Double usd_rate = bpi.getJSONObject("USD").getDouble("rate_float");
            Double eur_rate = bpi.getJSONObject("EUR").getDouble("rate_float");

            setData(eur_rate, usd_rate);
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public synchronized void setData(Double eur, Double usd) {
        this.eur = eur;
        this.usd = usd;
    }

    public synchronized Double getEur() {
        return eur;
    }

    public synchronized Double getUsd() {
        return usd;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Log.i(Constants.TAG, "[SERVER THREAD] Waiting for a client invocation...");
                Socket socket = serverSocket.accept();
                Log.i(Constants.TAG, "[SERVER THREAD] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());
                CommunicationThread communicationThread = new CommunicationThread(this, socket);
                communicationThread.start();
            }
        } catch (Exception clientProtocolException) {
            Log.e(Constants.TAG, "[SERVER THREAD] An exception has occurred: " + clientProtocolException.getMessage());
            if (Constants.DEBUG) {
                clientProtocolException.printStackTrace();
            }
        }
    }

    public void stopThread() {
        interrupt();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[SERVER THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}
