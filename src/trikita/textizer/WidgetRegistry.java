package trikita.textizer;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.*;
import android.os.FileObserver;
import android.content.Intent;
import android.appwidget.AppWidgetManager;

public class WidgetRegistry {

	private final static String tag = "WidgetRegistry";

	private final static String WIDGET_PREFERENCES = "textizer";

	private static FileObserver observer = null;

	// a file observer to detect config script changes
	private static class WidgetRegisteryObserver extends FileObserver {
		private Context mContext;

		public WidgetRegisteryObserver(Context c) {
			super(c.getExternalFilesDir(null).getAbsolutePath());
			mContext = c;
			startWatching();
		}

		@Override
		public void onEvent(int event, String path) {
			Log.d(tag, "file observer event: " + event + ": " + path);
			if (event == FileObserver.CLOSE_WRITE) {
				WidgetRegistry.update(mContext, 0);
			}
		}
	}

	public static void update(Context c, int id) {
		Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
		c.sendBroadcast(intent);
	}

	private static synchronized void ensureObserverIsRunning(Context c) {
		if (observer == null) observer = new WidgetRegisteryObserver(c);
	}

	public static WidgetPresenter getWidgetPresenter(Context c, int id,
			TextizerProvider p) {
		String scriptName = WidgetRegistry.get(c, id);
		if (scriptName == null) {
			Log.e(tag, "No script associated with id " + id);
			return null;
		}
		Log.d(tag, "get presenter for script '" + scriptName + "'");
		File f = new File(c.getExternalFilesDir(null), scriptName + ".scm");
		Log.d(tag, "reading widget script at " + f.getAbsolutePath());
		try {
			WidgetPresenter wp = new WidgetPresenter(c, id, p.getWidth(), p.getHeight(),
					new FileInputStream(f));
			return wp;
		} catch (IOException e) {
			Log.e(tag, "IOException: ", e);
		} catch (RuntimeException e) {
			Log.e(tag, "RuntimeException: ", e);
		}
		return null;
	}

	private static void createScript(Context c, String name)
		throws IOException {
		File f = new File(c.getExternalFilesDir(null), name + ".scm");
		if (f.exists()) {
			Log.d(tag, "script already exists. Using an existing script");
			return;
		}

		Log.d(tag, "new widget script at " + f.getAbsolutePath());
		PrintStream stream = null;
		try {
			stream = new PrintStream(new FileOutputStream(f));
			stream.println("; auto-generated template for '" + name + "' widget");
			stream.println("(grid 1 1 \"#80333333\" 60)");
			stream.println("(cell '(1 1 1 1) \"" + name + "\")");
			stream.println();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public static void addWidget(Context c, int id, String name)
		throws IOException {
		ensureObserverIsRunning(c);
		WidgetRegistry.createScript(c, name);
		String key = "id." + id;
		Log.d(tag, "Adding widget with id " + id + ", name: " + name);
		SharedPreferences prefs = c.getSharedPreferences(WIDGET_PREFERENCES, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, name);
		editor.commit();
	}

	private static String get(Context c, int id) {
		ensureObserverIsRunning(c);
		SharedPreferences prefs = c.getSharedPreferences(WIDGET_PREFERENCES, 0);
		String name = prefs.getString("id." + id, null);
		if (name == null) {
			Log.d(tag, "Widget with id " + id + " not found");
		}
		return name;
	}

	public static void removeWidget(Context c, int id) {
		ensureObserverIsRunning(c);
		String key = "id." + id;
		Log.d(tag, "Removing widget with id " + id);
		SharedPreferences prefs = c.getSharedPreferences(WIDGET_PREFERENCES, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
	}
}
