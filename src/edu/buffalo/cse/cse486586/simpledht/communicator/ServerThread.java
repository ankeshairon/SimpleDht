package edu.buffalo.cse.cse486586.simpledht.communicator;

import android.util.Log;
import edu.buffalo.cse.cse486586.simpledht.constants.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {

    private static final String TAG = ServerThread.class.getSimpleName();
    private ServerSocket serverSocket;
    private final OperationsHandler operationsHandler;

    public ServerThread(OperationsHandler operationsHandler) {
        this.operationsHandler = operationsHandler;
        try {
            serverSocket = new ServerSocket(Constants.SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Error! Unable to  create a ServerSocket");
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        BufferedReader bufferedReader = null;
        Socket clientSocket = null;
        String line;

        try {
            while (true) {
                clientSocket = serverSocket.accept();
                Log.d(TAG, "incoming received");
                bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                if ((line = bufferedReader.readLine()) != null) {
                    operationsHandler.handleRequest(line);
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                bufferedReader.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "ServerThread IOException");
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                bufferedReader.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception closing serversocket");
                e.printStackTrace();
            }
        }
    }
}
