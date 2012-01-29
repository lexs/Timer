package se.oddcode.timer;

import se.oddcode.timer.TimerContract.Columns;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class TimerProvider extends ContentProvider {
	private static final String TAG = "TimerProvider";
	
	private static final String TABLE_NAME = "timers";
	
	private static final int TIMERS = 1;
    private static final int TIMERS_ID = 2;
    
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    private DatabaseHelper openHelper;
    
    static {
    	uriMatcher.addURI(TimerContract.CONTENT_AUTHORITY, "timer", TIMERS);
    	uriMatcher.addURI(TimerContract.CONTENT_AUTHORITY, "timer/#", TIMERS_ID);
    }
	
	@Override
	public boolean onCreate() {
		openHelper = new DatabaseHelper(getContext());
		
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		int match = uriMatcher.match(uri);
		switch (match) {
			case TIMERS:
				return "vnd.android.cursor.dir/timers";
			case TIMERS_ID:
				return "vnd.android.cursor.item/timers";
			default:
				throw new IllegalArgumentException("Unknown uri");
		}
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		int match = uriMatcher.match(uri);
		switch (match) {
			case TIMERS_ID:
				long id = ContentUris.parseId(uri);
				qb.appendWhere(Columns._ID + "=");
				qb.appendWhere(String.valueOf(id));
			case TIMERS:
				qb.setTables(TABLE_NAME);
				break;
			default:
				throw new IllegalArgumentException("Unknown uri");
		}
		
		if (sortOrder == null) {
			sortOrder = TimerContract.DEFAULT_SORT_ORDER;
		}
		
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (uriMatcher.match(uri) != TIMERS) {
			throw new IllegalArgumentException("Can't insert to: " + uri);
		}
		
		SQLiteDatabase db = openHelper.getWritableDatabase();
		long id = db.insert(TABLE_NAME, null, values);
		
		if (id < 0) {
			throw new SQLException("Failed to insert row");
		}
		
		notifyChange(TimerContract.CONTENT_URI);
		
		return ContentUris.withAppendedId(TimerContract.CONTENT_URI, id);
	}
	

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (uriMatcher.match(uri) != TIMERS_ID) {
			throw new UnsupportedOperationException("Can not update uri: " + uri);
		}
		
		long id = ContentUris.parseId(uri);
		
		SQLiteDatabase db = openHelper.getWritableDatabase();
		int count = db.update("alarms", values, Columns._ID + "=?", new String[] { String.valueOf(id) });
		
		notifyChange(uri);
		
		return count;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int match = uriMatcher.match(uri);
		switch (match) {
			case TIMERS:
				break;
			case TIMERS_ID:
				long id = ContentUris.parseId(uri);
				selection = Columns._ID + "=?";
				selectionArgs = new String[] { String.valueOf(id) };
				break;
			default:
				throw new IllegalArgumentException("Unknown uri: " + uri);
		}
		
		SQLiteDatabase db = openHelper.getWritableDatabase();
		int count = db.delete(TABLE_NAME, selection, selectionArgs);
		
		notifyChange(uri);
		
		return count;
	}

	private void notifyChange(Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "timers.db";
		private static final int DATABASE_VERSION = 1;
		
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			
			SQLiteDatabase db = getWritableDatabase();
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + "("
					+ Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Columns.END_TIME + " INTEGER,"
					+ Columns.DURATION + " INTEGER,"
					+ Columns.VIBRATE + " INTEGER,"
					+ Columns.LABEL + " TEXT,"
					+ Columns.ALERT + " INTEGER)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "Upgrading database from version " + oldVersion + "to version " + newVersion
				+ ". Which will destroy all old data");
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
	        onCreate(db);
		}

	}
}
