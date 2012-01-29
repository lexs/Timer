package se.oddcode.timer;

import android.content.Context;
import android.os.PowerManager;

public class TimerAlertWakeLock {
	private static final String TAG = "TimerAlertWakeLock";

	private static PowerManager.WakeLock cpuWakeLock;

	public static PowerManager.WakeLock createPartialWakeLock(Context context) {
		PowerManager pm = (PowerManager) context .getSystemService(Context.POWER_SERVICE);
		
		return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	}

	public static void acquireCpuWakeLock(Context context) {
		if (cpuWakeLock != null) {
			return;
		}

		cpuWakeLock = createPartialWakeLock(context);
		cpuWakeLock.acquire();
	}

	public static void releaseCpuLock() {
		if (cpuWakeLock != null) {
			cpuWakeLock.release();
			cpuWakeLock = null;
		}
	}
}
