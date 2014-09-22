package edu.buffalo.cse.cse486586.simpledht.communicator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static edu.buffalo.cse.cse486586.simpledht.constants.Constants.*;
import static edu.buffalo.cse.cse486586.simpledht.constants.Operation.*;
import static edu.buffalo.cse.cse486586.simpledht.utils.Utils.genHash;

public class OperationsHandler {

    private static String TAG = OperationsHandler.class.getName();
    /**
     * - handle new node requests
     * - handle data insertion requests
     * - handle query requests
     * - return query results
     * - handle  deletion requests
     * - handle pointer update requests
     * - handle query about successorPort id requests
     */

    private final ContentResolver contentResolver;
    private final int myPort;
    private int successorPort;
    private int predecessorPort;

    private final String myId;
    private String successorId;
    private String predecessorId;

    private volatile Map<Integer, Object> receivedQueryData;
    private Integer queryRequestCounter;
    private final Object lock;

    public OperationsHandler(ContentResolver contentResolver, int myPort) {
        this.contentResolver = contentResolver;
        this.myPort = myPort;
        TAG = TAG + myPort;
        myId = genHash(myPort / 2);
        receivedQueryData = new HashMap<Integer, Object>();
        queryRequestCounter = 0;
        joinTheChord();
        lock = new Object();
    }

    private void joinTheChord() {
        setSuccessor(myPort, myId);
        setPredecessor(myPort, myId);

        if (myPort == LEADER_PORT) {
            Log.d(TAG, "leader joining chord");
        } else {
            Log.d(TAG, "sending request to leader for joining chord");
            sendRequestTo(NEW_NODE + DELIMITER + myPort, LEADER_PORT);
        }
    }

    public void handleRequest(String request) {
        final String[] requestTokens = request.split(DELIMITER);

        switch (Integer.parseInt(requestTokens[0])) {
            case INSERT: //receieved insert request // operationId--original requester port--key---value
                Log.d(TAG, "Insert request received");
                handleInsertRequest(requestTokens, request);
                break;
            case DELETE:
                Log.d(TAG, "Delete request received");
                handleDeleteRequest(requestTokens, request); //receieved delete request // operationId--original requester port---key
                break;
            case NEW_NODE:  // operationId---port
                Log.d(TAG, "New node join request received");
                handleNewNodeJoinRequest(Integer.parseInt(requestTokens[1]), request);
                break;
            case QUERY: //operation---requester port --- queryRequestId---key---data
                Log.d(TAG, "Query request/response received");
                handleQueryRequest(requestTokens, request);
                break;
            case NEIGHBOR_UPDATE: // operationId---predecessorPort---successorPort
                Log.d(TAG, "Neighbour update request received");
                handleNeighborUpdateRequest(requestTokens[1], requestTokens[2]);
                break;
        }
    }

    private void handleDeleteRequest(String[] requestTokens, String request) {
        // operationId--original requester port---key
        Log.d(TAG, "Delete request received for key - " + requestTokens[2]);
        if (requestTokens[2].equals(ALL_KEYS)) {
            if (!requestTokens[1].equals(String.valueOf(myPort))) {
                contentResolver.delete(URI, LOCAL_KEYS, null);
                sendRequestTo(request, successorPort);
            } //else do nothing
        } else {
            if (requestTokens[1].equals(String.valueOf(myPort))) {
                Log.w(TAG, "Key not found. Can't be deleted - " + requestTokens[2]);
            } else if (!isInMyScope(requestTokens[2])) {
                Log.d(TAG, "Forwarding delete request for key - " + requestTokens[2]);
                sendRequestTo(request, successorPort);
            } else {
                Log.d(TAG, "Deleting record from myself for key - " + requestTokens[2]);
                contentResolver.delete(URI, requestTokens[2], null);
            }
        }
    }

    private void handleQueryRequest(String[] requestTokens, String requestReceived) {
        //operation---requester port --- queryRequestId---key---data

        if (requestTokens[1].equals(String.valueOf(myPort))) {
            //set value in expected data corresp to request id
            Log.d(TAG, "Received response for query request sent by me");
            synchronized (lock) {
                receivedQueryData.put(Integer.parseInt(requestTokens[2]), Arrays.copyOfRange(requestTokens, 4, requestTokens.length));
                lock.notifyAll();
            }
        } else {

            Log.d(TAG, "Received query sent by " + requestTokens[1] + " for key -" + requestTokens[3]);

            StringBuilder updatedRequest = new StringBuilder();
            updatedRequest.append(requestReceived);

            if (ALL_KEYS.equals(requestTokens[3])) {
                //query own db & attach it to incoming data
                Log.d(TAG, "Querying own db for all keys");
                queryOwnDbForKeyAndAddTo(updatedRequest, LOCAL_KEYS);
                //forward request
                Log.d(TAG, "Forwarding updated request to successor");
                sendRequestTo(updatedRequest.toString(), successorPort);
            } else {
                if (isInMyScope(requestTokens[3])) {
                    Log.d(TAG, "Query request key in my scope.. querying for key - " + requestTokens[3]);
                    queryOwnDbForKeyAndAddTo(updatedRequest, requestTokens[3]);
                    Log.d(TAG, "Returning results to requestor : " + requestTokens[1]);
                    sendRequestTo(updatedRequest.toString(), Integer.parseInt(requestTokens[1]));
                } else {
                    Log.d(TAG, "Query request key NOT in my scope.. Forwarding querying for key - " + requestTokens[3] + " to " + successorPort);
                    sendRequestTo(requestReceived, successorPort);
                }
            }
        }
    }

    private void queryOwnDbForKeyAndAddTo(StringBuilder updatedRequest, String key) {
        Log.d(TAG, "Querying own DB ");
        Cursor sqliteCursor = contentResolver.query(URI, null, key, null, null);
        if (sqliteCursor != null && sqliteCursor.getCount() != 0) {
            int keyIndex = sqliteCursor.getColumnIndex(KEY_FIELD);
            int valueIndex = sqliteCursor.getColumnIndex(VALUE_FIELD);

            sqliteCursor.moveToFirst();
            do {
                updatedRequest.append(DELIMITER).append(sqliteCursor.getString(keyIndex)).append(DELIMITER).append(sqliteCursor.getString(valueIndex));
            } while (sqliteCursor.moveToNext());
        }
    }

    private void handleInsertRequest(String[] requestTokens, String request) {
        Log.d(TAG, "Insert request received for key - " + requestTokens[2]);

        if (requestTokens[1].equals(String.valueOf(myPort))) {
            Log.e(TAG, "Forwarded insert request reached me back! Error!  " +
                    "  key - " + requestTokens[2] +
                    " , hash(Key) -  " + genHash(requestTokens[2]) +
                    " , hash(my id) - " + genHash(myId) +
                    " , hash(predecessor id) - " + genHash(predecessorId) +
                    " , hash(successor id) - " + genHash(successorId));
            return;
        }

        if (!isInMyScope(requestTokens[2])) {
            Log.d(TAG, "Forwarding insert request for key - " + requestTokens[2]);
            sendRequestTo(request, successorPort);
        } else {
            Log.d(TAG, "Inserting in my DB key - " + requestTokens[2]);
            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD, requestTokens[2]);
            contentValues.put(VALUE_FIELD, requestTokens[3]);

            contentResolver.insert(URI, contentValues);
        }
    }

    public Boolean isInMyScope(String key) {
        String hashKey = genHash(key);
        return                     //alone in chord
                (successorId.equals(myId) ||
                        //normal case
                        (isAGreaterThanB(myId, hashKey) && isAGreaterThanB(hashKey, predecessorId)) ||
                        //minus infinity
                        (isALesserThanB(hashKey, myId) && isALesserThanB(hashKey, predecessorId) && isALesserThanB(myId, predecessorId)) ||
                        //plus infinity
                        (isAGreaterThanB(hashKey, predecessorId) && isAGreaterThanB(hashKey, myId) && isALesserThanB(myId, predecessorId)));
    }

    private void handleNeighborUpdateRequest(String predecessorPort, String successorPort) {
        Log.d(TAG, "Updating my predecessor & successor to  " + predecessorPort + " " + successorPort);
        if (!"null".equals(predecessorPort)) {
            setPredecessor(Integer.parseInt(predecessorPort), getIdForPort(predecessorPort));
        }
        if (!"null".equals(successorPort)) {
            setSuccessor(Integer.parseInt(successorPort), getIdForPort(successorPort));
        }
    }

    private void handleNewNodeJoinRequest(int newNodePort, String request) {
        Log.d(TAG, "New node join request received for port - " + newNodePort);
        final String newNodeId = genHash(newNodePort / 2);

        if (successorPort == myPort) {
            //u r alone in chord -> handle urself
            Log.d(TAG, "Notify new node to update their predecessor & successor to " + predecessorPort + " " + successorPort);
            sendRequestTo(createNeigborUpdateRequest(myPort, myPort), newNodePort);

            Log.d(TAG, "Adding second node to the chord - " + newNodePort);
            setPredecessor(newNodePort, newNodeId);
            setSuccessor(newNodePort, newNodeId);
        } else if (isInMyScope(String.valueOf(newNodePort/2))) {

            Log.d(TAG, "My new predecessor joined " + newNodePort);

            Log.d(TAG, "Notify new node to update their predecessor & successor to " + predecessorPort + " " + myPort);
            sendRequestTo(createNeigborUpdateRequest(predecessorPort, myPort), newNodePort);

            Log.d(TAG, "Notify old predecessor to update their predecessor & successor to " + " null" + newNodePort);
            sendRequestTo(createNeigborUpdateRequest(null, newNodePort), predecessorPort);

            setPredecessor(newNodePort, newNodeId);
        } else {
            //forward request
            Log.d(TAG, "Not my predecessor.. forwarding new node join request to " + successorPort);
            sendRequestTo(request, successorPort);
        }
    }


    private String createNeigborUpdateRequest(Integer predecessorPort, Integer successorPort) {
        return NEIGHBOR_UPDATE + DELIMITER + predecessorPort + DELIMITER + successorPort;
    }

    private void sendRequestTo(String request, int destinationPort) {
        new ClientTask(destinationPort, request).start();
//        new ClientTask(destinationPort).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request);
    }

    private boolean isAGreaterThanB(String a, String b) {
        return a.compareTo(b) > 0;
    }

    private boolean isALesserThanB(String a, String b) {
        return a.compareTo(b) < 0;
    }

    private void setSuccessor(int port, String id) {
        this.successorPort = port;
        this.successorId = id;
    }

    private void setPredecessor(int port, String id) {
        this.predecessorPort = port;
        this.predecessorId = id;
    }

    /**
     * **METHODS TO BE CALLED BY CONTENT PROVIDER*****
     */

    public void forwardInsertRequest(Object key, Object value, String requesterPort) {
        Log.d(TAG, "Forwarding insert request for key - " + key);
        String request = createGenericRequest(INSERT, requesterPort, key, value);
        sendRequestTo(request, successorPort);
    }

    public void forwardDeleteRequest(String key, String requesterPort) {
        if (successorPort == myPort) {
            Log.w(TAG, "Could not find delete key - " + key);
            return;
        }

        Log.d(TAG, "Forwarding delete request for key - " + key);
        String request = createGenericRequest(DELETE, requesterPort, key);
        sendRequestTo(request, successorPort);
    }

    //only gets requests for  * and keys in other avds
    public Cursor forwardQueryRequest(String key, String requesterPort) {
        Log.d(TAG, "Forwarding query request for key - " + key);
        MatrixCursor cursor = new MatrixCursor(new String[]{KEY_FIELD, VALUE_FIELD});

        Integer queryRequestId = forwardRequestIfThereIsASuccessor(key, requesterPort);
        if (ALL_KEYS.equals(key)) {
            queryYourselfMeanwhile(cursor);
        }
        if (successorPort == myPort) {
            if (cursor.getCount() == 0) {
                return null;
            }
            return cursor;
        }
        waitForResults(key, queryRequestId);
        Log.d(TAG, "Collating query results for query request");
        populateResultsIntoACursor(queryRequestId, cursor);
        return cursor;
    }

    private void waitForResults(String key, Integer queryRequestId) {
        Log.d(TAG, "Waiting for query results for " + key);
        synchronized (lock) {
            try {
                boolean notUpdated = AWAITED.equals(receivedQueryData.get(queryRequestId));
                if (notUpdated) {
                    lock.wait();
                }
                lock.notifyAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Done waiting.. received response!");
    }

    private void queryYourselfMeanwhile(MatrixCursor cursor) {
        Log.d(TAG, "Querying @ for self");
        Cursor sqliteCursor = contentResolver.query(URI, null, LOCAL_KEYS, null, null);

        if (sqliteCursor != null && sqliteCursor.getCount() != 0) {
            int keyIndex = sqliteCursor.getColumnIndex(KEY_FIELD);
            int valueIndex = sqliteCursor.getColumnIndex(VALUE_FIELD);

            sqliteCursor.moveToFirst();
            do {
                cursor.addRow(new String[]{sqliteCursor.getString(keyIndex), sqliteCursor.getString(valueIndex)});
            } while (sqliteCursor.moveToNext());
        }
    }

    private Integer forwardRequestIfThereIsASuccessor(String key, String requesterPort) {
        Integer queryRequestId = null;
        if (successorPort != myPort) {
            //not alone in chord.. can forward the request
            queryRequestId = queryRequestCounter++;
            String request = createGenericRequest(QUERY, requesterPort, queryRequestId, key);
            receivedQueryData.put(queryRequestId, AWAITED);
            Log.d(TAG, "Forwarding Query for " + key);
            sendRequestTo(request, successorPort);
        }
        return queryRequestId;
    }

    private String createGenericRequest(int operation, String requesterPort, Object... valuesToSend) {
        StringBuilder request = new StringBuilder();

        request.append(operation).append(DELIMITER);
        if (requesterPort == null) {
            request.append(myPort);
        } else {
            request.append(requesterPort);
        }
        request.append(DELIMITER);

        for (Object valueToSend : valuesToSend) {
            request.append(valueToSend).append(DELIMITER);
        }

        return request.substring(0, request.length() - DELIMITER.length());
    }

    private void populateResultsIntoACursor(Integer queryRequestId, MatrixCursor cursor) {
        final String[] queryResults = (String[]) receivedQueryData.get(queryRequestId);
        receivedQueryData.remove(queryRequestId);

        if (queryResults.length == 0 || "null".equals(queryResults[0])) {
            return;
        }
        addRowsToCursor(queryResults, cursor);
    }

    private void addRowsToCursor(String[] queryResults, MatrixCursor cursor) {
        String[] row;
        for (int i = 0, queryResultsLength = queryResults.length; i < queryResultsLength; i += 2) {
            Log.d(TAG, "Data received : " + queryResults[i] + " ----> " + queryResults[i + 1]);
            row = new String[]{queryResults[i], queryResults[i + 1]};
            cursor.addRow(row);
        }
    }

    private String getIdForPort(String port) {
        Integer p = Integer.parseInt(port);
        return genHash(p / 2);
    }
}