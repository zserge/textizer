package trikita.textizer;

import jscheme.*;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.graphics.Color;
import android.text.TextPaint;
import android.graphics.Typeface;
import android.text.Layout;

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

	// (clock FORMAT) - return formatted date
	public static class SimpleClockProvider extends Procedure {
		public Object apply(Scheme scheme, Object args) {
			String fmt = new String((char[])first(args));
			SimpleDateFormat formatter = new SimpleDateFormat(fmt);
			return formatter.format(new Date()).toCharArray();
		}
	}

	// (logd ...) - output logs
	public static class Logger extends Procedure {
		public Object apply(Scheme scheme, Object args) {
			String s = "";
			for ( ; args instanceof Pair; args = ((Pair)args).rest) {
				s = s + stringify(first(args)) + " ";
			}
			Log.d(tag, s);
			return args;
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
