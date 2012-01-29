package se.oddcode.timer.ui;

import se.oddcode.timer.R;
import se.oddcode.timer.TimerApplication;
import se.oddcode.timer.TimerContract;
import se.oddcode.timer.TimerContract.Columns;
import se.oddcode.timer.service.TimerService;
import se.oddcode.timer.util.AsyncQueryManager.DeleteListener;
import se.oddcode.timer.view.CountdownView;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class TimerListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	private SimpleCursorAdapter adapter;
	
	private View addTimer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_timer_list, container, false);
		
		addTimer = inflater.inflate(R.layout.item_add_timer, null);
		
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		getListView().addHeaderView(addTimer);
		
		adapter = new SimpleCursorAdapter(getActivity(), R.layout.item_timer, null,
				new String[] { Columns.END_TIME, Columns.LABEL }, new int[] { R.id.time_left, R.id.label }, 0);
		adapter.setViewBinder(new ViewBinder());
		setListAdapter(adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.menu_main, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_cancel_all:
				TimerApplication.query().delete(TimerContract.CONTENT_URI, null, null, new DeleteListener() {
					@Override
					public void onDeleteComplete(int result) {
						// Update timers when it's completed
						Intent intent = new Intent(TimerService.ACTION_UPDATE_TIMERS, null, getActivity(), TimerService.class);
						getActivity().startService(intent);
					}
				});
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		
		return true;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (position == 0) {
			// Add timer was clicked
			Intent intent = new Intent(getActivity(), AddTimerActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), TimerContract.CONTENT_URI, null, null, null, TimerContract.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		adapter.swapCursor(c);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> c) {
		adapter.swapCursor(null);
	}
	
	private static class ViewBinder implements SimpleCursorAdapter.ViewBinder {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (view.getId() != R.id.time_left) {
				return false;
			}
			
			CountdownView timeView = (CountdownView) view;
			long endTime = cursor.getLong(columnIndex);
			
			timeView.setEndTime(endTime);
			
			return true;
		}

		
	}
}
