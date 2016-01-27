package net.cafox.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ViewPagerIndicator extends View implements ViewPager.OnPageChangeListener
{
	// Paint.
	Paint paint = new Paint();
	int indicatorColor = Color.BLACK;
	int backgroundColor = Color.WHITE;

	// Geometry.
	float indicatorX = 0;
	float indicatorWidth = -1;
	float widthPercentage = -1;

	// Client listener.
	ViewPager.OnPageChangeListener listener;

	public ViewPagerIndicator(Context context)
	{
		super(context);
	}

	public void setPageCount(int count)
	{
		widthPercentage = 1.0f / count;
	}

	public void setColor(int backgroundColor, int indicatorColor, boolean invalidate)
	{
		this.backgroundColor = backgroundColor;
		this.indicatorColor = indicatorColor;

		if(invalidate)
		{
			invalidate();
		}
	}

	public void setIndicatorColor(int indicatorColor, boolean invalidate)
	{
		this.indicatorColor = indicatorColor;

		if(invalidate)
		{
			invalidate();
		}
	}

	public void setBackgroundColor(int backgroundColor, boolean invalidate)
	{
		this.backgroundColor = backgroundColor;

		if(invalidate)
		{
			invalidate();
		}
	}

	public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener)
	{
		this.listener = listener;
	}

	@Override
	public void onPageScrollStateChanged(int state)
	{
		// Call client listener.
		if(listener != null)
		{
			listener.onPageScrollStateChanged(state);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
	{
		// Call client listener.
		if(listener != null)
		{
			listener.onPageScrolled(position, positionOffset, positionOffsetPixels);
		}

		indicatorX = (position * indicatorWidth) +  (positionOffset * indicatorWidth);

		// Request redraw.
		invalidate();
	}

	@Override
	public void onPageSelected(int position)
	{
		// Call client listener.
		if(listener != null)
		{
			listener.onPageSelected(position);
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		indicatorWidth = getMeasuredWidth() * widthPercentage;
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		int indicatorRight = (int) (indicatorX + indicatorWidth);

		// Background.
		paint.setColor(backgroundColor);
		canvas.drawRect(getLeft(), 0, indicatorX, getMeasuredHeight(), paint);
		canvas.drawRect(indicatorRight, 0, getMeasuredWidth(), getMeasuredHeight(), paint);

		// Indicator.
		paint.setColor(indicatorColor);
		canvas.drawRect(indicatorX, 0, indicatorRight, getMeasuredHeight(), paint);
	}
}
