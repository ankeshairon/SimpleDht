package edu.buffalo.cse.cse486586.simpledht.communicator;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientTask extends Thread {
//public class ClientTask extends AsyncTask<String, Void, Void> {
    private static final String TAG = ClientTask.class.getSimpleName();
    private final int remotePort;
    private final String request;

    public ClientTask(int remotePort, String request) {
//    public ClientTask(int remotePort) {
        this.remotePort = remotePort;
        this.request = request;
    }

    @Override
//    public Void doInBackground(String...msgs) {
    public void run() {
        Socket socket;
        PrintWriter writer;
        try {
            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
            socket.getOutputStream();
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(request);
//            writer.println(msgs[0]);
            Log.d(TAG, "Request sent");
            if (!socket.isClosed()) {
                socket.close();
            }
            writer.close();
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException. Sending interrupted!");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "ClientTask socket IOException. Request not sent : " + request);
//            Log.e(TAG, "ClientTask socket IOException. Request not sent : " + msgs[0]);
            e.printStackTrace();
        }
//        return null;
    }
}
