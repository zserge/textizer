package trikita.textizer;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.content.Context;

public class SchemeService extends IntentService {

	private final static String tag = "SchemeService";

	public final static String ACTION_UPDATE_COMPLETE = "trikita.textizer.ACTION_UPDATE_COMPLETE";

	public SchemeService() {
		super("SchemeService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int id = intent.getIntExtra("id", 0);
		int w = intent.getIntExtra("width", 1);
		int h = intent.getIntExtra("height", 1);
		Log.d(tag, "onHandleIntent(): id="+id+", w="+w+", h="+h);
		WidgetPresenter wp = WidgetRegistry.getWidgetPresenter(this, id, w, h);
		if (wp == null) {
			Log.e(tag, "failed to get presenter for widget " + id);
			return;
		}

		int color = wp.getColor();
		Bitmap bitmap = wp.createBitmap();

		Intent result = new Intent(ACTION_UPDATE_COMPLETE);
		result.putExtra("id", id);
		result.putExtra("bgcolor", color);
		result.putExtra("bitmap", bitmap);
		this.sendBroadcast(result);
	}

	public static void startUpdate(Context c, int id, int w, int h) {
		Intent intent = new Intent(c, SchemeService.class);
		intent.putExtra("id", id);
		intent.putExtra("width", w);
		intent.putExtra("height", h);
		c.startService(intent);
	}
}

