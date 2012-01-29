package se.oddcode.timer.service;

import se.oddcode.timer.AsyncHandler;
import se.oddcode.timer.R;
import se.oddcode.timer.Timer;
import se.oddcode.timer.TimerAlertWakeLock;
import se.oddcode.timer.Timers;
import se.oddcode.timer.ui.MainActivity;
import se.oddcode.timer.ui.TimerAlert;
import se.oddcode.timer.ui.TimerAlertFullscreen;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class TimerReceiver extends BroadcastReceiver {
	private static final String TAG = "TimerReceiver";
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final PendingResult result = goAsync();
        final WakeLock wl = TimerAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();
        AsyncHandler.post(new Runnable() {
            @Override public void run() {
                handleIntent(context, intent);
                result.finish();
                wl.release();
            }
        });
		//handleIntent(context, intent);
	}
	
	private void handleIntent(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (Timers.ACTION_TIMER_STARTED.equals(action)) {
			updateTimers(context);
		} else if (Timers.ACTION_TIMER_EXPIRED.equals(action)) {
			Log.d(TAG, "Timer expired: " + intent.getDataString());
			Timer timer = Timers.getRawTimer(intent);
			
			if (timer != null) {
				timerExpired(context, timer);
			}
			
			updateTimers(context);
		} else if (Timers.ACTION_TIMER_DISMISSED.equals(action)) {
			Timer timer = intent.getParcelableExtra(Timers.EXTRA_TIMER);
			
			NotificationManager nm = getNotificationManager(context);
			nm.cancel((int) timer.getId());
			
			updateTimers(context);
		} else if (Timers.ACTION_TIMER_KILLED.equals(action)) {
			Timer timer = intent.getParcelableExtra(Timers.EXTRA_TIMER);
			int timeout = intent.getIntExtra(Timers.EXTRA_KILLED_TIMEOUT, 0);
			
			timerSilenced(context, timer, timeout);
			
			updateTimers(context);
		} else {
			Log.w(TAG, "Unknown action: " + action);
		}
	}

	private void updateTimers(Context context) {
		Intent intent = new Intent(TimerService.ACTION_UPDATE_TIMERS, null, context, TimerService.class);
		context.startService(intent);
	}
	
	private void timerExpired(Context context, Timer timer) {
		// Maintain a cpu wake lock until the AlertService can
        // pick it up.
		TimerAlertWakeLock.acquireCpuWakeLock(context);
		
		// Close dialogs and window shade
		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(closeDialogs);

		// Decide which activity to start based on the state of the keyguard.
		Class<?> c = TimerAlert.class;
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		if (km.inKeyguardRestrictedInputMode()) {
			// Use the full screen activity for security.
			c = TimerAlertFullscreen.class;
		}
		
		// Play the alert via AlertService
		Intent alertIntent = new Intent(Timers.ACTION_TIMER_EXPIRED, timer.getUri(), context, AlertService.class);
		alertIntent.putExtra(Timers.EXTRA_TIMER, timer);
		context.startService(alertIntent);
        
		// Trigger a notification that, when clicked, will show the alarm alert
        // dialog. No need to check for fullscreen since this will always be
        // launched from a user action.
        Intent notify = new Intent(Intent.ACTION_VIEW, timer.getUri(), context, TimerAlert.class);
        notify.putExtra(Timers.EXTRA_TIMER, timer);
        PendingIntent pendingNotify = PendingIntent.getActivity(context, 0, notify, 0);

		String label = timer.getLabelOrDefault(context);
		Notification n = new Notification(R.drawable.stat_notify_timer, label,
				timer.getEndTime());
		n.setLatestEventInfo(context, label,
				context.getString(R.string.timer_notifiy_text), pendingNotify);
		n.flags |= Notification.FLAG_SHOW_LIGHTS
				| Notification.FLAG_ONGOING_EVENT;
		n.defaults |= Notification.DEFAULT_LIGHTS;

		// NEW: Embed the full-screen UI here. The notification manager will
		// take care of displaying it if it's OK to do so.
		Intent timerAlert = new Intent(Intent.ACTION_VIEW, timer.getUri(), context, c);
		timerAlert.putExtra(Timers.EXTRA_TIMER, timer);
		timerAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		n.fullScreenIntent = PendingIntent.getActivity(context, 0, timerAlert, 0);

		// Send the notification using the alarm id to easily identify the
		// correct notification.
		NotificationManager nm = getNotificationManager(context);
		nm.notify((int) timer.getId(), n);
	}
	
	private void timerSilenced(Context context, Timer timer, int timeout) {
		PendingIntent intent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
		
		String label = timer.getLabelOrDefault(context);
		String text = context.getString(R.string.notification_text_silenced, timeout);

		Notification n = new Notification(R.drawable.stat_notify_timer, label,
				timer.getEndTime());
		n.setLatestEventInfo(context, label, text, intent);
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		
		// We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain
        // notification.
		NotificationManager nm = getNotificationManager(context);
        nm.cancel((int) timer.getId());
        nm.notify((int) timer.getId(), n);
	}

	private NotificationManager getNotificationManager(Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
}
