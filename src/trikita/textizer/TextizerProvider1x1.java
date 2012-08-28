package trikita.textizer;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

public class TextizerProvider1x1 extends TextizerProvider {
	protected int getWidth() {
		return 1;
	}

	protected int getHeight() {
		return 1;
	}

	protected int[] getAppWidgetIds(Context c, AppWidgetManager manager) {
		return manager.getAppWidgetIds(new ComponentName(c, TextizerProvider1x1.class));
	}
}
