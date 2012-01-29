package se.oddcode.timer.view;

import se.oddcode.timer.util.TimeUtils;
import se.oddcode.timer.util.TimeUtils.Time;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.TextView;

public class CountdownView extends TextView {
	private static final int TICK_WHAT = 0;
	
	private boolean running = false;
	private boolean visible = true;
	
	private long endTime;
	
	public CountdownView(Context context) {
		this(context, null);
	}
	
	public CountdownView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public CountdownView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		visible = false;
		updateRunning();
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		
		visible = visibility == VISIBLE;
		updateRunning();
	}

	
	public void setEndTime(long endTime) {
		this.endTime = endTime;
		updateRunning();
		// Force text update
		updateText();
	}
	
	private void updateRunning() {
		if (!running && visible) {
			running = true;
			
			updateText();
			handler.sendEmptyMessageDelayed(TICK_WHAT, 1000);
		} else if (running && !visible) {
			running = false;
			
			handler.removeMessages(TICK_WHAT);
		}
	}
	
	private void updateText() {
		StringBuilder sb = new StringBuilder();
		
		long now = System.currentTimeMillis();
		
		long duration = endTime - now;
		if (duration < 0) {
			duration = -duration;
			sb.append('-');
			//setTextColor(Color.RED);
		}
		
		Time time = TimeUtils.toTime(duration);
		int hours = time.getHours();
		int minutes = time.getMinutes();
		int seconds = time.getSeconds();

		/*int minutes = (int) ((duration / (1000*60)) % 60);
		int hours   = (int) ((duration / (1000*60*60)) % 24);
		int seconds = (int) (duration / 1000) % 60;*/
		
		if (hours != 0) {
			sb.append(hours).append("h ");
		}
		
		if (hours != 0 || minutes != 0) {
			sb.append(minutes).append("m ");
		}
		
		sb.append(seconds).append('s');
		
		setText(sb.toString());
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message m) {
			if (running) {
				updateText();
				sendEmptyMessageDelayed(TICK_WHAT, 1000);
			}
		}
	};
}
