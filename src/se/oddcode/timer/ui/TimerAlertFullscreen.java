package se.oddcode.timer.ui;

import se.oddcode.timer.R;
import se.oddcode.timer.Timer;
import se.oddcode.timer.TimerApplication;
import se.oddcode.timer.Timers;
import se.oddcode.timer.service.AlertService;
import se.oddcode.timer.view.CountdownView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

public class TimerAlertFullscreen extends Activity {
	protected static final String SCREEN_OFF = "screen_off";
	
	private Receiver receiver = new Receiver();
	
	private Timer currentTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		currentTimer = getIntent().getParcelableExtra(Timers.EXTRA_TIMER);
		
		final Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		// Turn on the screen unless we are being launched from the AlarmAlert
		// subclass as a result of the screen turning off.
		if (!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
			win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
					| WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
		}

		updateLayout();
		updateTimer();
		
		IntentFilter filter = new IntentFilter(Timers.ACTION_TIMER_DISMISSED);
		registerReceiver(receiver, filter);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(receiver);
	}

	/**
	 * This is called when a second alarm is triggered while a previous alert
	 * window is still active.
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		currentTimer = intent.getParcelableExtra(Timers.EXTRA_TIMER);

		updateTimer();
	}

	@Override
	public void onBackPressed() {
		// Don't allow back to dismiss. This method is overriden by TimerAlert
		// so that the dialog is dismissed.
		return;
	}

	protected Timer getTimer() {
		return currentTimer;
	}
	
	protected int getLayoutResId() {
        return R.layout.activity_timer_alert_fullscreen;
    }
	
	private void updateLayout() {
		setContentView(getLayoutResId());
		
		View dismiss = findViewById(R.id.dismiss);
		dismiss.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}
	
	private void updateTimer() {
		updateTitle();
		
		CountdownView timerView = (CountdownView) findViewById(R.id.time_left);
		timerView.setEndTime(currentTimer.getEndTime());
	}
	
	private void updateTitle() {
		String label = currentTimer.getLabelOrDefault(this);
        
        setTitle(label);
	}

	protected void dismiss() {
		TimerApplication.query().delete(currentTimer.getUri(), null, null, null);
		
		stopService(new Intent(this, AlertService.class));
		Timers.sendDismissBroadcast(this, currentTimer);
		
		finish();
	}
	
	/**
	 * This receiver will catch other instances of this activity
	 * dismissing the timer
	 */
	private class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Timers.ACTION_TIMER_DISMISSED.equals(action)) {
				Timer timer = intent.getParcelableExtra(Timers.EXTRA_TIMER);
				
				if (currentTimer.equals(timer)) {
					finish();
				}
			}
		}
	}
}
