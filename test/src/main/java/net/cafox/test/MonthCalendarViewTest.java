package net.cafox.test;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import net.cafox.widget.MonthCalendarView;
import net.cafox.widget.MonthCalendarView.DayHolder;
import net.cafox.widget.MonthCalendarView.OnDayHolderSelectedListener;
import net.cafox.widget.MonthCalendarView.OnDisplayMonthChangedListener;

public class MonthCalendarViewTest extends AppCompatActivity implements OnDisplayMonthChangedListener, OnDayHolderSelectedListener
{
	public class TextDayHolder extends DayHolder<TextView>
	{
		public TextDayHolder(@NonNull TextView view)
		{
			super(view);
		}
	}

	private class DayAdapter extends MonthCalendarView.Adapter<TextDayHolder>
	{
		@Override
		protected TextDayHolder onCreateDayHolder(MonthCalendarView parent)
		{
			TextView textView = new TextView(MonthCalendarViewTest.this);
			textView.setLayoutParams(new LayoutParams(0, 80));
			return new TextDayHolder(textView);
		}

		@Override
		protected void onBindDayHolder(TextDayHolder dayHolder)
		{
			final int color;
			if(dayHolder.isSelected()) color = Color.GREEN;
			else if(dayHolder.isToday()) color = Color.RED;
			else color = getResources().getColor(R.color.foreground_black_primary);

			dayHolder.getView().setText("" + dayHolder.getDay());
			dayHolder.getView().setTextColor(color);
		}
	}

	private MonthCalendarView monthCalendarView;
	private TextView yearView;
	private TextView monthView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_month_calendar_view_test);

		monthCalendarView = (MonthCalendarView) findViewById(R.id.calendar_view);
		yearView = (TextView) findViewById(R.id.year);
		monthView = (TextView) findViewById(R.id.month);
		View getSelectedDayView = findViewById(R.id.get_selected);
		View previousMonthView = findViewById(R.id.previous_month);
		View nextMonthView = findViewById(R.id.next_month);
		View selectTodayView = findViewById(R.id.select_today);

		monthCalendarView.setAdapter(new DayAdapter())
				.setHorizontalDivider(R.drawable.horizontal_divider_1dp_black_secondary)
				.setVerticalDivider(R.drawable.vertical_divider_1dp_black_secondary)
				.setOnDisplayMonthChangedListener(this)
				.setOnDayHolderSelectedListener(this)
				.displayToday()
				.bindDayHolder();

		getSelectedDayView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				getSelected();
			}
		});
		previousMonthView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				monthCalendarView.displayPreviousMonth().bindDayHolder();
			}
		});
		nextMonthView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				monthCalendarView.displayNextMonth().bindDayHolder();
			}
		});
		selectTodayView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				monthCalendarView.selectToday();
			}
		});
	}

	@Override
	public void onDisplayMonthChanged(int year, int month)
	{
		Log.v("", "onDisplayMonthChanged");
		Log.v("", "year: " + monthCalendarView.getDisplayedYear() + " parameter year: " + year);
		Log.v("", "month: " + monthCalendarView.getDisplayedMonth() + " parameter month: " + month);

		yearView.setText("" + year);
		monthView.setText("" + month);
	}

	@Override
	public void onDayHolderSelected(DayHolder dayHolder)
	{
		makeText(dayHolder);
	}

	private void getSelected()
	{
		makeText(monthCalendarView.getSelectedDayHolder());
	}

	private void makeText(DayHolder dayHolder)
	{
		if(dayHolder == null) Toast.makeText(this, "not selecting any day", Toast.LENGTH_SHORT).show();
		else Toast.makeText(this, "selecting "
				+ dayHolder.getYear() + " "
				+ dayHolder.getMonth() + " "
				+ dayHolder.getDay() + ".", Toast.LENGTH_SHORT).show();
	}
}
