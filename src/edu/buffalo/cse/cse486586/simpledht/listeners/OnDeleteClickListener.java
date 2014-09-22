package edu.buffalo.cse.cse486586.simpledht.listeners;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import edu.buffalo.cse.cse486586.simpledht.SimpleDhtActivity;

import static edu.buffalo.cse.cse486586.simpledht.utils.Utils.buildUri;
import static edu.buffalo.cse.cse486586.simpledht.utils.Utils.genHash;

public class OnDeleteClickListener implements OnClickListener {

    private static final String TAG = OnDeleteClickListener.class.getName();
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private final TextView mTextView;
    private final ContentResolver mContentResolver;
    private final Uri mUri;
    private final ContentValues mContentValues;

    private SimpleDhtActivity mainActivity;
    private String lastEntry;

    public OnDeleteClickListener(TextView _tv, ContentResolver _cr, SimpleDhtActivity mainActivity) {
        mTextView = _tv;
        mContentResolver = _cr;
        this.mainActivity = mainActivity;
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
        mContentValues = new ContentValues();
    }

    @Override
    public void onClick(View v) {
        new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Task extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (!mainActivity.hasText()) {
                publishProgress("No text to delete\n");
                return null;
            }
            if (testDelete()) {
                publishProgress("delete success\n");
            } else {
                publishProgress("delete fail\n");
                return null;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (testQuery()) {
                publishProgress("Query success\n");
            } else {
                publishProgress("Query fail\n");
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            mTextView.append(strings[0]);
        }

        private boolean testDelete() {
            try {
                final String dataValue = mainActivity.getEditText();
                final String dataKey = genHash(dataValue);

                lastEntry = dataValue;

                mContentResolver.delete(mUri, dataKey, null);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                return false;
            }

            return true;
        }

        private boolean testQuery() {
            try {

                Cursor resultCursor = mContentResolver.query(mUri, null, genHash(lastEntry), null, null);
                if (!(resultCursor == null || resultCursor.getCount() == 0)) {
                    Log.e(TAG, "Found records for deleted keys");
                    return false;
                }

//                int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
//                int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
//                if (keyIndex == -1 || valueIndex == -1) {
//                    Log.e(TAG, "Wrong columns");
//                    resultCursor.close();
//                    throw new Exception();
//                }
//
//                resultCursor.moveToFirst();
//
//                if (!(resultCursor.isFirst() && resultCursor.isLast())) {
//                    Log.e(TAG, "Wrong number of rows");
//                    resultCursor.close();
//                    throw new Exception();
//                }
//
//                String returnKey = resultCursor.getString(keyIndex);
//                String returnValue = resultCursor.getString(valueIndex);
//                if (!(returnKey.equals(lastEntry.getKey()) && returnValue.equals(lastEntry.getValue()))) {
//                    Log.e(TAG, "(key, value) pairs don't match\n");
//                    resultCursor.close();
//                    throw new Exception();
//                }

                if (resultCursor != null) {
                    resultCursor.close();
                }
//                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }
}
