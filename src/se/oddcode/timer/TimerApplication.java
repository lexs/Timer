package se.oddcode.timer;

import se.oddcode.timer.util.AsyncQueryManager;
import android.app.Application;
import android.os.StrictMode;

public class TimerApplication extends Application {
	private static AsyncQueryManager queryManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		queryManager = new AsyncQueryManager(this);
		
		if (true) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectAll()
				.build());
			
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectAll()
				.build());
		}
	}
	
	public static AsyncQueryManager query() {
		return queryManager;
	}
}
