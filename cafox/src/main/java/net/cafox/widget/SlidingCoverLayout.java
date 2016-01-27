package net.cafox.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.database.Observable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

/**
 * A layout which works only with two children. Upon sliding request made by calling {@link #openCover()},
 * {@link #closeCover()}, or {@link #toggleCoverState()}, it slides the cover to the right until all of
 * the treasure beneath is visible. It is guaranteed to stop at where the two views just touch each other
 * so that no space is left in between.
 * <p>
 * Terminology:<br>
 * <i>Cover</i>: The child view on top which will be sliding back and forth, as if it is the cover
 * of a treasure box.<br>
 * <i>Treasure</i>: The child view beneath which just sits there, "waiting" to be revealed.
 */
// TODO: Add manually drag-to-open feature?
public class SlidingCoverLayout extends FrameLayout
{
	/**
	 * Listener used to listen to sliding events. For every sliding request, the events are guaranteed
	 * to be called in the following order:<p>
	 * 1. {@link #onSlidingStart()}
	 * <p>
	 * 2. {@link #onOpen()} or {@link #onClose()}
	 * <p>
	 * 3. {@link #onOpened()} or {@link #onClosed()}
	 * <p>
	 * 4. {@link #onSlidingEnd()}
	 */
	public static class OnSlidingListener
	{
		/**
		 * Called when the cover is about to slide.
		 */
		public void onSlidingStart() {}

		/**
		 * Called when the cover finished sliding.
		 */
		public void onSlidingEnd() {}

		/**
		 * Called when the cover is about to be slided to opened state. (i.e. {@link SlidingCoverLayout#openCover()} has been called)
		 */
		public void onOpen() {}

		/**
		 * Called when the cover is opened.
		 */
		public void onOpened() {}

		/**
		 * Called when the cover is about to be slided to closed state. (i.e. {@link SlidingCoverLayout#closeCover()} has been called)
		 */
		public void onClose() {}

		/**
		 * Called when the cover is closed.
		 */
		public void onClosed() {}
	}

	/**
	 * A convenient class that encapsulate event broadcast to all listeners.
	 */
	private static class OnSlidingObservable extends Observable<OnSlidingListener>
	{
		public void notifyOnSlidingStart()
		{
			for(OnSlidingListener onSlidingListener : mObservers)
			{
				onSlidingListener.onSlidingStart();
			}
		}

		public void notifyOnSlidingEnd()
		{
			for(OnSlidingListener onSlidingListener : mObservers)
			{
				onSlidingListener.onSlidingEnd();
			}
		}

		public void notifyOnOpen()
		{
			for(OnSlidingListener onSlidingListener : mObservers)
			{
				onSlidingListener.onOpen();
			}
		}

		public void notifyOnOpened()
		{
			for(OnSlidingListener onSlidingListener : mObservers)
			{
				onSlidingListener.onOpened();
			}
		}

		public void notifyOnClose()
		{
			for(OnSlidingListener onSlidingListener : mObservers)
			{
				onSlidingListener.onClose();
			}
		}

		public void notifyOnClosed()
		{
			for(OnSlidingListener onSlidingListener : mObservers)
			{
				onSlidingListener.onClosed();
			}
		}
	}

	/**
	 * Constant used to mark maximum children allowed for this view group.
	 */
	private final static int MAX_CHILD_COUNT = 2;

	/**
	 * Value used to indicate that this view's cover is closed.
	 */
	private final static int COVER_STATE_CLOSED = 1;

	/**
	 * Value used to indicate that this view's cover is opening.
	 */
	private final static int COVER_STATE_OPENING = 2;

	/**
	 * Value used to indicate that this view's cover is opened.
	 */
	private final static int COVER_STATE_OPENED = 3;

	/**
	 * Value used to indicate that this view's cover is closing.
	 */
	private final static int COVER_STATE_CLOSING = 4;

	/**
	 * Duration of cover sliding animation.
	 */
	private final static long SLIDING_DURATION = 250;

	/**
	 * Interpolator for sliding animation.
	 */
	// TODO: Since it highly resembles iOS translation, refactor to CAFOX for generic use?
	private Interpolator interpolator = new Interpolator()
	{
		@Override
		public float getInterpolation(float i)
		{
			i = 1.0f - i;
			return 1.0f - (float) Math.pow(i, (float) Math.pow(15, i) + 1.0f);
		}
	};

	/**
	 * Listener for sliding animation to broadcast this view's sliding events.
	 */
	private AnimatorListenerAdapter slidingAnimatorListener = new AnimatorListenerAdapter()
	{
		@Override
		public void onAnimationStart(Animator animation) { SlidingCoverLayout.this.onAnimationStart(); }

		@Override
		public void onAnimationEnd(Animator animation) { SlidingCoverLayout.this.onAnimationEnd(); }
	};

	private View treasure;
	private View cover;

	/**
	 * Internal sliding state. It is either {@link #COVER_STATE_CLOSED} or {@link #COVER_STATE_OPENING},
	 * {@link #COVER_STATE_OPENED}, or {@link #COVER_STATE_CLOSING}.
	 */
	private int coverState = COVER_STATE_CLOSED;

	/**
	 * The list that contains all <code>OnSlidingListener</code> client had registered.
	 */
	private OnSlidingObservable onSlidingObservable = new OnSlidingObservable();

	/**
	 * Cached width of bottom view.
	 */
	private int treasureWidth;

	/**
	 * Status to determine whether user is trying to close the drawer by touching outside the treasure.
	 */
	private boolean closeCover;

	public SlidingCoverLayout(Context context)
	{
		super(context);
	}

	public SlidingCoverLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public SlidingCoverLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void addView(@NonNull View view, int index, ViewGroup.LayoutParams params)
	{
		if(getChildCount() >= MAX_CHILD_COUNT) throw new IllegalStateException("this view can only have two direct children");

		if(getChildCount() == 0)
		{
			treasure = view;
			updateTreasureVisibility();
		}
		else cover = view;

		super.addView(view, index, params);
	}

	@Override
	public boolean onInterceptTouchEvent(@NonNull MotionEvent event)
	{
		switch(coverState)
		{
			case COVER_STATE_CLOSING:
			case COVER_STATE_OPENING:
				return true;
			case COVER_STATE_CLOSED:
				return false;
		}

		// Opened.
		final int x = (int) event.getX();
		final int action = event.getActionMasked();
		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				closeCover = x > treasureWidth;
				if(closeCover) requestDisallowInterceptTouchEvent(true);
				break;
			default:
				closeCover = false;
				break;
		}

		return closeCover;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event)
	{
		// Opened.
		final int x = (int) event.getX();
		final int action = event.getActionMasked();
		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				closeCover = x > treasureWidth;
				if(closeCover) requestDisallowInterceptTouchEvent(true);
				break;
			case MotionEvent.ACTION_MOVE:
				// If the user moves his finger inside the treasure once, consider this
				// not an attempt to close the cover.
				if(!closeCover) break;
				closeCover = x > treasureWidth;
				break;
			case MotionEvent.ACTION_UP:
				if(closeCover) closeCover();
				return true;
		}

		return true;
	}

	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b)
	{
		if(getChildCount() != MAX_CHILD_COUNT) throw new IllegalStateException("this view must work with two direct children");
		super.onLayout(changed, l, t, r, b);

		// Use getRight() instead of getMeasuredWidth() to take into account of padding and margin.
		treasureWidth = treasure.getRight();
	}

	@Override
	public void onAnimationStart()
	{
		super.onAnimationStart();

		if(coverState == COVER_STATE_CLOSED)
		{
			setCoverState(COVER_STATE_OPENING);
			onSlidingObservable.notifyOnSlidingStart();
			onSlidingObservable.notifyOnOpen();
		}
		else if(coverState == COVER_STATE_OPENED)
		{
			setCoverState(COVER_STATE_CLOSING);
			onSlidingObservable.notifyOnSlidingStart();
			onSlidingObservable.notifyOnClose();
		}
		else throw new IllegalStateException("cover state is not closed nor opened when cover is about to slide");
	}

	@Override
	public void onAnimationEnd()
	{
		super.onAnimationEnd();

		if(coverState == COVER_STATE_OPENING)
		{
			setCoverState(COVER_STATE_OPENED);
			onSlidingObservable.notifyOnOpened();
			onSlidingObservable.notifyOnSlidingEnd();
		}
		else if(coverState == COVER_STATE_CLOSING)
		{
			setCoverState(COVER_STATE_CLOSED);
			onSlidingObservable.notifyOnClosed();
			onSlidingObservable.notifyOnSlidingEnd();
		}
		else throw new IllegalStateException("cover state is not closing nor opening when cover finished sliding");
	}

	public void toggleCoverState()
	{
		if(coverState == COVER_STATE_CLOSED) openCover();
		else if(coverState == COVER_STATE_OPENED) closeCover();
	}

	/**
	 * This method takes effect <b>only</b> when its cover state is {@link #COVER_STATE_CLOSED}.
	 */
	public void openCover()
	{
		if(coverState != COVER_STATE_CLOSED) return;

		cover.animate()
				.x(treasureWidth)
				.setDuration(SLIDING_DURATION)
				.setInterpolator(interpolator)
				.setListener(slidingAnimatorListener)
				.start();
	}

	/**
	 * This method takes effect <b>only</b> when its cover state is {@link #COVER_STATE_OPENED}.
	 */
	public void closeCover()
	{
		if(coverState != COVER_STATE_OPENED) return;

		cover.animate()
				.x(getPaddingLeft())
				.setDuration(SLIDING_DURATION)
				.setInterpolator(interpolator)
				.setListener(slidingAnimatorListener)
				.start();
	}

	private void setCoverState(int state)
	{
		coverState = state;
		updateTreasureVisibility();
	}

	private void updateTreasureVisibility()
	{
		treasure.setVisibility(coverState == COVER_STATE_CLOSED ? View.INVISIBLE : View.VISIBLE);
	}

	public void registerOnSlidingListener(OnSlidingListener onSlidingListener) { onSlidingObservable.registerObserver(onSlidingListener); }

	public void unregisterOnSlidingListener(OnSlidingListener onSlidingListener) { onSlidingObservable.unregisterObserver(onSlidingListener); }
}