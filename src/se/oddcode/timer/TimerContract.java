package se.oddcode.timer;

import android.net.Uri;
import android.provider.BaseColumns;

public class TimerContract {
	public static final String CONTENT_AUTHORITY = "se.oddcode.timer";
	public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/timer");
	
	public static final String DEFAULT_SORT_ORDER = Columns.END_TIME + " ASC";
	
	public interface Columns extends BaseColumns {
		static final String END_TIME = "end_time";
		static final String DURATION = "duration";
		static final String VIBRATE = "vibrate";
		static final String LABEL = "label";
		static final String ALERT = "alert";
	}
	
	private TimerContract() {}
}
