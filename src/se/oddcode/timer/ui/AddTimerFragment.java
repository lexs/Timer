package se.oddcode.timer.ui;

import se.oddcode.timer.R;
import se.oddcode.timer.Timer;
import se.oddcode.timer.TimerApplication;
import se.oddcode.timer.TimerContract;
import se.oddcode.timer.Timers;
import se.oddcode.timer.util.AsyncQueryManager.InsertListener;
import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

public class AddTimerFragment extends Fragment {
	private static final String TAG = "AddTimerFragment";
	
	private static final int REQUEST_RINGTONE = 1;
	
	private NumberPicker hoursPicker;
	private NumberPicker minutesPicker;
	private NumberPicker secondsPicker;
	
	private EditText labelView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_add_timer, container, false);
		
		setupPickers(v);
		
		labelView = (EditText) v.findViewById(R.id.label);
		
		View ringtone = v.findViewById(R.id.ringtone);
		ringtone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
					.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
				startActivityForResult(intent, REQUEST_RINGTONE);
			}
		});
		
		View startButton = v.findViewById(R.id.start);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startTimer();
			}
		});
		
		return v;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUEST_RINGTONE && resultCode == Activity.RESULT_OK) {
			Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			if (ringtone != null) {
				Log.d(TAG, "Got ringtone: " + ringtone);
			}
		}
	}
	
	private void setupPickers(View v) {
		hoursPicker = (NumberPicker) v.findViewById(R.id.hours);
		minutesPicker = (NumberPicker) v.findViewById(R.id.minutes);
		secondsPicker = (NumberPicker) v.findViewById(R.id.seconds);
		
		setupPicker(hoursPicker, 0, 99);
		setupPicker(minutesPicker, 0, 60);
		setupPicker(secondsPicker, 0, 60);
		
		hoursPicker.setFormatter(new Formatter("h"));
		minutesPicker.setFormatter(new Formatter("m"));
		secondsPicker.setFormatter(new Formatter("s"));
	}
	
	private void setupPicker(NumberPicker picker, int min, int max) {
		picker.setMinValue(min);
		picker.setMaxValue(max);
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	private void startTimer() {
		int duration = hoursPicker.getValue() * 60 * 60 + minutesPicker.getValue() * 60 + secondsPicker.getValue();
		
		// Convert to ms
		duration *= 1000;
		
		if (duration == 0) {
			Toast.makeText(getActivity(), R.string.incorrect_time, Toast.LENGTH_SHORT).show();
			return;
		}
		
		String label = labelView.getText().toString();
		
		long endTime = System.currentTimeMillis() + duration;
		
		Timer timer = new Timer(endTime, duration, true, label, null);
		TimerApplication.query().insert(TimerContract.CONTENT_URI, timer.toContentValues(), new InsertListener() {
			@Override
			public void onInsertComplete(Uri uri) {
				Intent intent = new Intent(Timers.ACTION_TIMER_STARTED);
				intent.setPackage(getActivity().getPackageName());
				getActivity().sendBroadcast(intent);
			}
		});

		getActivity().finish();
	}
	
	private static class Formatter implements NumberPicker.Formatter {
		public String postfix;
		
		public Formatter(String postfix) {
			this.postfix = postfix;
		}
		
		@Override
		public String format(int value) {
			return value + postfix;
		}
	}
}
