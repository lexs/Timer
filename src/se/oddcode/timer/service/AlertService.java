package se.oddcode.timer.service;

import se.oddcode.timer.Timer;
import se.oddcode.timer.TimerAlertWakeLock;
import se.oddcode.timer.Timers;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

public class AlertService extends Service {
	private static final String TAG = "AlertService";
	
	private static final int ALERT_TIMEOUT = 10 * 60 * 1000; // 10 minutes
	private static final long[] VIBRATE_PATTERN = new long[] { 500, 500 };
	private static final int WHAT_KILLER = 1000;
	
	private Vibrator vibrator;
	private KillerHandler killerHandler;
	
	private Timer currentTimer;
	
	private Ringtone currentRingtone;

	@Override
	public void onCreate() {
		super.onCreate();
		
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		killerHandler = new KillerHandler();
		
		TimerAlertWakeLock.acquireCpuWakeLock(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		stop();
		
		TimerAlertWakeLock.releaseCpuLock();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// No intent, tell the system not to restart us.
		if (intent == null) {
			stopSelf();
			return START_NOT_STICKY;
		}

		Timer timer = intent.getParcelableExtra(Timers.EXTRA_TIMER);

		if (timer == null) {
			Log.wtf(TAG, "AlertService failed to parse the alarm from the intent");
			stopSelf();
			return START_NOT_STICKY;
		}

		if (currentTimer != null) {
			sendKilledBroadcast(currentTimer);
		}

		play(timer);
		currentTimer = timer;
		// Record the initial call state here so that the new alarm has the
		// newest state.
		//mInitialCallState = mTelephonyManager.getCallState();

		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private void play(Timer timer) {
		// Stop anything that is currently running
		stop();
		
		Uri alertUri = timer.getAlert();
		if (alertUri == null) {
			alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		}
		
		/*ringtonePlaying = RingtoneManager.getRingtone(this, alertUri);
		ringtonePlaying.setStreamType(AudioManager.STREAM_ALARM);
		ringtonePlaying.play();*/
		new PlayTask().execute(alertUri);
		
		if (timer.shouldVibrate()) {
			vibrator.vibrate(VIBRATE_PATTERN, 0);
		} else {
			vibrator.cancel();
		}
		
		enableKiller(timer);
	}
	
	private void stop() {
		if (currentRingtone != null) {
			currentRingtone.stop();
			currentRingtone = null;
			
			vibrator.cancel();
		}
		
		disableKiller();
	}
	
	private void sendKilledBroadcast(Timer timer) {
		Timers.sendKilledBroadcast(this, timer);
	}
	
	private void enableKiller(Timer timer) {
		killerHandler.sendMessageDelayed(killerHandler.obtainMessage(WHAT_KILLER, timer), ALERT_TIMEOUT);
	}
	
	private void disableKiller() {
		killerHandler.removeMessages(WHAT_KILLER);
	}
	
	private class KillerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == WHAT_KILLER) {
				Log.d(TAG, "Alert killer triggered");
				
				sendKilledBroadcast((Timer) msg.obj);
				stopSelf();
			}
		}
	}

	private class PlayTask extends AsyncTask<Uri, Void, Ringtone> {
		@Override
		protected Ringtone doInBackground(Uri... params) {
			Uri alert = params[0];
			
			Ringtone ringtone = RingtoneManager.getRingtone(AlertService.this, alert);
			ringtone.setStreamType(AudioManager.STREAM_ALARM);
			ringtone.play();
			
			return ringtone;
		}

		@Override
		protected void onPostExecute(Ringtone result) {
			currentRingtone = result;
			/*if (currentTimer.shouldVibrate()) {
				vibrator.vibrate(VIBRATE_PATTERN, 0);
			} else {
				vibrator.cancel();
			}
			
			enableKiller(currentTimer);*/
		}

		@Override
		protected void onCancelled(Ringtone result) {
			result.stop();
		}
	}
}
