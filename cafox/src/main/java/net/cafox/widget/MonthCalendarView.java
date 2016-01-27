package net.cafox.widget;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Calendar;

public class MonthCalendarView extends LinearLayout
{
	public final static int INVALID = -1;

	public interface OnDisplayMonthChangedListener
	{
		void onDisplayMonthChanged(int year, int month);
	}

	public interface OnDayHolderSelectedListener
	{
		void onDayHolderSelected(DayHolder dayHolder);
	}

	public abstract static class DayHolder<V extends View>
	{
		private V view;
		private int previousYear;
		private int previousMonth;
		private int previousDay;
		private int year;
		private int month;
		private int day;
		private boolean isSelected;
		private boolean isToday;

		public DayHolder(@NonNull V view)
		{
			this.view = view;
			previousYear = INVALID;
			previousMonth = INVALID;
			previousDay = INVALID;
			isSelected = false;
			isToday = false;
		}

		void set(int year, int month, int day, boolean isToday)
		{
			this.previousYear = this.year;
			this.previousMonth = this.month;
			this.previousDay = this.day;
			this.year = year;
			this.month = month;
			this.day = day;
			this.isToday = isToday;
		}

		void setIsSelected(boolean isSelected) { this.isSelected = isSelected; }

		public V getView() { return view; }

		public int getPreviousYear() { return previousYear; }

		public int getPreviousMonth() { return previousMonth; }

		public int getPreviousDay() { return previousDay; }

		public int getYear() { return year; }

		public int getMonth() { return month; }

		public int getDay() { return day; }

		public boolean isSelected() { return isSelected; }

		public boolean isToday() { return isToday; }
	}

	public abstract static class Adapter<DH extends DayHolder>
	{
		protected abstract DH onCreateDayHolder(MonthCalendarView parent);

		protected abstract void onBindDayHolder(DH dayHolder);
	}

	private final static int COLUMN_COUNT = 7;

	private final static int ROW_COUNT = 6;

	private LinearLayout[] rows = new LinearLayout[ROW_COUNT];
	private DayHolder[][] dayHolders = new DayHolder[ROW_COUNT][COLUMN_COUNT];

	private DayHolder selectedDayHolder;
	private int displayedYear = INVALID;
	private int displayedMonth = INVALID;

	private Adapter adapter;

	private OnDisplayMonthChangedListener onDisplayMonthChangedListener;

	private OnDayHolderSelectedListener onDayHolderSelectedListener;

	public MonthCalendarView(Context context)
	{
		super(context);
		initialize(context);
	}

	public MonthCalendarView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize(context);
	}

	public MonthCalendarView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		initialize(context);
	}

	private void initialize(Context context)
	{
		// Setup.
		setOrientation(VERTICAL);
		for(int i = 0; i < ROW_COUNT; ++i)
		{
			LinearLayout row = new LinearLayout(context);

			row.setOrientation(HORIZONTAL);

			rows[i] = row;
			addView(row, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}

	public MonthCalendarView setAdapter(Adapter adapter)
	{
		this.adapter = adapter;

		if(adapter == null)
		{
			for(int i = 0; i < ROW_COUNT; ++i)
			{
				rows[i].removeAllViews();
				for(int j = 0; j < COLUMN_COUNT; ++j) dayHolders[i][j] = null;

				selectedDayHolder = null;
				displayedYear = INVALID;
				displayedMonth = INVALID;
			}
			return this;
		}

		// Setup new adapter.
		for(int i = 0; i < ROW_COUNT; ++i)
		{
			LinearLayout row = rows[i];
			for(int j = 0; j < COLUMN_COUNT; ++j)
			{
				DayHolder dayHolder = adapter.onCreateDayHolder(this);
				dayHolders[i][j] = dayHolder;
				ViewGroup.LayoutParams oldParams = dayHolder.getView().getLayoutParams();
				if(oldParams == null) throw new IllegalStateException("day holder's view has no layout params.");
				LayoutParams params = new LayoutParams(oldParams);
				params.weight = 1;
				row.addView(dayHolder.getView(), params);
			}
		}
		return this;
	}

	public MonthCalendarView resetSelectedDay()
	{
		if(selectedDayHolder == null) return this;
		selectedDayHolder = null;
		if(onDayHolderSelectedListener == null) return this;
		onDayHolderSelectedListener.onDayHolderSelected(null);
		return this;
	}

	public MonthCalendarView selectToday()
	{
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		selectDay(c.get(Calendar.YEAR), getDisplayMonthFromCalendarMonth(c.get(Calendar.MONTH)), c.get(Calendar.DAY_OF_MONTH));
		return this;
	}

	public MonthCalendarView selectDay(int year, int month, int day)
	{
		displayMonth(year, month);

		for(int i = 0; i < ROW_COUNT; ++i)
		{
			for(int j = 0; j < COLUMN_COUNT; ++j)
			{
				DayHolder dayHolder = dayHolders[i][j];
				if(dayHolder.getMonth() == month && dayHolder.getDay() == day)
				{
					if(selectedDayHolder == dayHolder) return this;

					// Clear old selection and select the new.
					if(selectedDayHolder != null) setIsDayHolderSelected(selectedDayHolder, false);
					selectedDayHolder = dayHolder;
					setIsDayHolderSelected(selectedDayHolder, true);

					// Notify.
					if(onDayHolderSelectedListener == null) return this;
					onDayHolderSelectedListener.onDayHolderSelected(dayHolder);

					return this;
				}
			}
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	private void setIsDayHolderSelected(DayHolder dayHolder, boolean isSelected)
	{
		dayHolder.setIsSelected(isSelected);
		adapter.onBindDayHolder(dayHolder);
	}

	public MonthCalendarView displayToday()
	{
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		displayMonth(c.get(Calendar.YEAR), getDisplayMonthFromCalendarMonth(c.get(Calendar.MONTH)));
		return this;
	}

	@SuppressWarnings({"ConstantConditions", "unchecked"})
	public MonthCalendarView displayMonth(int year, int month)
	{
		if(displayedYear == year && displayedMonth == month) return this;
		displayedYear = year;
		displayedMonth = month;

		final int calendarMonth = getCalendarMonthFromDisplayMonth(month);

		// Get information of this month.
		Calendar calendar = Calendar.getInstance();
		final int firstDay = 1;
		calendar.set(Calendar.YEAR, displayedYear);
		calendar.set(Calendar.MONTH, calendarMonth);
		calendar.set(Calendar.DATE, firstDay);
		final int firstDayIndex = getWeekIndexFromCalendarWeekDay(calendar.get(Calendar.DAY_OF_WEEK));
		final int dayCount = calendar.getActualMaximum(Calendar.DATE);

		// Get information of last month.
		final boolean isLastYear = calendarMonth == Calendar.JANUARY;
		final int lastYear = isLastYear ? displayedYear - 1 : displayedYear;
		final int lastMonth = isLastYear ? Calendar.DECEMBER : calendarMonth - 1;
		calendar.set(Calendar.YEAR, lastYear);
		calendar.set(Calendar.MONTH, lastMonth);
		final int lastDisplayMonth = getDisplayMonthFromCalendarMonth(lastMonth);
		final int lastDayIndex = firstDayIndex - 1;
		final int lastMonthDayCount = calendar.getActualMaximum(Calendar.DATE);

		// Get information of next month.
		final boolean isNextYear = calendarMonth == Calendar.DECEMBER;
		final int nextYear = isNextYear ? displayedYear + 1 : displayedYear;
		final int nextMonth = isNextYear ? Calendar.JANUARY : calendarMonth + 1;
		final int nextDisplayMonth = getDisplayMonthFromCalendarMonth(nextMonth);

		// Get information of today.
		final long currentTime = System.currentTimeMillis();
		calendar.setTimeInMillis(currentTime);
		final int currentYear = calendar.get(Calendar.YEAR);
		final int currentMonth = calendar.get(Calendar.MONTH);
		final int currentDay = calendar.get(Calendar.DATE);

		int accumulatedDay = 1;
		int nextMonthAccumulatedDay = 1;
		final boolean canPreviousMonthHasToday = lastYear == currentYear && lastMonth == currentMonth;
		final boolean canNextMonthHasToday = nextYear == currentYear && nextMonth == currentMonth;
		final boolean canThisMonthHasToday = displayedYear == currentYear && calendarMonth == currentMonth;
		for(int i = 0; i < ROW_COUNT; ++i)
		{
			for(int j = 0; j < COLUMN_COUNT; ++j)
			{
				DayHolder dayHolder = dayHolders[i][j];

				// Previous month's date.
				if(i == 0 && j < firstDayIndex)
				{
					final int diff = lastDayIndex - j;
					final int day = lastMonthDayCount - diff;
					dayHolder.set(lastYear, lastDisplayMonth, day, canPreviousMonthHasToday && day == currentDay);
					continue;
				}

				// Next month's date.
				if(accumulatedDay > dayCount)
				{
					dayHolder.set(nextYear, nextDisplayMonth, nextMonthAccumulatedDay, canNextMonthHasToday && nextMonthAccumulatedDay == currentDay);
					++nextMonthAccumulatedDay;
					continue;
				}

				// This month's date.
				dayHolder.set(displayedYear, displayedMonth, accumulatedDay, canThisMonthHasToday && accumulatedDay == currentDay);
				++accumulatedDay;
			}
		}

		// Default select nothing for a newly displayed month.
		resetSelectedDay();

		// Notify.
		if(onDisplayMonthChangedListener == null) return this;
		onDisplayMonthChangedListener.onDisplayMonthChanged(displayedYear, displayedMonth);

		return this;
	}

	public MonthCalendarView displayPreviousMonth()
	{
		return displayMonthBy(-1);
	}

	public MonthCalendarView displayNextMonth()
	{
		return displayMonthBy(1);
	}

	public MonthCalendarView displayMonthBy(int months)
	{
		Calendar c = Calendar.getInstance();
		final int firstDay = 1;
		c.set(Calendar.YEAR, displayedYear);
		c.set(Calendar.MONTH, getCalendarMonthFromDisplayMonth(displayedMonth));
		c.set(Calendar.DAY_OF_MONTH, firstDay);

		c.add(Calendar.MONTH, months);

		final int year = c.get(Calendar.YEAR);
		final int month = getDisplayMonthFromCalendarMonth(c.get(Calendar.MONTH));
		displayMonth(year, month);
		return this;
	}

	@SuppressWarnings("unchecked")
	public MonthCalendarView bindDayHolder()
	{
		for(int i = 0; i < ROW_COUNT; ++i)
		{
			for(int j = 0; j < COLUMN_COUNT; ++j)
			{
				adapter.onBindDayHolder(dayHolders[i][j]);
			}
		}

		return this;
	}

	public int getDisplayedYear() { return displayedYear; }

	public int getDisplayedMonth() { return displayedMonth; }

	public DayHolder getSelectedDayHolder() { return selectedDayHolder; }

	@SuppressWarnings("deprecation")
	public MonthCalendarView setVerticalDivider(@DrawableRes int resId)
	{
		for(int i = 0; i < ROW_COUNT; ++i)
		{
			rows[i].setDividerDrawable(getResources().getDrawable(resId));
			rows[i].setShowDividers(SHOW_DIVIDER_MIDDLE);
		}
		return this;
	}

	@SuppressWarnings("deprecation")
	public MonthCalendarView setHorizontalDivider(@DrawableRes int resId)
	{
		setDividerDrawable(getResources().getDrawable(resId));
		setShowDividers(SHOW_DIVIDER_MIDDLE);
		return this;
	}

	public MonthCalendarView setOnDisplayMonthChangedListener(OnDisplayMonthChangedListener onDisplayMonthChangedListener)
	{
		this.onDisplayMonthChangedListener = onDisplayMonthChangedListener;
		return this;
	}

	public MonthCalendarView setOnDayHolderSelectedListener(OnDayHolderSelectedListener onDayHolderSelectedListener)
	{
		this.onDayHolderSelectedListener = onDayHolderSelectedListener;
		return this;
	}

	private int getDisplayMonthFromCalendarMonth(int calendarMonth) {
		return calendarMonth + 1;
	}

	private int getCalendarMonthFromDisplayMonth(int displayMonth) { return displayMonth - 1; }

	private int getWeekIndexFromCalendarWeekDay(int calendarWeekDay) {
		return calendarWeekDay - 1;
	}
}
