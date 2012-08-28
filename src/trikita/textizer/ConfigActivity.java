package trikita.textizer;
import android.app.Activity;
import android.os.Bundle;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.os.Parcelable;

import java.io.*;

public class ConfigActivity extends Activity {

	private final static String tag = "ConfigActivity";

	private int mAppWidgetId;

	private EditText mEditText;

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.act_config);
		mEditText = (EditText) findViewById(R.id.edit);

		setResult(RESULT_CANCELED);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			// it's an impossible situation, widget ID is always passed to the config activity
			Log.d(tag, "Intent extras is null");
			finish();
			return;
		}

		mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			Log.d(tag, "Intent doesn't contain a valid appwidget ID");
			finish();
			return;
		}

		Log.d(tag, "ConfigActivity for widget id="+mAppWidgetId);
	}

	public void onOkButtonClick(View v) {
		Log.d(tag, "ConfigActivity: done, create new widget with id " + mAppWidgetId);
		String name = mEditText.getText().toString();

		// TODO: check if widget name is a valid filename, convert otherwise

		try {
			WidgetRegistry.addWidget(this, mAppWidgetId, name);

			Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			sendBroadcast(intent);

			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		} catch (IOException e) {
			Log.e(tag, "IOException: ", e);
		}
	}
}

