package se.oddcode.timer.util;

import java.util.concurrent.TimeUnit;

public class TimeUtils {
	public static Time toTime(long millis) {
		//int hours = (int) TimeUnit.MILLISECONDS.toHours(millis);
		//int minutes = (int) (TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours));
		//int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes));
		
		int minutes = (int) ((millis / (1000*60)) % 60);
		int hours   = (int) ((millis / (1000*60*60)) % 24);
		int seconds = (int) ((millis / 1000) % 60);
		
		return new Time(hours, minutes, seconds);
	}
	
	public static class Time {
		private int hours;
		private int minutes;
		private int seconds;

		private Time(int hours, int minutes, int seconds) {
			this.hours = hours;
			this.minutes = minutes;
			this.seconds = seconds;
		}

		public int getHours() {
			return hours;
		}

		public int getMinutes() {
			return minutes;
		}

		public int getSeconds() {
			return seconds;
		}
	}
	
	
	private TimeUtils() {}
}
