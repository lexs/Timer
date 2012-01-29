package se.oddcode.timer;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

public class Timers {
	private static final String TAG = "Timers";
	
	public static final String ACTION_TIMER_STARTED = "se.oddcode.timer.action.TIMER_STARTED";
	public static final String ACTION_UPDATE_NOTIFICATION = "se.oddcode.timer.action.UPDATE_NOTIFICATION";
	public static final String ACTION_TIMER_EXPIRED = "se.oddcode.timer.action.TIMER_EXPIRED";
	public static final String ACTION_TIMER_DISMISSED = "se.oddcode.timer.action.TIMER_DISMISSED";
	public static final String ACTION_TIMER_KILLED = "se.oddcode.timer.action.TIMER_KILLED";
	public static final String EXTRA_TIMER = "timer";
	public static final String EXTRA_TIMER_RAW_DATA = "timer_raw_data";
	public static final String EXTRA_KILLED_TIMEOUT = "killed_timeout";
	
	public static void putRawTimer(Intent intent, Timer timer) {
		// XXX: This is a slight hack to avoid an exception in the remote
        // AlarmManagerService process. The AlarmManager adds extra data to
        // this Intent which causes it to inflate. Since the remote process
        // does not know about the Alarm class, it throws a
        // ClassNotFoundException.
        //
        // To avoid this, we marshall the data ourselves and then parcel a plain
        // byte[] array. The AlarmReceiver class knows to build the Alarm
        // object from the byte[] array.
        Parcel out = Parcel.obtain();
        timer.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(Timers.EXTRA_TIMER_RAW_DATA, out.marshall());
	}
	
	public static Timer getRawTimer(Intent intent) {
		Timer timer = null;
		// Grab the alarm from the intent. Since the remote AlarmManagerService
		// fills in the Intent to add some extra data, it must unparcel the
		// Alarm object. It throws a ClassNotFoundException when unparcelling.
		// To avoid this, do the marshalling ourselves.
		final byte[] data = intent.getByteArrayExtra(EXTRA_TIMER_RAW_DATA);
		if (data != null) {
			Parcel in = Parcel.obtain();
			in.unmarshall(data, 0, data.length);
			in.setDataPosition(0);
			timer = Timer.CREATOR.createFromParcel(in);
		}

		if (timer == null) {
			Log.wtf(TAG, "Failed to parse the timer from the intent");
		}
		
		return timer;
	}
	
	public static void sendDismissBroadcast(Context context, Timer timer) {
		Intent intent = new Intent(Timers.ACTION_TIMER_DISMISSED);
		intent.putExtra(Timers.EXTRA_TIMER, timer);
		intent.setPackage(context.getPackageName());
		
		context.sendBroadcast(intent);
	}
	
	public static void sendKilledBroadcast(Context context, Timer timer) {
		long millis = System.currentTimeMillis() - timer.getEndTime();
        int minutes = (int) Math.round(millis / 60000.0);
		
		Intent intent = new Intent(Timers.ACTION_TIMER_KILLED);
		intent.putExtra(Timers.EXTRA_TIMER, timer);
		intent.putExtra(EXTRA_KILLED_TIMEOUT, minutes);
		intent.setPackage(context.getPackageName());
		
		context.sendBroadcast(intent);
	}
	
	private Timers() {}
}
