package se.oddcode.timer.service;

import java.util.ArrayList;
import java.util.List;

import se.oddcode.timer.R;
import se.oddcode.timer.Timer;
import se.oddcode.timer.TimerContract;
import se.oddcode.timer.TimerContract.Columns;
import se.oddcode.timer.Timers;
import se.oddcode.timer.ui.MainActivity;
import se.oddcode.timer.util.TimeUtils;
import se.oddcode.timer.util.TimeUtils.Time;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class TimerService extends IntentService {
	private static final String TAG = "TimerService";
	
	private static final int NOTIFICATION_ID = -1;
	
	public static final String ACTION_UPDATE_TIMERS = "update_timers";
	private static final String ACTION_UPDATE_NOTIFICATION = "update_notification";
	
	private static final String TIMER_SELECTION = Columns.END_TIME + " > ?";
	
	private AlarmManager alarmManager;
	private NotificationManager notificationManager;
	
	public TimerService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		
		if (ACTION_UPDATE_TIMERS.equals(action)) {
			updateTimers();
		} else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
			Timer timer = Timers.getRawTimer(intent);
			
			notifyOne(timer);
			scheduleNextUpdate(timer);
		}
	}

	private void updateTimers() {
		Cursor c = getContentResolver().query(TimerContract.CONTENT_URI, null, TIMER_SELECTION,
				new String[] { String.valueOf(System.currentTimeMillis()) }, null);
		
		List<Timer> timers = toList(c);
		c.close();
		
		Log.d(TAG, timers.size() + " timer(s) running");
		
		// Cancel possible notification updates
		alarmManager.cancel(getUpdateNotificationIntent(null));
		
		int count = timers.size();
		if (count == 1) {
			Timer timer = timers.get(0);

			notifyOne(timer);
			scheduleNextUpdate(timer);
		} else if (count > 1)  {
			notifyMany(timers);
		} else {
			notificationManager.cancel(NOTIFICATION_ID);
		}
		
		for (Timer timer : timers) {
			setExpireAlarm(timer);
		}
	}
	
	private List<Timer> toList(Cursor c) {
		ArrayList<Timer> list = new ArrayList<Timer>(c.getCount());
		
		while (c.moveToNext()) {
			Timer timer = Timer.fromCursor(c);
			
			if (!timer.isExpired()) {
				list.add(timer);
			}
		}
		
		return list;
	}
	
	private void notifyMany(List<Timer> timers) {
		int count = timers.size();
		
		// Timers are sorted with latest first
		long when = timers.get(0).getEndTime();
		
		String text = getString(R.string.notification_text_multiple, count);
		
		Notification notification = getNotification(when, null, text);
		notificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	private void notifyOne(Timer timer) {
		long duration = timer.timeLeft();
		
		Time time = TimeUtils.toTime(duration);
		int hours = time.getHours();
		int minutes = time.getMinutes();
		//int minutes = (int) ((duration / (1000*60)) % 60);
		//int hours   = (int) ((duration / (1000*60*60)) % 24);
		//int seconds = (int) (duration / 1000) % 60;
		
		String text = null;
		
		if (hours > 0) {
			text = getResources().getQuantityString(R.plurals.notification_text_hour, hours, hours);
		} else {
			if (minutes == 0) {
				text = getString(R.string.notification_text_0_minute);
			} else {
				text = getResources().getQuantityString(R.plurals.notification_text_minute, minutes, minutes);
			}
		}
		
		String title = null;
		if (!TextUtils.isEmpty(timer.getLabel())) {
			title = timer.getLabel();
		}
		
		Notification notification = getNotification(timer.getEndTime(), title, text);
		notificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	/*private Notification.Builder getBaseBuilder() {
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
		
		return new Notification.Builder(this)
			.setSmallIcon(R.drawable.stat_timer_v2)
			.setContentIntent(contentIntent)
			.setContentTitle(getString(R.string.notification_title))
			.setOngoing(true)
			.setTicker(getString(R.string.notification_ticker))
			.setWhen(System.currentTimeMillis());
	}*/
	
	private Notification getNotification(long when, String title, String text) {
		if (title == null) {
			title = getString(R.string.notification_title);
		}
		
		Notification n = new Notification(R.drawable.stat_notify_timer, getString(R.string.notification_ticker), when);
		
		n.flags |= Notification.FLAG_ONGOING_EVENT;
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
		n.setLatestEventInfo(this, title, text, contentIntent);
		
		return n;
	}
	
	private void scheduleNextUpdate(Timer timer) {
		long duration = timer.timeLeft();
		
		Time time = TimeUtils.toTime(duration);
		int hours = time.getHours();
		int minutes = time.getMinutes();
		int seconds = time.getSeconds();
		
		//int minutes = (int) ((duration / (1000*60)) % 60);
		//int hours   = (int) ((duration / (1000*60*60)) % 24);
		//int seconds = (int) (duration / 1000) % 60;
		
		if (hours == 0 && minutes == 0) {
			Log.d(TAG, "Timer expires in less than 60 seconds, no need to schedule update");
			return;
		}
		
		if (hours != 0) {
			seconds += minutes * 60;
		}
		
		
		PendingIntent intent = getUpdateNotificationIntent(timer);
		
		// We should get a new minute when seconds is zero
		// Make sure seconds is not 0
		seconds += 1;
		
		long trigger = System.currentTimeMillis() + seconds * 1000;
		alarmManager.set(AlarmManager.RTC, trigger, intent);
		
		Log.d(TAG, "Sceduled an update for timer " + timer.getId() + " in " + seconds + " seconds");
	}
	
	private PendingIntent getUpdateNotificationIntent(Timer timer) {
		Intent intent = new Intent(TimerService.ACTION_UPDATE_NOTIFICATION, null, this, TimerService.class);
		
		int flags = 0;
		
		if (timer != null) {
			Timers.putRawTimer(intent, timer);
		} else {
			flags = PendingIntent.FLAG_NO_CREATE;
		}
		
		return PendingIntent.getService(this, 0, intent, flags);
	}
	
	private void setExpireAlarm(Timer timer) {
		if (timer.isExpired()) {
			return;
		}
		
		Intent intent = new Intent(Timers.ACTION_TIMER_EXPIRED, timer.getUri(), this, TimerReceiver.class);
		Timers.putRawTimer(intent, timer);
		
		PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, 0);
		alarmManager.set(AlarmManager.RTC_WAKEUP, timer.getEndTime(), pending);
	}
	
	private void deleteExpiredAlarms() {
		int removed = getContentResolver().delete(TimerContract.CONTENT_URI, Columns.END_TIME + " < ?",
				new String[] { String.valueOf(System.currentTimeMillis()) });
		
		Log.d(TAG, "Removed " + removed + " expired alarms");
	}
}
