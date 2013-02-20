package trikita.textizer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.Config;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout;
import android.graphics.Rect;

import jscheme.*;
import java.io.*;
import java.util.*;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.os.SystemClock;
import android.net.Uri;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.text.Html;
import android.os.BatteryManager;
import android.content.SharedPreferences;

public class WidgetPresenter {

	private final static String tag = "WidgetPresenter";

	private Scheme scheme;

	private Context mContext;

	private int mWidgetId;
	private int mPixelWidth;
	private int mPixelHeight;

	public WidgetPresenter(Context c, int id, int width, int height, FileInputStream stream)
		throws IOException, RuntimeException {
		mContext = c;
		Log.d(tag, "starting Scheme for WidgetPresenter " + width + "x" + height);

		mWidgetId = id;

		Paint paint = new Paint();
		paint.setAntiAlias(true);

		mTextPaint = new TextPaint(paint);
		mTextPaint.setColor(Color.BLACK);
		mTextPaint.setTextSize(20);

		int dipWidth, dipHeight;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			dipWidth = width * 70 - 30; // ICS
			dipHeight = height * 70 - 30;
		} else {
			dipWidth = width * 74 - 2; // Gingerbread
			dipHeight = height * 74 - 2;
		}

		Log.d(tag, "density: " + mContext.getResources().getDisplayMetrics().density);
		mPixelWidth = (int) (dipWidth * mContext.getResources().getDisplayMetrics().density);
		mPixelHeight = (int) (dipHeight * mContext.getResources().getDisplayMetrics().density);

		scheme = new Scheme(null);
		scheme.getEnvironment().define("logd", new SchemeBindings.Logger());
		scheme.getEnvironment().define("hack-size", new SchemeBindings.HackWidgetSize(this));
		scheme.getEnvironment().define("clock", new SchemeBindings.SimpleClockProvider());
		scheme.getEnvironment().define("style", new SchemeBindings.PaintStyle(this));
		scheme.getEnvironment().define("grid", new SchemeBindings.Grid(this));
		scheme.getEnvironment().define("cell", new SchemeBindings.Cell(this));
		scheme.getEnvironment().define("set-var",
				new SchemeBindings.SetVariableProcedure(mContext));
		scheme.getEnvironment().define("get-var", 
				new SchemeBindings.GetVariableProcedure(mContext));
		scheme.load(new InputPort(stream));
		Log.d(tag, "widget script processed");
	}

	public void updateSize(int w, int h) {
		mPixelWidth = w;
		mPixelHeight = h;
	}

	public class WidgetCell {
		int x;
		int y;
		int w;
		int h;
		Procedure provider;
		Object args;
	}

	List<WidgetCell> cells = new ArrayList<WidgetCell>();

	private int mWidth = 1;
	private int mHeight = 1;
	private int mColor = 0xff333333;
	private long mUpdateInterval = 30 * 60 * 1000;

	public TextPaint getTextPaint() {
		return mTextPaint;
	}

	public void setGridSize(int w, int h) {
		mWidth = w;
		mHeight = h;
	}

	public void setColor(int color) {
		mColor = color;
	}

	public int getColor() {
		return mColor;
	}

	public void setUpdateInterval(int interval) {
		mUpdateInterval = interval * 1000;
	}

	public WidgetCell createCell(int x, int y, int w, int h) {
		WidgetCell c = new WidgetCell();
		c.x = x; c.y = y; c.w = w; c.h = h;
		cells.add(c);
		return c;
	}

	public static class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int id = Integer.parseInt(intent.getData().getSchemeSpecificPart());
			Log.d(tag, "update interval: starting update for widget id " + id);
			WidgetRegistry.update(context, id);
		}
	}

	private void startInterval() {
		AlarmManager am = (AlarmManager)
			mContext.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(null, Uri.parse("widget:"+mWidgetId),
				mContext, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		am.cancel(pendingIntent);
		am.set(AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime() + mUpdateInterval, pendingIntent);
		Log.d(tag, "started next alarm after " + mUpdateInterval + "ms");
	}

	private TextPaint mTextPaint;
	private Layout.Alignment mAlignment = Layout.Alignment.ALIGN_CENTER;
	private Layout.Alignment mVerticalAlignment = Layout.Alignment.ALIGN_CENTER;

	public void setAlignment(Layout.Alignment align) {
		mAlignment = align;
	}

	public void setVerticalAlignment(Layout.Alignment align) {
		mVerticalAlignment = align;
	}

	public Bitmap createBitmap() {
		startInterval();
		Log.d(tag, "Rendering bitmap " + mPixelWidth + "x" + mPixelHeight);

		Bitmap bitmap = Bitmap.createBitmap(mPixelWidth, mPixelHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		for (WidgetCell c : cells) {
			int x = mPixelWidth/mWidth * (c.x-1);
			int y = mPixelHeight/mHeight * (c.y-1);
			Log.d(tag, "x="+x+", y="+y+", w="+mPixelWidth/mWidth*c.w);
			canvas.save();
			canvas.translate(x, y);
			String text = "error";
			try {
				Object res = c.provider.apply(scheme, c.args);
				if (res instanceof char[]) {
					text = new String((char[]) res);
				} else if (res instanceof String) {
					text = (String) res;
				} else {
					throw new RuntimeException("provider returned neither char[] nor String");
				}
			} catch (RuntimeException e) {
				Log.d(tag, "error when applying the provider: ", e);
			}
			StaticLayout sl = new StaticLayout(Html.fromHtml(text),
					mTextPaint, mPixelWidth/mWidth * c.w,
					mAlignment, 1, 0, false);

			Log.d(tag, "mPixelHeight: " + sl.getHeight() + ", total: " + mPixelHeight/mHeight);
			if (mVerticalAlignment == Layout.Alignment.ALIGN_CENTER) {
				Log.d(tag, "vertical align: center");
				canvas.translate(0, (mPixelHeight/mHeight*c.h - sl.getHeight()) / 2);
			} else if (mVerticalAlignment == Layout.Alignment.ALIGN_OPPOSITE) {
				Log.d(tag, "vertical align: bottom");
				canvas.translate(0, (mPixelHeight/mHeight*c.h - sl.getHeight()));
			}

			sl.draw(canvas);
			canvas.restore();
		}

		return bitmap;
	}
}
