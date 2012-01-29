package se.oddcode.timer.util;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AsyncQueryManager {
	private QueryHandler queryHandler;
	
	public interface InsertListener {
		void onInsertComplete(Uri uri);
	}
	
	public interface DeleteListener {
		void onDeleteComplete(int result);
	}
	
	public interface QueryListener {
		void onQueryComplete(Cursor c);
	}
	
	public interface UpdateListener {
		void onUpdateComplete(int result);
	}
	
	public AsyncQueryManager(Context context) {
		queryHandler = new QueryHandler(context.getContentResolver());
	}
	
	public void insert(Uri uri, ContentValues initialValues, InsertListener listener) {
		queryHandler.startInsert(0, listener, uri, initialValues);
	}
	
	public void delete(Uri uri, String selection, String[] selectionArgs, DeleteListener listener) {
		queryHandler.startDelete(0, listener, uri, selection, selectionArgs);
	}
	
	public void query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy, QueryListener listener) {
		queryHandler.startQuery(0, listener, uri, projection, selection, selectionArgs, orderBy);
	}
	
	public void update(Uri uri, ContentValues values, String selection, String[] selectionArgs, UpdateListener listener) {
		queryHandler.startUpdate(0, listener, uri, values, selection, selectionArgs);
	}
	
	private static class QueryHandler extends AsyncQueryHandler {
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			if (cookie != null) {
				DeleteListener listener = (DeleteListener) cookie;
				
				listener.onDeleteComplete(result);
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			if (cookie != null) {
				InsertListener listener = (InsertListener) cookie;
				
				listener.onInsertComplete(uri);
			}
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if (cookie != null) {
				QueryListener listener = (QueryListener) cookie;
				
				listener.onQueryComplete(cursor);
			}
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			if (cookie != null) {
				UpdateListener listener = (UpdateListener) cookie;
				
				listener.onUpdateComplete(result);
			}
		}
	}
}
