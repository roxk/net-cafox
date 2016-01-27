package net.cafox.debug;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.cafox.R;

/**
 * A simple implementation of an activity that display information about a previous uncaught exception.
 * Client should use the the nested class {@link CrashReportActivity.StartActivityHandler} along with
 * this activity instead of the base {@link net.cafox.debug.StartActivityHandler}.
 */
public class CrashReportActivity extends AppCompatActivity
{
	public final static String INFO_STACK_TRACE_KEY = "stack_trace";

	public static class StartActivityHandler extends net.cafox.debug.StartActivityHandler
	{
		public StartActivityHandler(Context context)
		{
			super(context, CrashReportActivity.class, INFO_STACK_TRACE_KEY);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crash_report);
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null) actionBar.hide();

		// Get crash info.
		Intent intent = getIntent();
		String stackTrace = intent.getStringExtra(INFO_STACK_TRACE_KEY);

		TextView info = (TextView) findViewById(R.id.info);
		info.setText("********** Stack Trace **********\n" + stackTrace);

		// Setup clicking on info to copy.
		info.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String text = ((TextView) v).getText().toString();
				ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData data = ClipData.newPlainText(text, text);
				manager.setPrimaryClip(data);
				Toast.makeText(CrashReportActivity.this, getResources().getString(R.string.activity_crash_report_crash_info_copied), Toast.LENGTH_SHORT).show();
			}
		});
	}
}
