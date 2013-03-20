package trikita.textizer;

import jscheme.*;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.graphics.Color;
import android.text.TextPaint;
import android.graphics.Typeface;
import android.text.Layout;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.StringReader;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.BatteryManager;

public class SchemeBindings {

	private final static String tag = "SchemeBindings";

	// (hack-size WIDTH HEIGHT) - force changing widget size
	public static class HackWidgetSize extends Procedure {
		private WidgetPresenter mWidgetPresenter;

		public HackWidgetSize(WidgetPresenter wp) {
			mWidgetPresenter = wp;
		}

		public Object apply(Scheme scheme, Object args) {
			int w = (int) num(first(args));
			int h = (int) num(second(args));
			mWidgetPresenter.updateSize(w, h);
			return TRUE;
		}
	}

	public static String VARIABLES = "vars";

	// (set-var name value)
	public static class SetVariableProcedure extends Procedure {
		private Context mContext;

		public SetVariableProcedure(Context c) {
			mContext = c;
		}

		public Object apply(Scheme scheme, Object args) {
			String name = new String((char[])first(args));
			Object value = second(args);

			SharedPreferences prefs = mContext.getSharedPreferences(VARIABLES, 0);
			SharedPreferences.Editor editor = prefs.edit();

			String s = stringify(value, true);

			editor.putString(name, s);

			editor.commit();
			return 0;
		}
	}
	// (get-var name)
	public static class GetVariableProcedure extends Procedure {
		private Context mContext;

		public GetVariableProcedure(Context c) {
			mContext = c;
		}

		public Object apply(Scheme scheme, Object args) {
			String name = new String((char[])first(args));

			SharedPreferences prefs = mContext.getSharedPreferences(VARIABLES, 0);
			if (!prefs.contains(name)) {
				return FALSE;
			}

			String s = prefs.getString(name, "#f");
			Object x = new InputPort(new StringReader(s)).read();
			return x;
		}
	}

	// (clock FORMAT) - return formatted date
	public static class SimpleClockProvider extends Procedure {
		public Object apply(Scheme scheme, Object args) {
			String fmt = new String((char[])first(args));
			SimpleDateFormat formatter = new SimpleDateFormat(fmt);
			return formatter.format(new Date()).toCharArray();
		}
	}

	// (battery ATTRIBUTE) - return formatted battery info
	public static class SimpleBatteryProvider extends Procedure {
		private Context mContext;
		private final static int LEVEL = 1;
		private final static int IS_CHARGING = 2;

		public SimpleBatteryProvider(Context c, Scheme scheme) {
			mContext = c;
			scheme.getEnvironment().define("*level*", LEVEL);
			scheme.getEnvironment().define("*charging*", IS_CHARGING);
			scheme.getEnvironment().define("*charging/usb*", 2);
			scheme.getEnvironment().define("*charging/ac*", 1);
			scheme.getEnvironment().define("*charging/none*", 0);
		}

		public Object apply(Scheme scheme, Object args) {
			int arg = (int) num(first(args));

			IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = mContext.registerReceiver(null, filter);

			int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
				status == BatteryManager.BATTERY_STATUS_FULL;
			int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
			boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

			float batteryPct = level / (float) scale;

			if (arg == LEVEL) {
				return num(batteryPct);
			} else if (arg == IS_CHARGING) {
				if (isCharging) {
					return num(1);
				} else {
					return num(0);
				}
			}
			return FALSE;
		}
	}

	// (format fmt ...) - output logs
	public static class Format extends Procedure {
		public Object apply(Scheme scheme, Object args) {
			char[] fmt = (char[]) first(args);
			Log.d(tag, "fmt = " + new String(fmt));
			args = rest(args);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < fmt.length; i++) {
				if (fmt[i] == '~') {
					i++;
					if (i == fmt.length) {
						return FALSE;
					}
					if (fmt[i] == 'a') {
						sb.append(stringify(first(args), true));
						args = rest(args);
					} else if (fmt[i] == 's') {
						sb.append(stringify(first(args), false));
						args = rest(args);
					} else if (fmt[i] == '%') {
						sb.append('\n');
					} else if (fmt[i] == '~') {
						sb.append('~');
					} else {
						return FALSE;
					}
				} else {
					sb.append(fmt[i]);
				}
			}
			Log.d(tag, "sb = " + sb.toString());
			return sb.toString();
		}
	}

	// (style ...) - set paint style
	public static class PaintStyle extends Procedure {

		private WidgetPresenter mWidgetPresenter;

		public PaintStyle(WidgetPresenter w) {
			mWidgetPresenter = w;
		}

		public Object apply(Scheme scheme, Object args) {
			TextPaint paint = mWidgetPresenter.getTextPaint();
			for (; args != null; args = rest(args)) {
				Object pair = first(args);
				String key = (String) first(pair);
				String value = new String((char[])second(pair));
				if (key.equals("color")) {
					int color = Color.parseColor(value);
					paint.setColor(color & 0xffffff);
					paint.setAlpha((color >> 24) & 0xff);
				} else if (key.equals("font")) {
					if (value.equals("sans")) {
						paint.setTypeface(Typeface.SANS_SERIF);
					} else if (value.equals("serif")) {
						paint.setTypeface(Typeface.SERIF);
					} else if (value.equals("mono")) {
						paint.setTypeface(Typeface.MONOSPACE);
					} else if (value.equals("bold")) {
						paint.setTypeface(Typeface.DEFAULT_BOLD);
					} else {

					}
					if (third(pair) != null) {
						float size = (float) num(third(pair));
						paint.setTextSize(size);
					}
				} else if (key.equals("align")) {
					if (value.equals("right")) {
						mWidgetPresenter.setAlignment(Layout.Alignment.ALIGN_OPPOSITE);
					} else if (value.equals("left")) {
						mWidgetPresenter.setAlignment(Layout.Alignment.ALIGN_NORMAL);
					} else if (value.equals("center")) {
						mWidgetPresenter.setAlignment(Layout.Alignment.ALIGN_CENTER);
					} else {

					}
					if (third(pair) != null) {
						value = new String((char[]) third(pair));
						if (value.equals("bottom")) {
							mWidgetPresenter.setVerticalAlignment(Layout.Alignment.ALIGN_OPPOSITE);
						} else if (value.equals("top")) {
							mWidgetPresenter.setVerticalAlignment(Layout.Alignment.ALIGN_NORMAL);
						} else if (value.equals("center")) {
							mWidgetPresenter.setVerticalAlignment(Layout.Alignment.ALIGN_CENTER);
						} else {

						}
					}
				} else {

				}
				Log.d(tag, "args: " + key + ": " + value);
			}
			return TRUE;
		}
	}

	// (grid WIDTH HEIGHT) - create grid layout
	public static class Grid extends Procedure {

		private WidgetPresenter mWidgetPresenter;

		public Grid(WidgetPresenter w) {
			mWidgetPresenter = w;
		}

		public Object apply(Scheme scheme, Object args) {
			double gridWidth = num(first(args)); args = rest(args);
			double gridHeight = num(first(args)); args = rest(args);
			String colorString = new String((char[])first(args)); args = rest(args);
			int updateInterval = (int) num(first(args));
			// TODO: assert than no more args left
			int color = Color.parseColor(colorString);
			Log.d(tag, "new grid: " + gridWidth + "x" + gridHeight + ", color " + colorString);
			Log.d(tag, "update interval: " + updateInterval);
			mWidgetPresenter.setUpdateInterval(updateInterval);
			mWidgetPresenter.setGridSize((int)gridWidth, (int)gridHeight);
			mWidgetPresenter.setColor(color);
			return TRUE;
		}
	}

	// (cell RECT PROVIDER ARGS...) - create logical cell inside a grid
	public static class Cell extends Procedure {
		public WidgetPresenter mWidgetPresenter;

		public Cell(WidgetPresenter w) {
			mWidgetPresenter = w;
		}

		public Object apply(Scheme scheme, Object args) {
			Object rect = first(args); args = rest(args);
			int x = (int) num(first(rect)); rect = rest(rect);
			int y = (int) num(first(rect)); rect = rest(rect);
			int w = 1;
			int h = 1;
			if (rect instanceof Pair) {
				w = (int) num(first(rect)); rect = rest(rect);
				if (rect instanceof Pair) {
					h = (int) num(first(rect)); rect = rest(rect);
				}
			}
			WidgetPresenter.WidgetCell cell = mWidgetPresenter.createCell(x, y, w, h);
			Object obj;
			if (first(args) instanceof String) {
				Log.d(tag, "Provider named " + sym(first(args)));
				obj = scheme.getEnvironment().lookup(sym(first(args)));
				if (!(obj instanceof Procedure)) {
					Log.d(tag, "Symbol " + sym(first(args)) + " is not a procedure");
				}
				args = rest(args);
			} else if (first(args) instanceof char[]) {
				Log.d(tag, "Provider plain text");
				obj = new Procedure() {
					public Object apply(Scheme scheme, Object args) {
						return first(args);
					}
				};
			} else if (first(args) instanceof Procedure) {
				Log.d(tag, "Provider unnamed (lambda)");
				obj = first(args);
				args = rest(args);
			} else {
				Log.e(tag, "Invalid syntax");
				return FALSE;
			}
			cell.provider = (Procedure) obj;
			cell.args = args;
			return TRUE;
		}
	}
}

