package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    private SharedPreferences mPrefs = null;
    private final static String SHARED_PREF_FILENAME = "edu.buffalo.cse.cse486586.groupmessenger1.sharedpref";
    private final static String TAG = GroupMessengerProvider.class.toString();
    private final static String KEY_LAST_SEQNUM = "lastSeq";

    public final static String COLUMN_KEY = "key";
    public final static String COLUMN_VALUE = "value";
    public final static String FROM_REMOTE_AVD = "fromRemoteAvd";
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        Log.v("insert", values.toString());
        //TODO: Hoping that the returned uri does not need to have some sort of ID appended to the original URI, in which case, we might have
        //to modify the logic
        assert mPrefs != null;
        String key = values.getAsString(COLUMN_KEY);
        String value = values.getAsString(COLUMN_VALUE);
        boolean isMessageFromAVD = false;
        if(key.equals(FROM_REMOTE_AVD)) {   //An AVD sent a message(part 2). THis ensures that part 1 & grading is not disturbed
            isMessageFromAVD = true;
            key = getNextSeqNumber();
        }
        Log.d(TAG,"Inserting: Key:"+key+", Value:"+value);
        /*boolean status = */mPrefs.edit().putString(key,value).apply();
//        assert status;
        if(isMessageFromAVD) {   //Do this only for messags sent via avd (not messages part of PTest)
            storeLastSeqNumber(key);
        }
        return uri;
    }

    //TODO: thread safety?!

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        mPrefs = getContext().getSharedPreferences(SHARED_PREF_FILENAME, Context.MODE_PRIVATE);//multiprocess rquired?
        mPrefs.edit().putString(KEY_LAST_SEQNUM,"-1").commit();  //initial sequence number

        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_KEY,COLUMN_VALUE});
        cursor.addRow(new Object[]{selection,mPrefs.getString(selection,null)});
        return cursor;

    }
    //TODO: replace commit with apply? safe to do so?

    private void storeLastSeqNumber(String lastSeqNum) {
        mPrefs.edit().putString(KEY_LAST_SEQNUM,lastSeqNum).commit();
    }

    private String getNextSeqNumber() {
        int seqNum = Integer.valueOf(mPrefs.getString(KEY_LAST_SEQNUM,"-1"));
        return String.valueOf((seqNum+1));
    }
}
