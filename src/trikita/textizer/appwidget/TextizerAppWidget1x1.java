package trikita.textizer.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

public class TextizerAppWidget1x1 extends TextizerAppWidget {
	public int getWidth() {
		return 1;
	}

	public int getHeight() {
		return 1;
	}

	public int[] getAppWidgetIds(Context c, AppWidgetManager manager) {
		return manager.getAppWidgetIds(new ComponentName(c, TextizerAppWidget1x1.class));
	}
}
