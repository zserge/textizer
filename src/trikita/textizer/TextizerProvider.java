package trikita.textizer;

import java.util.Random;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import android.content.ComponentName;
import android.content.Intent;
import android.app.PendingIntent;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.Config;

import jscheme.*;
import android.os.Bundle;
import android.widget.Toast;

public abstract class TextizerProvider extends AppWidgetProvider {
	private final static String tag = "TextizerProvider";

	protected abstract int getWidth();
	protected abstract int getHeight();
	protected abstract int[] getAppWidgetIds(Context c, AppWidgetManager manager);

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(tag, "onReceive");
		super.onReceive(context, intent);

		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
				int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
				onUpdate(context, appWidgetManager, new int[] { appWidgetId });
			}
		}
	}

	@Override
	public void onUpdate(Context c, AppWidgetManager manager, int[] dummyWidgetIds) {
		Log.d(tag, "onUpdate()");
		super.onUpdate(c, manager, dummyWidgetIds);

		int[] widgetIds = getAppWidgetIds(c, manager);

		for (int id : widgetIds) {
			WidgetPresenter wp = WidgetRegistry.getWidgetPresenter(c, id, this);
			if (wp == null) {
				Log.e(tag, "failed to get presenter for widget " + id);
				return;
			}

			int color = wp.getColor();

			RemoteViews remoteViews = new RemoteViews(c.getPackageName(), R.layout.widget);
			remoteViews.setInt(R.id.image, "setBackgroundColor", color);

			Bitmap b = wp.createBitmap();
			remoteViews.setImageViewBitmap(R.id.image, b);

			manager.updateAppWidget(id, remoteViews);
		}
	}

	public void onEnabled(Context c) {
		Log.d(tag, "onEnabled()");
	}

	public void onDisabled(Context c) {
		Log.d(tag, "onDisabled()");
	}

	public void onDeleted(Context c, int[] ids) {
		for (int id : ids) {
			Log.d(tag, "onDeleted(): " + id);
			WidgetRegistry.removeWidget(c, id);
		}
	}
}

