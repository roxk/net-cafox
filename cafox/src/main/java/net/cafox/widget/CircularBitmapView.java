package net.cafox.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import net.cafox.R;

/**
 * An view that adjust the its view bound into a perfect square according to a set of client defined rules. If a
 * bitmap is specified it draws it in a perfect circle that just touches the view bound, scale it and center it when
 * necessarily. If no bitmap is specified, a colored perfect circle is drawn instead.
 * <p>
 * One can use <code>setViewBoundType(int)</code> to specify whether the view should adjust its view bound to a
 * perfect square, and how it should perform the adjustment. Since its view bound is partly determined by its parent,
 * in order to adjust its view correctly some layout constraints must be satisfied. For example, when one set the
 * <code>viewBoundType</code> to <code>VIEW_BOUND_MATCH_PARENT_WIDTH</code>, both <code>layout_width</code>
 * and <code>layout_height</code> have to be set to <code>wrap_content</code>. For more information, see
 * {@link #setViewBoundType(int)}. Default implementation uses <code>VIEW_BOUND_NO_ADJUSTMENT</code>.
 * <p>
 * One should use {@link #setImageBitmap(Bitmap)} to specify the bitmap. The bitmap will be drawn using scale type
 * <code>centerCrop</code> as specified in ImageView. That is, the bitmap will be centered both vertically and
 * horizontally, and scaled to fill the whole view bound, i.e. the length of the shorter side of the bitmap will
 * always be equal to the length of each side of the view bound, which makes parts of the longer side of the bitmap
 * invisible since they are outside of the view bound.
 * <p>
 * A colored circle will be drawn when there is no bitmap specified. One should use {@link #setNoBitmapColor(int)} to
 * specify the color, or set the color to {@link Color#TRANSPARENT} when this behavior is not desired. This is also
 * specifiable in xml through the attribute <code>noBitmapColor</code>. Default
 * implementation uses {@link Color#TRANSPARENT}.
 */
@SuppressWarnings("UnusedDeclaration")
public class CircularBitmapView extends View
{
	/**
	 * Value used in {@link #setDesiredWidthAndHeight(int, int)} to indicate that either
	 * <code>desiredWidth</code> or <code>desiredHeight</code> should match its parent.
	 */
	public final static int MATCH_PARENT = -1;

	/**
	 * Internal value used to mark invalid value of <code>desiredWidth</code> and <code>desiredHeight</code>.
	 */
	private final static int INVALID_DIMENSION = -2;

	/**
	 * Value used to specify how the view should adjust view bound. When this value is used, the view
	 * adjust the view bound according to width, i.e. height will be adjusted so that it has the same
	 * length as width.
	 */
	public final static int VIEW_BOUND_MATCH_PARENT_WIDTH = 1;

	/**
	 * Value used to specify how the view should adjust view bound. When this value is used, the view
	 * adjust the view bound according to height, i.e. width will be adjusted so that it has the same
	 * length as height.
	 */
	public final static int VIEW_BOUND_MATCH_PARENT_HEIGHT = 2;

	/**
	 * Value used to specify how the view should adjust view bound. When this value is used, the view
	 * adjust the view bound according to the shorter side, i.e. the longer side will be adjusted so
	 * that it has the same length as the shorter side.
	 */
	public final static int VIEW_BOUND_MATCH_SHORTER = 3;

	/**
	 * Value used to specify how the view should adjust view bound. When this value is used, the view
	 * adjust the view bound according to the longer side, i.e. the shorter side will be adjusted so
	 * that it has the same length as the longer side.
	 */
	public final static int VIEW_BOUND_MATCH_LONGER = 4;

	/**
	 * Value used to indicate that no view bound adjustment should be made.
	 */
	public final static int VIEW_BOUND_NO_ADJUSTMENT = 5;

	private final static String VIEW_BOUND_ILLEGAL_ARGUMENT_DESCRIPTION =
					"viewBoundType is not set to any of the following value: " +
					"VIEW_BOUND_MATCH_PARENT_WIDTH, VIEW_BOUND_MATCH_PARENT_HEIGHT, " +
					"VIEW_BOUND_MATCH_SHORTER, VIEW_BOUND_MATCH_LONGER, " +
					"VIEW_BOUND_NO_ADJUSTMENT.";

	private final static String DESIRED_WIDTH_ILLEGAL_ARGUMENT_DESCRIPTION =
					"desired_width is not set to either match_parent or await_adjustment.";

	private final static String DESIRED_HEIGHT_ILLEGAL_ARGUMENT_DESCRIPTION =
					"desired_height is not set to either match_parent or await_adjustment.";

	private final static String ADJUST_VIEW_BOUND_UNSUPPORTED_OPERATION_DESCRIPTION =
					"contract of how viewBoundType, layout_width, layout_height, " +
					"desired_width, and desired_height work is broken. See the documentation " +
					"of CircularBitmapView.checkValidityAndCompatibility(int, int, int) " +
					"for more information.";

	/**
	 * Internal view bound type.
	 */
	private int viewBoundType;

	/**
	 * Width value used when <code>viewBoundType</code> is not {@link #VIEW_BOUND_NO_ADJUSTMENT}
	 */
	private int desiredWidth;

	/**
	 * Width value used when <code>viewBoundType</code> is not {@link #VIEW_BOUND_NO_ADJUSTMENT}
	 */
	private int desiredHeight;

	/**
	 * The bitmap specified. This is stored to conveniently get its dimension information.
	 */
	private Bitmap bitmap;

	private int noBitmapColor;

	private Paint paint = new Paint();
	private Matrix shaderMatrix = new Matrix();
	private float circleCenter;

	public CircularBitmapView(@NonNull final Context context) { super(context); initialize(context, null); }

	public CircularBitmapView(@NonNull final Context context, @NonNull final AttributeSet attrs) { super(context, attrs); initialize(context, attrs); }

	public CircularBitmapView(@NonNull final Context context, @NonNull final AttributeSet attrs, final int defStyleAttr)
	{
		super(context, attrs, defStyleAttr); initialize(context, attrs);
	}

	private void initialize(@NonNull final Context context, final AttributeSet attrs)
	{
		// Default value.
		int noBitmapColor = Color.TRANSPARENT;
		int specifiedViewBoundType = VIEW_BOUND_NO_ADJUSTMENT;
		int specifiedDesiredWidth = INVALID_DIMENSION;
		int specifiedDesiredHeight = INVALID_DIMENSION;

		if(attrs != null)
		{
			TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircularBitmapView, 0, 0);
			try
			{
				noBitmapColor = array.getColor(R.styleable.CircularBitmapView_noBitmapColor, noBitmapColor);
				specifiedViewBoundType = array.getInt(R.styleable.CircularBitmapView_viewBoundType, specifiedViewBoundType);

				// Try to get desired_width and desired_height by enum (i.e. match_parent). If failed, switch to obtain by dimension.
				try { specifiedDesiredWidth = array.getInt(R.styleable.CircularBitmapView_desired_width, specifiedDesiredWidth); }
				catch(Exception e) { specifiedDesiredWidth = array.getDimensionPixelSize(R.styleable.CircularBitmapView_desired_width, 0); }
				try { specifiedDesiredHeight = array.getInt(R.styleable.CircularBitmapView_desired_height, specifiedDesiredHeight); }
				catch(Exception e) { specifiedDesiredHeight = array.getDimensionPixelSize(R.styleable.CircularBitmapView_desired_height, 0); }
			}
			finally { array.recycle(); }
		}
		setNoBitmapColor(noBitmapColor);
		viewBoundType = specifiedViewBoundType;
		desiredWidth = specifiedDesiredWidth;
		desiredHeight = specifiedDesiredHeight;

		setImageBitmap(null);
		paint.setAntiAlias(true);
	}

	/**
	 * Set the color of the circle that will be drawn when there is no bitmap specified. One should set it
	 * as {@link Color#TRANSPARENT} when no circle should be drawn.
	 * <p>
	 * Its xml equivalent is <code>noBitmapColor</code>. Possible values are hex color code.
	 * <p>
	 * This method triggers a redraw if no bitmap is specified.
	 * <p>
	 * Default color is {@link Color#TRANSPARENT}.
	 * @param color The color of the circle.
	 * @see #setImageBitmap(Bitmap)
	 */
	public void setNoBitmapColor(final int color)
	{
		noBitmapColor = color;

		if(paint.getShader() == null)
		{
			if(paint.getColor() == color) return;
			paint.setColor(color);
			invalidate();
		}
	}

	/**
	 * Specify how the view should adjust its view bound so that all sides are of equal length, or that no
	 * adjustment should be made. The size of view bound directly affect the size of the circle to draw.
	 * <p>
	 * <code>viewBoundType</code> must be one of the following value: {@link #VIEW_BOUND_MATCH_PARENT_WIDTH},
	 * {@link #VIEW_BOUND_MATCH_PARENT_HEIGHT}, {@link #VIEW_BOUND_MATCH_SHORTER},
	 * {@link #VIEW_BOUND_MATCH_LONGER}, {@link #VIEW_BOUND_NO_ADJUSTMENT}.
	 * <p>
	 * When this is not set to <code>VIEW_BOUND_NO_ADJUSTMENT</code>, <code>layout_width</code> and
	 * <code>layout_height</code> must be set to <code>wrap_content</code> to avoid measurement problem in certain layout,
	 * such as <code>RelativeLayout</code>.
	 * <p>
	 * When this is set to <code>VIEW_BOUND_MATCH_PARENT_WIDTH</code> or <code>VIEW_BOUND_MATCH_PARENT_HEIGHT</code>,
	 * although <code>layout_width</code> and <code>layout_height</code> will still have to be specified, they will be
	 * ignored when calculating the size of view bound. On top of that, no other parameters need to be specified.
	 * The size of the reference side will be automatically calculated, as if one set the reference side in
	 * layout_width or layout_height to <code>match_parent</code>. The non-reference side will then be adjusted according
	 * to the length of the reference side. The reason why no parameters are needed is to avoid unnecessary logic handling
	 * when one attempt to set a concrete dimension on the reference side, which makes the adjustment redundant, for one
	 * can simply set that concrete dimension to non-reference side directly.
	 * <p>
	 * When this is set to <code>VIEW_BOUND_MATCH_SHORTER</code> or <code>VIEW_BOUND_MATCH_LONGER</code>,
	 * desired_width and desired_height will be used to calculate the size of view bound and hence need to be specified
	 * explicitly, either through xml or {@link #setDesiredWidthAndHeight(int, int)}.
	 * <p>
	 * When this is set to <code>VIEW_BOUND_NO_ADJUSTMENT</code>, no adjustment will be made, <code>desired_width</code>
	 * and <code>desired_height</code> will be ignored, and <code>layout_width</code> and <code>layout_height</code> will be
	 * used as-is. The circle will be drawn at the center of the view without bleeding out of the view bound.
	 * <p>
	 * <b>Note:</b> One should bare in mind that all possible values of this variable comes with some constraints with other variables.
	 * For example, when this is set to <code>VIEW_BOUND_MATCH_PARENT_HEIGHT</code>, both <code>layout_width</code> and
	 * <code>layout_height</code> must be set to <code>wrap_content</code>. When this is set to
	 * <code>VIEW_BOUND_MATCH_SHORTER</code>, on top of the previous constraint, <code>desired_width</code> and
	 * <code>desired_height</code> must be specified and there should only be one concrete dimension value. For more
	 * information on the exact specification of all constraints, see {@link #checkValidityAndCompatibility(int, int, int)}.
	 * <p>
	 * Its xml equivalent is <code>viewBoundType</code>. Possible values are <code>match_width</code>,
	 * <code>match_height</code>, <code>match_shorter</code>, <code>match_longer</code>, and <code>no_adjustment</code>.
	 * <p>
	 * This method triggers a layout request.
	 * <p>
	 * Default value is <code>VIEW_BOUND_NO_ADJUSTMENT</code>.
	 * @param viewBoundType The adjusting type.
	 * @throws IllegalArgumentException See {@link #checkValidityAndCompatibility(int, int, int)}.
	 * @throws UnsupportedOperationException See {@link #checkValidityAndCompatibility(int, int, int)}.
	 * @throws NullPointerException See {@link  #checkValidityAndCompatibility(int, int, int)}.
	 */
	public void setViewBoundType(final int viewBoundType)
	{
		checkValidityAndCompatibility(viewBoundType, desiredWidth, desiredHeight);
		if(this.viewBoundType == viewBoundType) return;
		this.viewBoundType = viewBoundType;
		requestLayout();
	}

	/**
	 * Specify desired width and desired height. These two variables are used only when
	 * <code>viewBoundType</code> is either <code>ADJUST_VIEW_BOUND_SHORTER</code> or
	 * <code>ADJUST_VIEW_BOUND_LONGER</code>, and will be ignored and not used otherwise. They work similarly
	 * to <code>layout_width</code> and <code>layout_height</code>, in that one can set {@link #MATCH_PARENT} or
	 * a concrete dimension value to them. {@link ViewGroup.LayoutParams#WRAP_CONTENT} is <b>NOT</b> a valid value.
	 * <p>
	 * There are constraints on the possible values of these parameters in respect to <code>viewBoundType</code>.
	 * For more information, see {@link #setViewBoundType(int)}.
	 * <p>
	 * Its xml equivalent is <code>desired_width</code> and <code>desired_height</code>. Possible values are
	 * <code>match_parent</code> and a concrete dimension value.
	 * <p>
	 * This method triggers a layout request.
	 * <p>
	 * These two variables do not have a default value.
	 * @param newDesiredWidth The new desired width.
	 * @param newDesiredHeight The new desired height.
	 * @throws IllegalArgumentException See {@link #checkValidityAndCompatibility(int, int, int)}.
	 * @throws UnsupportedOperationException See {@link #checkValidityAndCompatibility(int, int, int)}.
	 * @throws NullPointerException See {@link  #checkValidityAndCompatibility(int, int, int)}.
	 */
	public void setDesiredWidthAndHeight(final int newDesiredWidth, final int newDesiredHeight)
	{
		checkValidityAndCompatibility(viewBoundType, newDesiredWidth, newDesiredHeight);
		if(desiredWidth == newDesiredWidth && desiredHeight == newDesiredHeight) return;
		desiredWidth = newDesiredWidth;
		desiredHeight = newDesiredHeight;
		requestLayout();
	}

	/**
	 * Specify only desired width. For information about what desired width is, see {@link #setDesiredWidthAndHeight(int, int)}.
	 * Please note that constraints specified in that method applies directly to this method.
	 * <p>
	 * Its xml equivalent is <code>desired_width</code>. Possible values are <code>match_parent</code> and a concrete
	 * dimension value.
	 * <p>
	 * This method triggers a layout request.
	 * @param newDesiredWidth The new desired width.
	 * @throws IllegalArgumentException See {@link #setDesiredWidthAndHeight(int, int)}.
	 * @throws UnsupportedOperationException See {@link #setDesiredWidthAndHeight(int, int)}.
	 * @throws NullPointerException See {@link #setDesiredWidthAndHeight(int, int)}.
	 */
	public void setDesiredWidth(final int newDesiredWidth)
	{
		checkValidityAndCompatibility(viewBoundType, newDesiredWidth, desiredHeight);
		if(desiredWidth == newDesiredWidth) return;
		desiredWidth = newDesiredWidth;
		requestLayout();
	}

	/**
	 * Specify only desired height. For information about what desired height is, see {@link #setDesiredWidthAndHeight(int, int)}.
	 * Please note that constraints specified in that method applies directly to this method.
	 * <p>
	 * Its xml equivalent is <code>desired_height</code>. Possible values are <code>match_parent</code> and a concrete
	 * dimension value.
	 * <p>
	 * This method triggers a layout request.
	 * @param newDesiredHeight The new desired height.
	 * @throws IllegalArgumentException See {@link #setDesiredWidthAndHeight(int, int)}.
	 * @throws UnsupportedOperationException See {@link #setDesiredWidthAndHeight(int, int)}.
	 * @throws NullPointerException See {@link #setDesiredWidthAndHeight(int, int)}.
	 */
	public void setDesiredHeight(final int newDesiredHeight)
	{
		checkValidityAndCompatibility(viewBoundType, desiredWidth, newDesiredHeight);
		if(desiredHeight == newDesiredHeight) return;
		desiredHeight = newDesiredHeight;
		requestLayout();
	}

	/**
	 * Set the bitmap to be clipped. The image will be scaled and centered automatically. Set it as <code>null</code>
	 * to reset the bitmap, and as a result a colored circle will be drawn.
	 * <p>
	 * This method triggers a redraw.
	 * @param bitmap The bitmap to be clipped.
	 * @see #setNoBitmapColor(int)
	 */
	public void setImageBitmap(@Nullable final Bitmap bitmap)
	{
		if(this.bitmap == bitmap) return;
		this.bitmap = bitmap;

		if(bitmap == null)
		{
			paint.setShader(null);
			paint.setColor(noBitmapColor);
		}
		else
		{
			paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
			paint.setAlpha(255);
		}

		getCircleSizeAndApplyShaderMatrix();
		invalidate();
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	public void onMeasure(final int w, final int h)
	{
		int widthMode = MeasureSpec.getMode(w);
		int heightMode = MeasureSpec.getMode(h);
		int width = MeasureSpec.getSize(w);
		int height = MeasureSpec.getSize(h);
		int newWidth;
		int newHeight;
		int length;

		// Use desiredWidth and desiredHeight when they are specified as concrete dimension value.
		switch(viewBoundType)
		{
			case VIEW_BOUND_MATCH_SHORTER:
			case VIEW_BOUND_MATCH_LONGER:
				if(desiredWidth != MATCH_PARENT) width = desiredWidth;
				if(desiredHeight != MATCH_PARENT) height = desiredHeight;
				break;
			default:
				break;
		}

		// Determine the ideal size regardless of measure spec mode first.
		switch(viewBoundType)
		{
			case VIEW_BOUND_MATCH_PARENT_WIDTH:
				newWidth = width;
				newHeight = width;
				break;
			case VIEW_BOUND_MATCH_PARENT_HEIGHT:
				newWidth = height;
				newHeight = height;
				break;
			case VIEW_BOUND_MATCH_SHORTER:
				length = Math.min(width, height);
				newWidth = length;
				newHeight = length;
				break;
			case VIEW_BOUND_MATCH_LONGER:
				length = Math.max(width, height);
				newWidth = length;
				newHeight = length;
				break;
			case VIEW_BOUND_NO_ADJUSTMENT:
				newWidth = width;
				newHeight = height;
				break;
			default:
				throw new IllegalArgumentException(VIEW_BOUND_ILLEGAL_ARGUMENT_DESCRIPTION);
		}

		// Now clamp against measure spec mode.
		switch(widthMode)
		{
			case MeasureSpec.AT_MOST:
				newWidth = Math.min(width, newWidth);
				break;
			case MeasureSpec.EXACTLY:
				newWidth = width;
				break;
		}
		switch(heightMode)
		{
			case MeasureSpec.AT_MOST:
				newHeight = Math.min(height, newHeight);
				break;
			case MeasureSpec.EXACTLY:
				newHeight = height;
				break;
		}
		setMeasuredDimension(newWidth, newHeight);

		getCircleSizeAndApplyShaderMatrix();
	}

	@Override
	public void onDraw(@NonNull final Canvas canvas)
	{
		super.onDraw(canvas);
		canvas.drawCircle(circleCenter, circleCenter, circleCenter, paint);
	}

	@Override
	public boolean isInEditMode() { return true; }

	/**
	 * Check the validity of the give parameters, and their compatibility. Validity means whether the given
	 * parameter is  one of the allowed enum value. These values are specified in their respective
	 * <code>set</code> method, listed in the <code>See</code> section below. Compatibility means whether any
	 * of the following contracts are broken.
	 * <p>
	 * <b>Contract 1:</b> <b>Both</b> <code>layout_width</code> and <code>layout_height</code> are set to
	 * <code>wrap_content</code> when <code>viewBoundType</code> is not
	 * <code>VIEW_BOUND_NO_ADJUSTMENT</code>.
	 * <p>
	 * <b>Contract 2:</b> When <code>viewBoundType</code> is <code>VIEW_BOUND_NO_ADJUSTMENT</code>,
	 * <b>both</b> <code>layout_width</code> and <code>layout_height</code> must <b>NOT</b> be set to
	 * <code>wrap_content</code>.
	 * <p>
	 * <b>Contract 3:</b> When <code>viewBoundType</code> is <code>VIEW_BOUND_MATCH_SHORTER</code>
	 * or <code>VIEW_BOUND_MATCH_LONGER</code>, only <b>one</b> of <code>desiredWidth</code> and
	 * <code>desiredHeight</code> is set to a concrete dimension value. That is, either both of them are set
	 * <code>MATCH_PARENT</code>, or only of them is a concrete dimension value and another is set to
	 * <code>MATCH_PARENT</code>.
	 * @param viewBoundType The adjusting type to check.
	 * @param desiredWidth The desired width to check.
	 * @param desiredHeight The desired height to check.
	 * @throws IllegalArgumentException When any of the argument is invalid.
	 * @throws UnsupportedOperationException When there is compatibility issue between parameters.
	 * @throws NullPointerException When layout params is null.
	 * @see #setViewBoundType(int)
	 * @see #setDesiredWidthAndHeight(int, int)
	 */
	private void checkValidityAndCompatibility(final int viewBoundType, final int desiredWidth, final int desiredHeight)
	{
		boolean illegalAdjustment = false;
		boolean illegalWidth = false;
		boolean illegalHeight = false;
		boolean incompatible = false;

		// Contract 1 and 2.
		final ViewGroup.LayoutParams lp = getLayoutParams();
		if(lp == null) throw new NullPointerException("layout params is null. If this view is not instantiated in xml, please set a layout params.");
		switch(viewBoundType)
		{
			case VIEW_BOUND_NO_ADJUSTMENT:
				if(lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) incompatible = true;
				break;
			default:
				if(lp.width != ViewGroup.LayoutParams.WRAP_CONTENT || lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) incompatible = true;
				break;
		}

		// Validity and Contract 3.
		switch(viewBoundType)
		{
			case VIEW_BOUND_MATCH_PARENT_WIDTH:
				break;
			case VIEW_BOUND_MATCH_PARENT_HEIGHT:
				break;
			case VIEW_BOUND_MATCH_SHORTER:
			case VIEW_BOUND_MATCH_LONGER:
				if(desiredWidth <= INVALID_DIMENSION) illegalWidth = true;
				if(desiredHeight <= INVALID_DIMENSION) illegalHeight = true;
				if(desiredWidth != MATCH_PARENT && desiredHeight != MATCH_PARENT) incompatible = true;
				break;
			case VIEW_BOUND_NO_ADJUSTMENT:
				break;
			default:
				illegalAdjustment = true;
				break;
		}

		if(illegalAdjustment) throw new IllegalArgumentException(VIEW_BOUND_ILLEGAL_ARGUMENT_DESCRIPTION);
		else if(illegalWidth) throw new IllegalArgumentException(DESIRED_WIDTH_ILLEGAL_ARGUMENT_DESCRIPTION);
		else if(illegalHeight) throw new IllegalArgumentException(DESIRED_HEIGHT_ILLEGAL_ARGUMENT_DESCRIPTION);
		else if(incompatible) throw new UnsupportedOperationException(ADJUST_VIEW_BOUND_UNSUPPORTED_OPERATION_DESCRIPTION);
	}

	/**
	 * Calculate the circle's location and size. Scaled and transform shader so that it fits the center of
	 * the circle.
	 */
	private void getCircleSizeAndApplyShaderMatrix()
	{
		float size = getMeasuredWidth();
		if(size == 0) return;

		circleCenter = size * 0.5f;

		final Shader shader = paint.getShader();
		if(shader == null) return;

		final int bitmapWidth = bitmap.getWidth();
		final int bitmapHeight = bitmap.getHeight();

		// Scale the bitmap. Use the shorter side as scale reference.
		final float scale;
		if(bitmapWidth < bitmapHeight) scale = size / bitmapWidth;
		else scale = size / bitmapHeight;

		// Center the bitmap shader.
		final float scaledBitmapWidth = bitmapWidth * scale;
		final float scaledBitmapHeight = bitmapHeight * scale;
		int deltaY = (int) (((size - scaledBitmapHeight) * 0.5f) + 0.5f);
		int deltaX = (int) (((size - scaledBitmapWidth) * 0.5f) + 0.5f);

		shaderMatrix.reset();
		shaderMatrix.setScale(scale, scale);
		shaderMatrix.postTranslate(deltaX, deltaY);
		shader.setLocalMatrix(shaderMatrix);
	}
}