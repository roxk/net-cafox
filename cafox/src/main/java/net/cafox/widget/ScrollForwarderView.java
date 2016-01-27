package net.cafox.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.OverScroller;

import net.cafox.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A view that measure and layout children similarly to {@link LinearLayout} but can be scrolled smoothly across
 * several scrollable and non-scrollable children, either by dragging, flinging, or programmatically through calling
 * <code>scrollTo(int, int)</code> or <code>scrollBy(int, int)</code>, as if all views' content belong to one container
 * (or paper, using Material Design language). This is achieved by dividing the scrolling behavior of this view broadly
 * into two types: self-scrolling, or scroll forwarding. That is, when this view is asked to scroll, either its content
 * is scrolled in the same way as it is in {@link ScrollView} (hence self-scrolling), or it will scroll the content of
 * one particular visible scrollable child, such as a <code>ScrollView</code> or {@link RecyclerView} (hence scroll
 * forwarding).
 */
// TODO: Handle forwardee container list appropriately when addView() and removeView() is called after layout pass since
// elements in the list might become invalid.
@SuppressWarnings("UnusedDeclaration")
public class ScrollForwarderView extends ViewGroup
{
	/**
	 * An interface that this view uses to forward scroll to scrollable children and to determine whether they has
	 * reached either end of their content. Each view associated with a forwardee <b>MUST</b> have a unique id <b>AND</b>
	 * belongs to a forwardee container, or is itself a forwardee container.
	 */
	public interface Forwardee
	{
		/**
		 * @return The id associated with the scrollable view that will receive forwarded scroll.
		 */
		int getViewId();

		/**
		 * @return The amount scrolled horizontally, i.e. in x axis.
		 */
		int getScrollX();

		/**
		 * @return The amount scrolled vertically, i.e. in y axis.
		 */
		int getScrollY();

		/**
		 * It is where the view associated with this forwardee receives forwarded scroll. That view should scroll
		 * <code>dx</code> pixels horizontally, i.e. in x axis, when this method is invoked. If it reaches either the
		 * left end or right end of its scrollable content and hence cannot consume a delta amount of scroll, it
		 * should return that delta so that <code>ScrollForwarderView</code> knows this forwardee has already reached
		 * the end and hence <code>ScrollForwarderView</code> will take back the forwarding token and consume that
		 * delta.
		 * @param dx The amount that the view associated with this forwardee should scroll in x axis, in pixels.
		 * @return The delta amount of scroll that the view cannot consume, ideally because it has reached either end
		 * of its scrollable content.
		 */
		int scrollHorizontally(final int dx);

		/**
		 * It is where the view associated with this forwardee receives forwarded scroll. That view should scroll
		 * <code>dy</code> pixels vertically, i.e. in y axis, when this method is invoked. If it reaches either the
		 * top end or bottom end of its scrollable content and hence cannot consume a delta amount of scroll, it
		 * should return that delta so that <code>ScrollForwarderView</code> knows this forwardee has already reached
		 * the end and hence <code>ScrollForwarderView</code> will take back the forwarding token and consume that
		 * delta.
		 * @param dy The amount that the view associated with this forwardee should scroll in y axis, in pixels.
		 * @return The delta amount of scroll that the view cannot consume, ideally because it has reached either end
		 * of its scrollable content.
		 */
		int scrollVertically(final int dy);
	}

	/**
	 * A convenient class that holds forwardee container and forwardee together. Typically, one forwardee container
	 * will have one of this class that stores which forwardee is currently active.
	 * @see #containerHolderList
	 */
	private static class ForwardeeContainerHolder
	{
		public View forwardeeContainer;
		public Forwardee forwardee;

		public ForwardeeContainerHolder(@Nullable final View forwardeeContainer, @Nullable final Forwardee forwardee)
		{
			this.forwardeeContainer = forwardeeContainer;
			this.forwardee = forwardee;
		}
	}

	/**
	 * A margin layout params sub-class that records only whether a child is a forwardee container.
	 */
	public static class LayoutParams extends MarginLayoutParams
	{
		public boolean isForwardeeContainer;

		public LayoutParams(@NonNull final Context c, @NonNull final AttributeSet attrs)
		{
			super(c, attrs);
			TypedArray array = c.getTheme().obtainStyledAttributes(attrs, R.styleable.ScrollForwarderView, 0, 0);
			isForwardeeContainer = array.getBoolean(R.styleable.ScrollForwarderView_layout_isForwardeeContainer, false);
			array.recycle();
		}

		public LayoutParams(ViewGroup.LayoutParams source)
		{
			super(source);
			isForwardeeContainer = false;
		}
	}

	/**
	 * Value used to indicate that this view should layout its children horizontally and
	 * hence should scroll horizontally.
	 */
	public final static int ORIENTATION_HORIZONTAL = 1;

	/**
	 * Value used to indicate that this view should layout its children vertically and
	 * hence should scroll vertically. This is the default value.
	 */
	public final static int ORIENTATION_VERTICAL = 2;

	/**
	 * Value used to indicate that this view is staying still, not being dragged nor scrolling.
	 */
	private final static int STATE_MOVEMENT_IDLE = 1;

	/**
	 * Value used to indicate that this view is being dragged by user.
	 */
	private final static int STATE_MOVEMENT_DRAGGING = 2;

	/**
	 * Value used to indicate that this view is flinging.
	 */
	private final static int STATE_MOVEMENT_FLINGING = 3;

	/**
	 * Value used to indicate that this view is forwarding scroll to forwardee.
	 */
	private final static int STATE_SCROLL_FORWARDING = 1;

	/**
	 * Value used to indicate that this view is scrolling itself.
	 */
	private final static int STATE_SCROLL_SCROLLING_SELF = 2;

	/**
	 * Value used to indicate that no forwardee container holder is valid in this view.
	 */
	private final static int CONTAINER_HOLDER_NO_INDEX = -1;

	/**
	 * The distance that a user's finger must move before his gesture is
	 * considered to be dragging.
	 */
	private int slop;

	/**
	 * The velocity that a user's finger must have for its leaving off the
	 * screen is considered to be a fling.
	 */
	private int minFlingVelocity;

	/**
	 * The maximum flinging velocity to clamp.
	 */
	private int maxFlingVelocity;

	private PointF lastTouch = new PointF(0.0f, 0.0f);
	private PointF currentTouch = new PointF(0.0f, 0.0f);
	private PointF initialTouch = new PointF(0.0f, 0.0f);

	private VelocityTracker velocityTracker;
	private OverScroller overScroller;
	private Interpolator interpolator = new Interpolator()
	{
		@Override
		public float getInterpolation(float i)
		{
			i -= 1.0f;
			return i * i * i * i * i + 1.0f;
		}
	};
	private int lastFlingX;
	private int lastFlingY;

	/**
	 * Internal movement state. It is either {@link #STATE_MOVEMENT_IDLE}, {@link #STATE_MOVEMENT_DRAGGING}
	 * or {@link #STATE_MOVEMENT_FLINGING}. It is modified when user touches the screen or {@link #scrollTo}, or
	 * {@link #scrollBy} is called.
	 */
	private int movementState = STATE_MOVEMENT_IDLE;

	/**
	 * Internal scroll state. It is either {@link #STATE_SCROLL_FORWARDING} or {@link #STATE_SCROLL_SCROLLING_SELF}.
	 * It is modified when this view switch its behavior between self-scrolling and forwarding scroll to forwardee.
	 */
	private int scrollState = STATE_SCROLL_SCROLLING_SELF;

	/**
	 * Internal orientation. It is modified either through xml declaration or {@link #setOrientation(int)}.
	 */
	private int orientation = ORIENTATION_VERTICAL;

	/**
	 * The index of the current forwardee container that will receive scroll forwarding. It is used to retrieve
	 * forwardee container or forwardee from {@link #containerHolderList}.
	 */
	private int currentContainerHolderIndex = CONTAINER_HOLDER_NO_INDEX;

	/**
	 * Value used to indicate whether {@link #containerHolderList} is completely sorted.
	 * @see #containerHolderList
	 * @see #containerHolderSortingBuffer
	 */
	private boolean isContainerListSorted = false;

	/**
	 * The list that stores which forwardee each forwardee container is currently having. This list is sorted by
	 * forwardee container's sequence in xml declaration, or in other words, their sequence of being added to this view.
	 * This ensures that this view can pass the forwarding token between forwardee containers by simply incrementing or
	 * decrementing {@link #currentContainerHolderIndex}. All methods that need to access to forwardee container and its
	 * forwardee will need to access to this list.
	 * @see #containerHolderSortingBuffer
	 * @see #scrollSelfHorizontallyBy(int)
	 * @see #scrollSelfVerticallyBy(int)
	 * @see #forwardScrollHorizontallyBy(int)
	 * @see #forwardScrollVerticallyBy(int)
	 */
	private List<ForwardeeContainerHolder> containerHolderList = new ArrayList<>();

	/**
	 * The list that temporarily stores information about which forwardee in which forwardee container is active when
	 * user calls {@link #setForwardeeInContainer(Forwardee, int)} for later sorting. This allows users to set
	 * forwardee without worrying about the appropriate sequence of calling this method. The actual sorting is done in
	 * {@link #onMeasure(int, int)}.
	 * @see #containerHolderList
	 * @see #setForwardeeInContainer(Forwardee, int)
	 */
	private List<ForwardeeContainerHolder> containerHolderSortingBuffer = new ArrayList<>();

	/**
	 * Cached forwardee container count. Only this value reflects the reality since <code>containerHolderList.size()</code>
	 * is not guaranteed to be a big as the numbers of forwardee container.
	 */
	private int forwardeeContainerCount;

	/**
	 * Cached current forwardee container.
	 */
	private View currentForwardeeContainer = null;

	/**
	 * Cached current forwardee.
	 */
	private Forwardee currentForwardee = null;

	/**
	 * Cached end of view port. This is either getMeasuredWidth() - getPaddingRight() or getMeasuredHeight() - getPaddingBottom().
	 */
	private int viewPortEnd;

	/**
	 * Cached end of previous forwardee container. This is either its getRight() or getBottom().
	 */
	private int previousContainerEnd;

	/**
	 * Cached start of current forwardee container. This is either its getLeft() or getTop();
	 */
	private int currentContainerStart;

	/**
	 * Cached end of the last child. This is either its getRight() + rightMargin or getBottom() + bottomMargin.
	 */
	private int contentEnd;

	/**
	 * Scrolled distance by this view <b>AND</b> forwardees. This is modified in both {@link #scrollSelfHorizontallyBy(int)}
	 * and {@link #forwardScrollHorizontallyBy(int)}.
	 */
	private int selfAndForwardedScrollX = 0;

	/**
	 * Scrolled distance by this view <b>AND</b> forwardees. This is modified in both {@link #scrollSelfVerticallyBy(int)}
	 * and {@link #forwardScrollVerticallyBy(int)}.
	 */
	private int selfAndForwardedScrollY = 0;

	public ScrollForwarderView(@NonNull final Context context) { super(context); initialize(context, null);}

	public ScrollForwarderView(@NonNull final Context context, @NonNull final AttributeSet attrs) { super(context, attrs); initialize(context, attrs); }

	public ScrollForwarderView(@NonNull final Context context, @NonNull final AttributeSet attrs, final int defStyleAttrs)
	{
		super(context, attrs, defStyleAttrs); initialize(context, attrs);
	}

	private void initialize(@NonNull final Context context, @Nullable final AttributeSet attrs)
	{
		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		slop = viewConfiguration.getScaledTouchSlop();
		minFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
		maxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
		overScroller = new OverScroller(context, interpolator);

		if(attrs == null) return;

		TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScrollForwarderView, 0, 0);
		orientation = array.getInt(R.styleable.ScrollForwarderView_orientation, ORIENTATION_VERTICAL);
	}

	@Override
	public LayoutParams generateLayoutParams(@NonNull final AttributeSet attrs) { return new LayoutParams(getContext(), attrs); }

	/**
	 * Generate a layout params whose {@link LayoutParams#isForwardeeContainer} is set to <code>false</code>.
	 */
	@Override
	protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(super.generateDefaultLayoutParams()); }

	@Override
	protected LayoutParams generateLayoutParams(@NonNull final ViewGroup.LayoutParams p) { return new LayoutParams(p); }

	// Override to allow type-checking of LayoutParams.
	@Override
	protected boolean checkLayoutParams(@NonNull final ViewGroup.LayoutParams p) { return p instanceof LayoutParams; }

	@Override
	public boolean onInterceptTouchEvent(@NonNull final MotionEvent e)
	{
		lastTouch.set(currentTouch);
		currentTouch.set(e.getX(), e.getY());
		final int action = e.getActionMasked();

		// Did the touch event end without this view's intervention?
		if(action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
		{
			// Simply recycler velocity tracker and return.
			velocityTracker.recycle();
			velocityTracker = null;
			return false;
		}

		// Touch event hasn't end yet.
		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				// Record first touch position when user start to touch.
				initialTouch.set(currentTouch);

				// Is currently flinging?
				if(movementState == STATE_MOVEMENT_FLINGING)
				{
					// Switch to dragging immediately since user might want to
					// stop the fling.
					movementState = STATE_MOVEMENT_DRAGGING;
				}
				else movementState = STATE_MOVEMENT_IDLE;

				if(velocityTracker == null) velocityTracker = VelocityTracker.obtain();
				break;
			case MotionEvent.ACTION_MOVE:
				// The view is already in dragging state. Just leave.
				if(movementState == STATE_MOVEMENT_DRAGGING) break;

				// See if user is initiating a drag gesture. User is considered to be
				// dragging if the current touch position is <slop> pixels away from
				// initial touch position.
				final float deltaX = currentTouch.x - initialTouch.x;
				final float deltaY = currentTouch.y - initialTouch.y;
				if((orientation == ORIENTATION_HORIZONTAL && Math.abs(deltaX) > slop)
						|| (orientation == ORIENTATION_VERTICAL && Math.abs(deltaY) > slop))
				{
					movementState = STATE_MOVEMENT_DRAGGING;
				}
				break;
		}

		velocityTracker.addMovement(e);

		return movementState == STATE_MOVEMENT_DRAGGING;
	}

	@Override
	public boolean onTouchEvent(@NonNull final MotionEvent e)
	{
		lastTouch.set(currentTouch);
		currentTouch.set(e.getX(), e.getY());

		int action = e.getActionMasked();
		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				// Reset movementState and first touch position when user start to touch.
				initialTouch.set(currentTouch);

				// Is currently flinging?
				if(movementState == STATE_MOVEMENT_FLINGING)
				{
					// Switch to dragging immediately.
					movementState = STATE_MOVEMENT_DRAGGING;
				}
				else movementState = STATE_MOVEMENT_IDLE;

				if(velocityTracker == null) velocityTracker = VelocityTracker.obtain();
				break;
			case MotionEvent.ACTION_MOVE:
				// The view is already in dragging state. Just leave.
				if(movementState == STATE_MOVEMENT_DRAGGING) break;

				// See if user is initiating a drag gesture. User is considered to be
				// dragging if the current touch position is <slop> pixels away from
				// initial touch position.
				final float deltaX = currentTouch.x - initialTouch.x;
				final float deltaY = currentTouch.y - initialTouch.y;
				if((orientation == ORIENTATION_HORIZONTAL && Math.abs(deltaX) > slop)
						|| (orientation == ORIENTATION_VERTICAL && Math.abs(deltaY) > slop))
				{
					movementState = STATE_MOVEMENT_DRAGGING;
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				// User is releasing their finger. See if user's finger is moving fast. Issue a fling if
				// their movement velocity is larger than <maxFlingVelocity> pixels per seconds.
				velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
				final float velocityX = velocityTracker.getXVelocity();
				final float velocityY = velocityTracker.getYVelocity();
				final boolean flingHorizontally = orientation == ORIENTATION_HORIZONTAL && Math.abs(velocityX) > minFlingVelocity;
				final boolean flingVertically = orientation == ORIENTATION_VERTICAL && Math.abs(velocityY) > minFlingVelocity;

				// Get scroll velocity. It is negative since scrolling is in opposite direction of finger moving.
				int velocity = 0;
				if(flingHorizontally) velocity = (int) -velocityX;
				else if(flingVertically) velocity = (int) -velocityY;

				// Is the user flinging?
				if(flingHorizontally || flingVertically)
				{
					// Yes. Issue a fling. Now, the actual scroll computation will be done on the next
					// call to computeScroll() by its parent, which is triggered by invalidate().
					movementState = STATE_MOVEMENT_FLINGING;

					// Reset last scroll position.
					lastFlingX = 0;
					lastFlingY = 0;

					overScroller.fling(0, 0,
							velocity, velocity,
							Integer.MIN_VALUE, Integer.MAX_VALUE,
							Integer.MIN_VALUE, Integer.MAX_VALUE);
					invalidate();
				}
				else movementState = STATE_MOVEMENT_IDLE;

				// Recycle velocity tracker and return immediately.
				velocityTracker.recycle();
				velocityTracker = null;
				return true;
		}

		// Is the view in dragging state?
		if(movementState == STATE_MOVEMENT_DRAGGING)
		{
			// Calculate scrolling amount and forward that amount to other methods to handle scroll logic.
			// It passes negative values instead of positive because scroll direction is opposite to the
			// direction to which user's finger is moving.
			final int scrollX = (int) (currentTouch.x - lastTouch.x);
			final int scrollY = (int) (currentTouch.y - lastTouch.y);
			handleScroll(-scrollX, -scrollY);
		}

		velocityTracker.addMovement(e);

		return true;
	}

	@Override
	public void scrollTo(final int x, final int y)
	{
		// It has to also take into account of forwarded scroll.
		handleScroll(x - getSelfAndForwardedScrollX(), y - getSelfAndForwardedScrollY());
	}

	@Override
	public void scrollBy(final int x, final int y)
	{
		handleScroll(x, y);
	}

	@Override
	public void computeScroll()
	{
		// This is where the flinging magic happens. It took me lots of hours to find methods to handle this.
		// As it turns out all is done in this simple function which are guaranteed to be called on every
		// invalidate. Sigh.

		// Is this view not in scrolling state?
		if(movementState != STATE_MOVEMENT_FLINGING)
		{
			overScroller.forceFinished(true);
			return;
		}

		// Is the flinging finished?
		if(!overScroller.computeScrollOffset())
		{
			movementState = STATE_MOVEMENT_IDLE;
			return;
		}

		// Get scroll delta and apply.
		final int currentScrollX = overScroller.getCurrX();
		final int currentScrollY = overScroller.getCurrY();
		final int dx = orientation == ORIENTATION_HORIZONTAL ? currentScrollX - lastFlingX : 0;
		final int dy = orientation == ORIENTATION_VERTICAL ? currentScrollY - lastFlingY : 0;
		handleScroll(dx, dy);

		// Record last scroll position.
		lastFlingX = currentScrollX;
		lastFlingY = currentScrollY;

		// Call invalidate again to have this method called again.
		invalidate();
	}

	@Override
	public void requestLayout()
	{
		super.requestLayout();

		// Request a re-sorting of the container list.
		isContainerListSorted = false;
	}

	@Override
	public void onMeasure(final int w, final int h)
	{
		// Reset current container index since it become invalid when new
		// views are added or existing views were removed.
		currentContainerHolderIndex = CONTAINER_HOLDER_NO_INDEX;

		final int widthSize = MeasureSpec.getSize(w);
		final int heightSize = MeasureSpec.getSize(h);
		final int horizontalPadding = getPaddingLeft() + getPaddingRight();
		final int verticalPadding = getPaddingTop() + getPaddingBottom();
		final int widthWithPadding = widthSize - horizontalPadding;
		final int heightWithPadding = heightSize - verticalPadding;
		final boolean scrollHorizontally = orientation == ORIENTATION_HORIZONTAL;
		final boolean scrollVertically = orientation == ORIENTATION_VERTICAL;

		// Measure all children one by one according to their child index.
		final int childCount = getChildCount();
		forwardeeContainerCount = 0;
		for(int i = 0; i < childCount; ++i)
		{
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();

			// Is the child a forwardee container?
			if(lp.isForwardeeContainer)
			{
				// Hard code this child's view bound to be as big as this view.
				// Only take into account of margin and padding on non-scrolling axis.
				final int childHorizontalMargin = scrollHorizontally ? 0 : lp.leftMargin + lp.rightMargin;
				final int childVerticalMargin = scrollVertically ? 0 : lp.topMargin + lp.bottomMargin;
				final int childWidthSize = widthWithPadding - childHorizontalMargin;
				final int childHeightSize = heightWithPadding - childVerticalMargin;
				final int forwardeeWidthSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
				final int forwardeeHeightSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);
				child.measure(forwardeeWidthSpec, forwardeeHeightSpec);

				// Check whether this child has been associated with any forwardee in
				// sorting buffer.
				boolean hasForwardee = false;
				for(ForwardeeContainerHolder container : containerHolderSortingBuffer)
				{
					final View forwardeeContainer = container.forwardeeContainer;
					final Forwardee forwardee = container.forwardee;
					if(child == forwardeeContainer && forwardee != null)
					{
						// This child has been associated with a valid forwardee.
						// Record this in the actual container list. It is assumed
						// that correct amount of space is already reserved in the list.
						containerHolderList.set(forwardeeContainerCount, container);
						hasForwardee = true;

						// Since the position of current forwardee container might change due to adding or removing containers,
						// we need to reflect this change by updating current container holder index.
						if(currentForwardeeContainer == forwardeeContainer) currentContainerHolderIndex = forwardeeContainerCount;
						break;
					}
				}

				if(hasForwardee)
				{
					++forwardeeContainerCount;
					continue;
				}

				// No forwardee. Tell users to set up forwardee for this container appropriately.
				final int containerId = child.getId();
				final String idName = getContext().getResources().getResourceName(containerId);
				throw new IllegalStateException(
						"forwardee container with id " + idName + " is not associated " +
								"with any forwardee. call setForwardeeInContainer(Forwardee, int) " +
								"to set up appropriately");
			}

			// This child is an ordinary view. Just measure it as usual.
			measureChildWithMargins(child, w, 0, h, 0);
		}

		// Is there no forwardee container?
		if(forwardeeContainerCount == 0)
		{
			throw new IllegalStateException(
						"no forwardee container is found. Set at least one forwardee container by setting " +
						"ScrollForwarderView.LayoutParams.isForwardeeContainer = true or declare in xml " +
						"with attribute layout_isForwardeeContainer and value true");
		}

		// Since this view will be as big as any specified size regardless of whether measure
		// mode is AT_MOST, EXACTLY or unspecified, just set the given dimension anyways.
		setMeasuredDimension(widthSize, heightSize);

		isContainerListSorted = true;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		if(orientation == ORIENTATION_HORIZONTAL) layoutHorizontal(l, t, r, b);
		else layoutVertical(l, t, r, b);

		if(currentContainerHolderIndex == CONTAINER_HOLDER_NO_INDEX)
		{
			// Either this is the first time onMeasure() is called, or a previously "current" forwardee container
			// is removed. Set the first container as current and try to scroll back to previous scroll position.
			setCurrentContainerHolderIndex(0);

			final int lastSelfAndForwardedX = getSelfAndForwardedScrollX();
			final int lastSelfAndForwardedY = getSelfAndForwardedScrollY();
			selfAndForwardedScrollX = 0;
			selfAndForwardedScrollY = 0;
			handleScroll(lastSelfAndForwardedX, lastSelfAndForwardedY);
		}
	}

	@Override
	public boolean isInEditMode() { return true; }

	private void layoutHorizontal(final int l, final int t, final int r, final int b)
	{
		// Just layout them vertically one by one.
		final int leftPadding = getPaddingLeft();
		final int topPadding = getPaddingTop();
		final int childCount = getChildCount();
		int accumulatedChildrenWidth = leftPadding;
		for(int i = 0; i < childCount; ++i)
		{
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			final boolean allowHorizontalMargin = !lp.isForwardeeContainer;
			final int childWidth = child.getMeasuredWidth();
			final int childHeight = child.getMeasuredHeight();
			final int leftMargin = allowHorizontalMargin ? lp.leftMargin : 0;
			final int rightMargin = allowHorizontalMargin ? lp.rightMargin : 0;

			// Take into account of margin and padding.
			final int childLeft = accumulatedChildrenWidth + leftMargin;
			final int childTop = t + topPadding + lp.topMargin;
			final int childRight = childLeft + childWidth;
			final int childBottom = childTop + childHeight;

			child.layout(childLeft, childTop, childRight, childBottom);
			accumulatedChildrenWidth = childRight + rightMargin;

			// Is it the last child? Cache its end.
			if(i + 1 == childCount) contentEnd = accumulatedChildrenWidth;
		}

		// Cache view port end.
		viewPortEnd = getMeasuredWidth() - getPaddingRight();
	}

	private void layoutVertical(final int l, final int t, final int r, final int b)
	{
		// Just layout them vertically one by one.
		final int leftPadding = getPaddingLeft();
		final int topPadding = getPaddingTop();
		final int childCount = getChildCount();
		int accumulatedChildrenHeight = topPadding;
		for(int i = 0; i < childCount; ++i)
		{
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			final boolean allowVerticalMargin = !lp.isForwardeeContainer;
			final int childWidth = child.getMeasuredWidth();
			final int childHeight = child.getMeasuredHeight();
			final int topMargin = allowVerticalMargin ? lp.topMargin : 0;
			final int bottomMargin = allowVerticalMargin ? lp.bottomMargin : 0;

			// Take into account of margin and padding.
			final int childLeft = l + leftPadding + lp.leftMargin;
			final int childTop = accumulatedChildrenHeight + topMargin;
			final int childRight = childLeft + childWidth;
			final int childBottom = childTop + childHeight;

			child.layout(childLeft, childTop, childRight, childBottom);
			accumulatedChildrenHeight = childBottom + bottomMargin;

			// Is it the last child? Cache its end.
			if(i + 1 == childCount) contentEnd = accumulatedChildrenHeight;
		}

		// Cache view port end.
		viewPortEnd = getMeasuredHeight() - getPaddingBottom();
	}

	public void setOrientation(final int orientation)
	{
		if(orientation != ORIENTATION_HORIZONTAL && orientation != ORIENTATION_VERTICAL)
		{
			throw new IllegalArgumentException(
					"orientation is neither ScrollForwarderView.ORIENTATION_HORIZONTAL nor " +
							"ScrollForwarderView.ORIENTATION_VERTICAL");
		}

		this.orientation = orientation;
	}

	public int getOrientation() { return orientation; }

	public void setForwardeeInContainer(@NonNull final Forwardee forwardee, @IdRes final int forwardeeContainerId)
	{
		// See if the given container id belongs to one of its direct children.
		final View forwardeeContainer = findDirectChildById(forwardeeContainerId);
		if(forwardeeContainer == null) throw new IllegalArgumentException(
				"this view has no direct child with the given id: " +
				getContext().getResources().getResourceName(forwardeeContainerId));

		// See if the child is a forwardee container.
		final LayoutParams lp = (LayoutParams) forwardeeContainer.getLayoutParams();
		if(!lp.isForwardeeContainer) throw new IllegalArgumentException(
				"the view with the given id " +
				getContext().getResources().getResourceName(forwardeeContainerId) + " " +
				"is not a forwardee container. set the given child as forwardee container " +
				"by calling ScrollForwarderView.LayoutParams.isForwardeeContainer = true " +
				"or declare in xml with attribute layout_isForwardeeContainer and value true");

		// See if the forwardee is associated with a view that is a child of container.
		final int forwardeeViewId = forwardee.getViewId();
		final View view = forwardeeContainer.findViewById(forwardeeViewId);
		if(view == null) throw new IllegalArgumentException(
				"the view associated with forwardee with id " +
				getContext().getResources().getResourceName(forwardee.getViewId()) + " " +
				"is not a child of scroll forwardee container nor the forwardee " +
				"container itself");

		// The arguments are valid. Now set the forwardee.
		// Is the container list already sorted?
		if(isContainerListSorted)
		{
			// Just iterate through existing container and replace forwardee.
			// For performance we assume the container's view is not null.
			for(ForwardeeContainerHolder container : containerHolderList)
			{
				if(container.forwardeeContainer.getId() == forwardeeContainerId)
				{
					container.forwardee = forwardee;

					// Since the forwardee might have scroll position different from that of the previous one,
					// we need to re-calculate self-and-forwarded scroll position.
					reCalculateSelfAndForwardedScroll();
					return;
				}
			}

			throw new IllegalStateException(
					"forwardee container list is sorted but no container has the given id: " +
					getContext().getResources().getResourceName(forwardeeContainerId));
		}

		// Ths container list is not yet sorted. It will be sorted later in measurement pass.
		// Search in sorting buffer.
		for(ForwardeeContainerHolder container : containerHolderSortingBuffer)
		{
			if(container.forwardeeContainer.getId() == forwardeeContainerId)
			{
				container.forwardee = forwardee;

				// Self-and-forwarded scroll position will be re-calculated in measurement pass. Just leave.
				return;
			}
		}

		// Container is not even in sorting buffer. Reserve a dummy to actual list and add
		// the given pair of container and forwardee to sorting buffer.
		containerHolderList.add(null);
		containerHolderSortingBuffer.add(new ForwardeeContainerHolder(forwardeeContainer, forwardee));
	}

	public @Nullable Forwardee getForwardeeInContainer(@IdRes final int forwardeeContainerId)
	{
		if(!isContainerListSorted) return null;

		for(final ForwardeeContainerHolder container : containerHolderList)
		{
			if(container.forwardeeContainer.getId() == forwardeeContainerId)
			{
				return container.forwardee;
			}
		}

		return null;
	}

	/**
	 * Similar to {@link #findViewById(int)}, except that this method only find direct children.
	 * @param id The id of the view to find.
	 * @return The view whose id is the same as the given id. <code>null</code> when no such
	 * view is found.
	 */
	private @Nullable View findDirectChildById(@IdRes final int id)
	{
		final int childCount = getChildCount();
		for(int i = 0; i < childCount; ++i)
		{
			final View child = getChildAt(i);
			if(child.getId() == id)
			{
				return child;
			}
		}

		return null;
	}

	/**
	 * Handle scrolling. For each axis, either horizontal or vertical, the algorithm is divided into two
	 * possible routes:
	 * <p>
	 * a. If this is scrolling towards negative, i.e. either upwards or towards left-hand side, it checks
	 * whether forwardee view has ever scrolled. If forwardee view did scroll, scroll forwardee view. When
	 * forwardee view reaches its top, or that forwardee has never scrolled at all, scroll this view itself.
	 * <p>
	 * b. If this is scrolling towards positive, i.e. either downwards or towards right-hand side, it checks
	 * whether anchor view has reached the top of this view. If it has reached the top, scroll forwardee view;
	 * scroll this view itself otherwise.
	 * <p>
	 * No matter which route this method goes, if this view itself has ever scrolled, the view bound of scroll
	 * forwardee's view will be enlarged or shrunk accordingly so that it always fills the whole layout.
	 * @param dx The amount to scroll in x axis.
	 * @param dy The amount to scroll in y axis.
	 */
	private void handleScroll(int dx, int dy)
	{
		// Toggle between self-scroll state and scroll forwarding state,
		// until either state consumes all scroll.
		if(orientation == ORIENTATION_HORIZONTAL && dx != 0)
		{
			while(true)
			{
				switch(scrollState)
				{
					case STATE_SCROLL_SCROLLING_SELF:
						dx = scrollSelfHorizontallyBy(dx);
						break;
					case STATE_SCROLL_FORWARDING:
						dx = forwardScrollHorizontallyBy(dx);
						break;
				}

				if(dx == 0) break;
				else toggleScrollState();
			}
		}
		else if(dy != 0)
		{
			while(true)
			{
				switch(scrollState)
				{
					case STATE_SCROLL_SCROLLING_SELF:
						dy = scrollSelfVerticallyBy(dy);
						break;
					case STATE_SCROLL_FORWARDING:
						dy = forwardScrollVerticallyBy(dy);
						break;
				}

				if(dy == 0) break;
				else toggleScrollState();
			}
		}
	}

	private void toggleScrollState()
	{
		if(scrollState == STATE_SCROLL_FORWARDING) scrollState = STATE_SCROLL_SCROLLING_SELF;
		else scrollState = STATE_SCROLL_FORWARDING;
	}

	private int scrollSelfHorizontallyBy(final int dx)
	{
		final int remainderDx;
		final int selfDx;

		// Scrolling leftward?
		if(dx < 0)
		{
			// See if previous forwardee container's right is about to be scrolled over screen right.
			if(currentContainerHolderIndex > 0)
			{
				final int screenRight = getScrollX() + viewPortEnd;
				selfDx = clampVelocity(previousContainerEnd, screenRight, dx, true);
				remainderDx = dx - selfDx;

				// Reached previous forwardee container?
				if(remainderDx != 0)
				{
					// Set previous container as the current container so that it can scroll right away.
					passTokenToPreviousContainer();
				}
			}
			else
			{
				// There is no previous forwardee container. Just clamp against its left.
				selfDx = clampVelocity(0, getScrollX(), dx, true);
				remainderDx = 0;
			}
		}
		else // Scrolling rightward.
		{
			if(currentContainerHolderIndex < forwardeeContainerCount)
			{
				// See if the current forwardee container's left is about to be scrolled over screen left.
				final int screenLeft = getScrollX() + getPaddingLeft();
				selfDx = clampVelocity(currentContainerStart, screenLeft, dx, false);
				remainderDx = dx - selfDx;
			}
			else
			{
				// We have scrolled over the last container. Just clamp against last child's right.
				final int screenRight = getScrollX() + viewPortEnd;
				selfDx = clampVelocity(contentEnd, screenRight, dx, false);
				remainderDx = 0;
			}
		}

		selfAndForwardedScrollX += selfDx;
		scrollSelfBy(selfDx, 0);
		return remainderDx;
	}

	private int scrollSelfVerticallyBy(final int dy)
	{
		final int remainderDy;
		final int selfDy;

		// Scrolling upward?
		if(dy < 0)
		{
			// See if previous forwardee container's bottom is about to be scrolled over screen bottom.
			if(currentContainerHolderIndex > 0)
			{
				final int screenBottom = getScrollY() + viewPortEnd;
				selfDy = clampVelocity(previousContainerEnd, screenBottom, dy, true);
				remainderDy = dy - selfDy;

				// Reached previous forwardee container?
				if(remainderDy != 0)
				{
					// Set previous container as the current container so that it can scroll right away.
					passTokenToPreviousContainer();
				}
			}
			else
			{
				// There is no previous forwardee container. Just clamp against its top.
				selfDy = clampVelocity(0, getScrollY(), dy, true);
				remainderDy = 0;
			}
		}
		else // Scrolling downward.
		{
			if(currentContainerHolderIndex < forwardeeContainerCount)
			{
				// See if the current forwardee container's top is about to be scrolled over screen top.
				final int screenTop = getScrollY() + getPaddingTop();
				selfDy = clampVelocity(currentContainerStart, screenTop, dy, false);
				remainderDy = dy - selfDy;
			}
			else
			{
				// We have scrolled over the last container. Just clamp against last child's bottom.
				final int screenBottom = getScrollY() + viewPortEnd;
				selfDy = clampVelocity(contentEnd, screenBottom, dy, false);
				remainderDy = 0;
			}
		}

		selfAndForwardedScrollY += selfDy;
		scrollSelfBy(0, selfDy);
		return remainderDy;
	}

	private void scrollSelfBy(final int dx, final int dy)
	{
		super.scrollTo(getScrollX() + dx, getScrollY() + dy);
	}

	private int forwardScrollHorizontallyBy(final int dx)
	{
		final int remainderDx = currentForwardee.scrollHorizontally(dx);
		final int scrolledDx = dx - remainderDx;
		selfAndForwardedScrollX += scrolledDx;
		if(remainderDx > 0) passTokenToNextContainer();
		return remainderDx;
	}

	private int forwardScrollVerticallyBy(final int dy)
	{
		final int remainderDy = currentForwardee.scrollVertically(dy);
		final int scrolledDy = dy - remainderDy;
		selfAndForwardedScrollY += scrolledDy;
		if(remainderDy > 0) passTokenToNextContainer();
		return remainderDy;
	}

	private void setCurrentContainerHolderIndex(int index)
	{
		currentContainerHolderIndex = index;
		cacheForwardeeContainerBound();
	}

	private void passTokenToPreviousContainer()
	{
		--currentContainerHolderIndex;
		cacheForwardeeContainerBound();
	}

	private void passTokenToNextContainer()
	{
		++currentContainerHolderIndex;
		cacheForwardeeContainerBound();
	}

	private void cacheForwardeeContainerBound()
	{
		// Branch by orientation first to avoid multiple check on orientation since we have two assignments.
		if(orientation == ORIENTATION_HORIZONTAL)
		{
			if(currentContainerHolderIndex > 0)
			{
				final int prevContainerHolderIndex = currentContainerHolderIndex - 1;
				final ForwardeeContainerHolder prevHolder = containerHolderList.get(prevContainerHolderIndex);
				final View forwardeeContainer = prevHolder.forwardeeContainer;
				previousContainerEnd = forwardeeContainer.getRight();
			}

			if(currentContainerHolderIndex < forwardeeContainerCount)
			{
				final ForwardeeContainerHolder currentHolder = containerHolderList.get(currentContainerHolderIndex);
				final View forwardeeContainer = currentHolder.forwardeeContainer;
				currentContainerStart = forwardeeContainer.getLeft();

				currentForwardeeContainer = forwardeeContainer;
				currentForwardee = currentHolder.forwardee;
			}
		}
		else
		{
			if(currentContainerHolderIndex > 0)
			{
				final int prevContainerHolderIndex = currentContainerHolderIndex - 1;
				final ForwardeeContainerHolder prevHolder = containerHolderList.get(prevContainerHolderIndex);
				final View forwardeeContainer = prevHolder.forwardeeContainer;
				previousContainerEnd = forwardeeContainer.getBottom();
			}

			if(currentContainerHolderIndex < forwardeeContainerCount)
			{
				final ForwardeeContainerHolder currentHolder = containerHolderList.get(currentContainerHolderIndex);
				final View forwardeeContainer = currentHolder.forwardeeContainer;
				currentContainerStart = forwardeeContainer.getTop();

				currentForwardeeContainer = forwardeeContainer;
				currentForwardee = currentHolder.forwardee;
			}
		}
	}

	private void reCalculateSelfAndForwardedScroll()
	{
		if(orientation == ORIENTATION_HORIZONTAL) reCalculateSelfAndForwardedScrollX();
		else reCalculateSelfAndForwardedScrollY();
	}

	private void reCalculateSelfAndForwardedScrollX()
	{
		selfAndForwardedScrollX = getScrollX();
		for(int i = 0; i < forwardeeContainerCount; ++i)
		{
			selfAndForwardedScrollX += containerHolderList.get(i).forwardee.getScrollX();
			if(i >= currentContainerHolderIndex) break;
		}
	}

	private void reCalculateSelfAndForwardedScrollY()
	{
		selfAndForwardedScrollY = getScrollY();
		for(int i = 0; i < forwardeeContainerCount; ++i)
		{
			selfAndForwardedScrollY += containerHolderList.get(i).forwardee.getScrollY();
			if(i >= currentContainerHolderIndex) break;
		}
	}

	/**
	 * @return The sum of scrolled distance in x axis of this view and forwardee views.
	 */
	public int getSelfAndForwardedScrollX()
	{
		return selfAndForwardedScrollX;
	}

	/**
	 * @return The sum of scrolled distance in y axis of this view and forwardee views.
	 */
	public int getSelfAndForwardedScrollY()
	{
		return selfAndForwardedScrollY;
	}

	/**
	 * This is a convenient method to clamp the velocity of a moving against a boundary.
	 * @param boundary The boundary that the point must not pass through.
	 * @param point The point that is moving.
	 * @param velocity The velocity of the moving point.
	 * @param clampNegative If this is <code>true</code>, the point is not allowed to go to the negative side of the
	 *                      boundary; if this is <code>false</code>, the point is allowed to go to the positive side
	 *                      of the boundary.
	 * @return The clamped velocity.
	 */
	private int clampVelocity(final int boundary, final int point, final int velocity, final boolean clampNegative)
	{
		final int destination = point + velocity;
		final int correction = destination - boundary;
		if((clampNegative && correction < 0)
			|| (!clampNegative && correction > 0)) return velocity - correction;
		else return velocity;
	}
}
