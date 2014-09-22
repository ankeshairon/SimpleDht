package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.util.HashMap;

import static edu.buffalo.cse.cse486586.simpledht.utils.Utils.buildUri;
import static edu.buffalo.cse.cse486586.simpledht.utils.Utils.genHash;

public class OnTestClickListener implements OnClickListener {

    private static final String TAG = OnTestClickListener.class.getName();
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private final TextView mTextView;
    private final ContentResolver mContentResolver;
    private final Uri mUri;
    private final ContentValues mContentValues;

    private SimpleDhtActivity mainActivity;
    private HashMap.SimpleEntry<String, String> lastEntry;

    public OnTestClickListener(TextView _tv, ContentResolver _cr, SimpleDhtActivity mainActivity) {
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
            if(!mainActivity.hasText()){
                publishProgress("No text to insert\n");
                return null;
            }
            if (testInsert()) {
                publishProgress("Insert success\n");
            } else {
                publishProgress("Insert fail\n");
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

        private boolean testInsert() {
            try {
                final String dataValue = mainActivity.getEditText();
                final String dataKey = genHash(dataValue);

                lastEntry = new HashMap.SimpleEntry<String, String>(dataKey, dataValue);

                mContentValues.put(KEY_FIELD, dataKey);
                mContentValues.put(VALUE_FIELD, dataValue);
                mContentResolver.insert(mUri, mContentValues);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                return false;
            }

            return true;
        }

        private boolean testQuery() {
            try {
//                for (int i = 0; i < TEST_CNT; i++) {
//                    String key = (String) mContentValues.get(KEY_FIELD);
//                    String val = (String) mContentValues.get(VALUE_FIELD);

                Cursor resultCursor = mContentResolver.query(mUri, null, lastEntry.getKey(), null, null);
                if (resultCursor == null) {
                    Log.e(TAG, "Result null");
                    throw new Exception();
                }

                int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
                int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
                if (keyIndex == -1 || valueIndex == -1) {
                    Log.e(TAG, "Wrong columns");
                    resultCursor.close();
                    throw new Exception();
                }

                resultCursor.moveToFirst();

                if (!(resultCursor.isFirst() && resultCursor.isLast())) {
                    Log.e(TAG, "Wrong number of rows");
                    resultCursor.close();
                    throw new Exception();
                }

                String returnKey = resultCursor.getString(keyIndex);
                String returnValue = resultCursor.getString(valueIndex);
                if (!(returnKey.equals(lastEntry.getKey()) && returnValue.equals(lastEntry.getValue()))) {
                    Log.e(TAG, "(key, value) pairs don't match\n");
                    resultCursor.close();
                    throw new Exception();
                }

                resultCursor.close();
//                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }
}
