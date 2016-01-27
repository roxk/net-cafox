package net.cafox.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * A direction-independent, generic divider item decoration for recycler view. It is direction independent in that
 * all four sides of the item view can have a dedicated divider length. A single color will be used to fill these
 * divider area.
 */
@SuppressWarnings("UnusedDeclaration")
public class SimpleDividerDecoration extends RecyclerView.ItemDecoration
{
	private Paint paint = new Paint();
	private Rect dividerRect;

	/**
	 * Construct a simple divider decoration. The parameter name should be self-explanatory.
	 * @param topOffset The top divider length.
	 * @param leftOffset The left divider length.
	 * @param rightOffset The right divider length.
	 * @param bottomOffset The bottom divider length.
	 * @param dividerColor The color of the divider.
	 */
	public SimpleDividerDecoration(int topOffset, int leftOffset, int rightOffset, int bottomOffset, int dividerColor)
	{
		dividerRect = new Rect(leftOffset, topOffset, rightOffset, bottomOffset);
		paint.setColor(dividerColor);
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
	{
		outRect.set(dividerRect);
	}

	@Override
	public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state)
	{
		// TODO: Profile to see if this is faster then reducing overdraw by drawing each divider separately.
		canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
	}
}
