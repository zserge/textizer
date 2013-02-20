package trikita.textizer.appwidget;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

public class TextizerAppWidget2x1 extends TextizerAppWidget {
	public int getWidth() {
		return 2;
	}

	public int getHeight() {
		return 1;
	}

	public int[] getAppWidgetIds(Context c, AppWidgetManager manager) {
		return manager.getAppWidgetIds(new ComponentName(c, TextizerAppWidget2x1.class));
	}
}
