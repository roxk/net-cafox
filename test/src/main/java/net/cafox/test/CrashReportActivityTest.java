package net.cafox.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import net.cafox.debug.CrashReportActivity;

/**
 *
 */
public class CrashReportActivityTest extends AppCompatActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crash_report_test);
		Thread.setDefaultUncaughtExceptionHandler(new CrashReportActivity.StartActivityHandler(this));
		View info = findViewById(R.id.info);
		info.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				throw new RuntimeException("Hello Exception!");
			}
		});
	}
}
