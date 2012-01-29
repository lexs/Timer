package se.oddcode.timer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class SetTimerView extends View {
	// scale configuration
	private static final int totalNicks = 100;
	private static final float degreesPerNick = 360.0f / totalNicks;
	private static final int centerDegree = 40; // the one in the top center (12 o'clock)
	private static final int minDegrees = -30;
	private static final int maxDegrees = 110;
	
	private Paint paint;
	private Paint scalePaint;
	
	private RectF scaleRect;
	
	public SetTimerView(Context context) {
		this(context, null);
	}
	
	public SetTimerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public SetTimerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		init();
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		
		// Scale to dimensions 1f
		float scale = Math.min(getWidth(), getHeight());
		canvas.scale(scale, scale);
		
		drawScale(canvas);
		
		canvas.restore();
	}
	
	private void drawScale(Canvas canvas) {
		canvas.drawOval(scaleRect, scalePaint);

		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		for (int i = 0; i < totalNicks; ++i) {
			float y1 = scaleRect.top;
			float y2 = y1 - 0.020f;
			
			canvas.drawLine(0.5f, y1, 0.5f, y2, scalePaint);
			
			if (i % 5 == 0) {
				int value = nickToDegree(i);
				
				if (value >= minDegrees && value <= maxDegrees) {
					String valueString = Integer.toString(value);
					canvas.drawText(valueString, 0.5f, y2 - 0.015f, scalePaint);
				}
			}
			
			canvas.rotate(degreesPerNick, 0.5f, 0.5f);
		}
		canvas.restore();		
	}
	
	private int nickToDegree(int nick) {
		int rawDegree = ((nick < totalNicks / 2) ? nick : (nick - totalNicks)) * 2;
		int shiftedDegree = rawDegree + centerDegree;
		return shiftedDegree;
	}
	
	private float degreeToAngle(float degree) {
		return (degree - centerDegree) / 2.0f * degreesPerNick;
	}
	
	private void init() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		RectF rimRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);
		
		float rimSize = 0.02f;
		RectF faceRect = new RectF();
		faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize, 
			     rimRect.right - rimSize, rimRect.bottom - rimSize);
		
		scalePaint = new Paint();
		scalePaint.setStyle(Paint.Style.STROKE);
		scalePaint.setColor(Color.WHITE);
		scalePaint.setStrokeWidth(0.005f);
		scalePaint.setAntiAlias(true);
		
		scalePaint.setTextSize(0.045f);
		scalePaint.setTypeface(Typeface.SANS_SERIF);
		scalePaint.setTextScaleX(0.8f);
		
		float scalePosition = 0.10f;
		scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
					  faceRect.right - scalePosition, faceRect.bottom - scalePosition);
		
		scaleRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);
	}

}
