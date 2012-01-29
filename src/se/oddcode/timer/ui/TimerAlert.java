package se.oddcode.timer.ui;

import se.oddcode.timer.R;
import se.oddcode.timer.Timers;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Full screen alarm alert: pops visible indicator and plays alarm tone. This
 * activity shows the alert as a dialog.
 */
public class TimerAlert extends TimerAlertFullscreen {
	private static final String TAG = "TimerAlert";
	
    // If we try to check the keyguard more than 5 times, just launch the full
    // screen activity.
    private int mKeyguardRetryCount;
    private final int MAX_KEYGUARD_CHECKS = 5;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleScreenOff((KeyguardManager) msg.obj);
        }
    };

    private final BroadcastReceiver mScreenOffReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    KeyguardManager km =
                            (KeyguardManager) context.getSystemService(
                            Context.KEYGUARD_SERVICE);
                    handleScreenOff(km);
                }
            };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Listen for the screen turning off so that when the screen comes back
        // on, the user does not need to unlock the phone to dismiss the alarm.
        registerReceiver(mScreenOffReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOffReceiver);
        // Remove any of the keyguard messages just in case
        mHandler.removeMessages(0);
    }

    @Override
    public void onBackPressed() {
    	// Pressing back is the same as dismissing the alert
    	dismiss();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.timer_alert;
    }
    
    private boolean checkRetryCount() {
        if (mKeyguardRetryCount++ >= MAX_KEYGUARD_CHECKS) {
            Log.e(TAG, "Tried to read keyguard status too many times, bailing...");
            return false;
        }
        return true;
    }

    private void handleScreenOff(final KeyguardManager km) {
        if (!km.inKeyguardRestrictedInputMode() && checkRetryCount()) {
            if (checkRetryCount()) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(0, km), 500);
            }
        } else {
            // Launch the full screen activity but do not turn the screen on.
            Intent i = new Intent(this, TimerAlertFullscreen.class);
            i.putExtra(Timers.EXTRA_TIMER, getTimer());
            i.putExtra(SCREEN_OFF, true);
            startActivity(i);
            finish();
        }
    }
}
