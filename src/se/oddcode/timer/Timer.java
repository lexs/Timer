package se.oddcode.timer;

import se.oddcode.timer.TimerContract.Columns;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Timer implements Parcelable {
	private long id = -1;
	private long endTime;
	private long duration;
	private boolean vibrate;
	private String label;
	private Uri alert;
	
	public Timer(long endTime, long duration, boolean vibrate, String label, Uri alert) {
		this.endTime = endTime;
		this.duration = duration;
		this.vibrate = vibrate;
		this.label = label;
		
		if (alert == null) {
			alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		}
		
		
		this.alert = alert;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Timer)) {
			return false;
		}
		Timer other = (Timer) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}

	public Uri getUri() {
		return ContentUris.withAppendedId(TimerContract.CONTENT_URI, id);
	}
	
	public boolean isExpired() {
		return timeLeft() <= 0;
	}

	public long timeLeft() {
		return endTime - System.currentTimeMillis();
	}
	
	public ContentValues toContentValues() {
		ContentValues v = new ContentValues();
		v.put(Columns.END_TIME, endTime);
		v.put(Columns.DURATION, duration);
		v.put(Columns.VIBRATE, vibrate);
		v.put(Columns.LABEL, label);
		v.put(Columns.ALERT, alert.toString());
		
		return v;
	}
	
	public static Timer fromCursor(Cursor c) {
		long id = c.getLong(c.getColumnIndexOrThrow(Columns._ID));
		long endTime = c.getLong(c.getColumnIndexOrThrow(Columns.END_TIME));
		long duration = c.getLong(c.getColumnIndexOrThrow(Columns.DURATION));
		boolean vibrate = c.getInt(c.getColumnIndexOrThrow(Columns.VIBRATE)) == 1;
		String label = c.getString(c.getColumnIndexOrThrow(Columns.LABEL));
		String alertString = c.getString(c.getColumnIndexOrThrow(Columns.ALERT));
		
		Uri alert = null;
		if (!TextUtils.isEmpty(alertString)) {
			alert = Uri.parse(alertString);
		}
		
		Timer timer = new Timer(endTime, duration, vibrate, label, alert);
		timer.id = id;
		
		return timer;
	}
	
	public long getId() {
		return id;
	}

	public long getEndTime() {
		return endTime;
	}

	public long getDuration() {
		return duration;
	}

	public boolean shouldVibrate() {
		return vibrate;
	}

	public String getLabel() {
		return label;
	}
	
	public String getLabelOrDefault(Context context) {
		if (TextUtils.isEmpty(label)) {
			return context.getString(R.string.default_label);
		} else {
			return label;
		}
	}

	public Uri getAlert() {
		return alert;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(id);
		out.writeLong(endTime);
		out.writeLong(duration);
		out.writeInt(vibrate ? 1 : 0);
		out.writeString(label);
		out.writeParcelable(alert, flags);
	}

	public static final Parcelable.Creator<Timer> CREATOR = new Parcelable.Creator<Timer>() {
		public Timer createFromParcel(Parcel in) {
			return new Timer(in);
		}

		public Timer[] newArray(int size) {
			return new Timer[size];
		}
	};

	private Timer(Parcel in) {
		id = in.readLong();
		endTime = in.readLong();
		duration = in.readLong();
		vibrate = in.readInt() == 1;
		label = in.readString();
		alert = in.readParcelable(null);
	}
}
