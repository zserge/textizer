package trikita.textizer.appwidget;

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

import trikita.textizer.SchemeService;
import trikita.textizer.WidgetPresenter;
import trikita.textizer.WidgetRegistry;
import trikita.textizer.R;

public abstract class TextizerAppWidget extends AppWidgetProvider {
	private final static String tag = "TextizerAppWidget";

	private final static String CLICK_ACTION = "trikita.textizer.CLICK_ACTION";

	public abstract int getWidth();
	public abstract int getHeight();
	public abstract int[] getAppWidgetIds(Context c, AppWidgetManager manager);

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(tag, "onReceive: " + action);

		if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			onUpdate(context, appWidgetManager, null);
		} else if (action.equals(SchemeService.ACTION_UPDATE_COMPLETE)) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			onUpdateComplete(context, appWidgetManager, intent);
		} else {
			super.onReceive(context, intent);
		}
	}

	private void onUpdateComplete(Context c, AppWidgetManager manager, Intent intent) {
		Log.d(tag, "onUpdateComplete()");

		int id = intent.getIntExtra("id", 0);
		int bgcolor = intent.getIntExtra("bgcolor", 0);
		Bitmap bitmap = (Bitmap) intent.getParcelableExtra("bitmap");

		RemoteViews remoteViews = new RemoteViews(c.getPackageName(), R.layout.widget);
		remoteViews.setInt(R.id.image, "setBackgroundColor", bgcolor);
		remoteViews.setImageViewBitmap(R.id.image, bitmap);

		//Intent intent = new Intent(TextizerAppWidget.CLICK_ACTION);
		//PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, 0);
		//remoteViews.setOnClickPendingIntent(R.id.image, pendingIntent);
		//Log.d(tag, "registered on click for id " + id);

		manager.updateAppWidget(id, remoteViews);
		Log.d(tag, "onUpdateComplete() finished");
	}

	@Override
	public void onUpdate(Context c, AppWidgetManager manager, int[] dummyWidgetIds) {
		Log.d(tag, "onUpdate()");
		int[] widgetIds = getAppWidgetIds(c, manager);

		for (int id : widgetIds) {
			SchemeService.startUpdate(c, id, this.getWidth(), this.getHeight());
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

