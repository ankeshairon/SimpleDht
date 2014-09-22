package edu.buffalo.cse.cse486586.simpledht.listeners;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import edu.buffalo.cse.cse486586.simpledht.constants.Constants;

public class OnDumpClickListener implements View.OnClickListener {
    private String targetId;
    private static final String TAG = OnDumpClickListener.class.getName();

    private final TextView textView;
    private final ContentResolver contentResolver;
    private final Uri uri;
//    private final ContentValues[] contentValues;

    public OnDumpClickListener(TextView textView, ContentResolver contentResolver, String targetId) {
        this.textView = textView;
        this.contentResolver = contentResolver;
        this.targetId = targetId;
        uri = Constants.URI;
    }

    @Override
    public void onClick(View view) {
        new Print().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Print extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Cursor cursor = contentResolver.query(uri, null, targetId, null, null);

            if (cursor == null || cursor.getCount() == 0) {
                Log.d(TAG, "No entries in database");
                return null;
            }

            cursor.moveToFirst();

            int keyColumnIndex = cursor.getColumnIndex(Constants.KEY_FIELD);
            int valueColumnIndex = cursor.getColumnIndex(Constants.VALUE_FIELD);

            do {
                publishProgress(cursor.getString(keyColumnIndex), " ---> ", cursor.getString(valueColumnIndex), "\n");
            } while (cursor.moveToNext());

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            for (String s : strings) {
                textView.append(s);
            }
        }
    }
}
