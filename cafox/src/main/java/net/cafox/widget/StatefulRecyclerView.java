package net.cafox.widget;

import android.content.Context;
import android.database.Observable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * A recycler view that can toggle the visibility of other views according to the state of its adapter. The adapter must be
 * a {@link StatefulAdapter} or its subclass. Setting the adapter other than <code>StatefulAdapter</code> result in an
 * {@link UnsupportedOperationException}.
 * <p>
 * Supported states of the adapter are
 * {@link StatefulAdapter#STATE_DISPLAY}, {@link StatefulAdapter#STATE_EMPTY}, and {@link StatefulAdapter#STATE_LOADING}.
 * The name of these states are self-explanatory: <code>STATE_DISPLAY</code> is the state where the adapter has
 * at least one item; <code>STATE_EMPTY</code> is the state where the adapter has no item;
 * <code>STATE_LOADING</code> is where the adapter is loading contents, usually from the Internet or internal
 * storage.
 * <p>
 * One can set a view to be displayed when the adapter is in
 * <code>STATE_EMPTY</code> by calling {@link #setEmptyView(View)}, and in
 * <code>STATE_LOADING</code> by {@link #setLoadingView(View)}. In
 * <code>STATE_DISPLAY</code>, adapter contents are displayed. Just as <code>StatefulAdapter</code> can only be in one
 * state at a time, these views are mutually exclusive. That is, only one view among loading view, empty view or adapter
 * content will be displayed at a time.
 * <p>
 * Since the default state of <code>StatefulAdapter</code> is <code>STATE_EMPTY</code>, an empty view will be displayed by
 * default. If not empty view is set, nothing will be shown.
 * <p>
 * For more information about <code>StatefulAdapter</code>, see {@link StatefulAdapter}.
 */
@SuppressWarnings("UnusedDeclaration")
public class StatefulRecyclerView extends RecyclerView
{
	/**
	 * Observer interface used to broadcast adapter's state change event.
	 */
	public interface OnAdapterStateChangeObserver
	{
		void onAdapterStateChange(final int state);
	}

	/**
	 * A subclass of a normal recycler view adapter which should be used together with {@link StatefulRecyclerView}. This
	 * adapter handles its internal states automatically. These states are {@link #STATE_DISPLAY}, {@link #STATE_EMPTY},
	 * and {@link #STATE_LOADING}. <code>STATE_DISPLAY</code> is the state where the adapter has at least one item;
	 * <code>STATE_EMPTY</code> is the state where the adapter has no item; <code>STATE_LOADING</code> is where the adapter
	 * is loading contents, usually from the Internet or internal storage.
	 * <p>
	 * <code>STATE_DISPLAY</code> and
	 * <code>STATE_EMPTY</code> are managed, and notified by the adapter automatically when {@link #notifyDataSetChanged()}
	 * or other equivalent methods which modify adapter contents are called. To set adapter state to <code>STATE_LOADING</code>
	 * and to broadcast this state change event, subclass of this adapter class must call {@link #notifyLoadingState()}. To
	 * switch from loading state back to other states, one simply needs to call {@link #notifyDataSetChanged()} or other
	 * equivalent methods again.
	 * <p>
	 * The default adapter state is <code>STATE_EMPTY</code>.
	 */
	public static abstract class StatefulAdapter<VH extends ViewHolder> extends RecyclerView.Adapter<VH>
	{
		/**
		 * Internal list of {@link OnAdapterStateChangeObserver} used to broadcast event.
		 */
		private class OnAdapterStateChangeObservable extends Observable<OnAdapterStateChangeObserver>
		{
			/**
			 * Broadcast adapter state to all observers.
			 * @param state The current state of the adapter.
			 */
			private void notifyAdapterStateChange(final int state)
			{
				for(OnAdapterStateChangeObserver observer : mObservers) observer.onAdapterStateChange(state);
			}
		}

		/**
		 * State value in which {@link StatefulAdapter} has at least one item.
		 */
		public final static int STATE_DISPLAY = 1;

		/**
		 * State value in which {@link StatefulAdapter} has no item.
		 */
		public final static int STATE_EMPTY = 2;

		/**
		 * State value in which {@link StatefulAdapter} is loading its item contents.
		 */
		public final static int STATE_LOADING = 3;

		/**
		 * Internal adapter state.
		 */
		private int state = STATE_EMPTY;
		private OnAdapterStateChangeObservable onAdapterStateChangeObservable = new OnAdapterStateChangeObservable();

		/**
		 * Internal data observer used to listen to its date set changed events. This is when the adapter's
		 * <code>STATE_DISPLAY</code> and <code>STATE_EMPTY</code> state change event are broadcast.
		 */
		private AdapterDataObserver adapterDataObserver = new AdapterDataObserver()
		{
			@Override
			public void onChanged() { setEmptyOrDisplayStateAndNotify(); }

			@Override
			public void onItemRangeChanged(final int positionStart, final int itemCount) { setEmptyOrDisplayStateAndNotify(); }

			@Override
			public void onItemRangeInserted(final int positionStart, final int itemCount) { setEmptyOrDisplayStateAndNotify(); }

			@Override
			public void onItemRangeRemoved(final int positionStart, final int itemCount) { setEmptyOrDisplayStateAndNotify(); }

			@Override
			public void onItemRangeMoved(final int fromPosition, final int toPosition, final int itemCount) { setEmptyOrDisplayStateAndNotify(); }
		};

		/**
		 * Listen to its own data set change events.
		 * @see #adapterDataObserver
		 */
		public StatefulAdapter()
		{
			registerAdapterDataObserver(adapterDataObserver);
		}

		/**
		 * Get its internal state.
		 * @return The current state of the adapter.
		 */
		public int getState() { return state; }

		/**
		 * Register a {@link OnAdapterStateChangeObserver} to this adapter.
		 * @param observer The observer to register.
		 */
		public void registerOnAdapterStateChangeObserver(@NonNull final OnAdapterStateChangeObserver observer) { onAdapterStateChangeObservable.registerObserver(observer); }

		/**
		 * Unregister a {@link OnAdapterStateChangeObserver} from this adapter.
		 * @param observer The observer to unregister.
		 */
		public void unregisterOnAdapterStateChangeObserver(@NonNull final OnAdapterStateChangeObserver observer) { onAdapterStateChangeObservable.unregisterObserver(observer); }

		/**
		 * Set its internal state to {@link #STATE_LOADING} and notify all registered {@link OnAdapterStateChangeObserver}
		 * that this adapter is now in <code>STATE_LOADING</code>. Subclass of this adapter class should call this method
		 * when the adapter is loading contents from the Internet, internal storage or other content providers so that the
		 * {@link StatefulRecyclerView} associated with this adapter can display its loading view to users.
		 */
		protected final void notifyLoadingState()
		{
			state = STATE_LOADING;
			onAdapterStateChangeObservable.notifyAdapterStateChange(state);
		}

		/**
		 * Determine whether the adapter is in {@link #STATE_DISPLAY} or {@link #STATE_EMPTY}, and broadcast the state.
		 */
		private void setEmptyOrDisplayStateAndNotify()
		{
			state = getItemCount() == 0 ? STATE_EMPTY : STATE_DISPLAY;
			onAdapterStateChangeObservable.notifyAdapterStateChange(state);
		}
	}

	private View loadingView;
	private View emptyView;

	/**
	 * This is where this view listens to its {@link StatefulAdapter} and toggle view visibility accordingly.
	 */
	private OnAdapterStateChangeObserver onAdapterStateChangeObserver = new OnAdapterStateChangeObserver()
	{
		@Override
		public void onAdapterStateChange(int state)
		{
			toggleViewVisibility(state);
		}
	};

	public StatefulRecyclerView(@NonNull final Context context) { super(context); }

	public StatefulRecyclerView(@NonNull final Context context, @NonNull final AttributeSet attrs) { super(context, attrs); }

	public StatefulRecyclerView(@NonNull final Context context, @NonNull final AttributeSet attrs, final int defStyle) { super(context, attrs, defStyle); }

	/**
	 * One should not use this to set adapter for this recycler view since a {@link StatefulAdapter} is
	 * required. {@link #setAdapter(StatefulAdapter)} should be called instead.
	 * @param adapter The adapter to set.
	 * @throws UnsupportedOperationException When this method is called.
	 */
	@Override
	public void setAdapter(@Nullable final Adapter adapter)
	{
		throw new UnsupportedOperationException("setAdapter(Adapter) is not supported. call setAdapter(StatefulAdapter) instead");
	}

	@Override
	public boolean isInEditMode() { return true; }

	/**
	 * Set the given adapter to this recycler view and change its view visibility according to the state of the adapter.
	 * Previously added adapter would no longer be listened to.
	 * <p>
	 * This method triggers a layout request.
	 * @param adapter The adapter to set.
	 * @see StatefulAdapter
	 */
	public void setAdapter(@Nullable final StatefulAdapter adapter)
	{
		if(getAdapter() == adapter) return;

		StatefulAdapter oldAdapter = (StatefulAdapter) getAdapter();
		if(oldAdapter != null) oldAdapter.unregisterOnAdapterStateChangeObserver(onAdapterStateChangeObserver);
		super.setAdapter(adapter);
		if(adapter != null) adapter.registerOnAdapterStateChangeObserver(onAdapterStateChangeObserver);

		toggleViewVisibilityAndRequestLayout();
	}

	/**
	 * Set the loading view to be displayed when its adapter is loading content.
	 * <p>
	 * This method triggers a layout request.
	 * @see StatefulAdapter
	 */
	public void setLoadingView(@Nullable final View loadingView)
	{
		if(this.loadingView == loadingView) return;
		this.loadingView = loadingView;

		toggleViewVisibilityAndRequestLayout();
	}

	/**
	 * Set the empty view to be displayed when its adapter has not item.
	 * <p>
	 * This method triggers a layout request.
	 * @param emptyView The empty view to display.
	 * @see StatefulAdapter
	 */
	public void setEmptyView(@Nullable final View emptyView)
	{
		if(this.emptyView == emptyView) return;
		this.emptyView = emptyView;

		toggleViewVisibilityAndRequestLayout();
	}

	/**
	 * Internal convenient method to toggle view visibility and then request layout.
	 */
	private void toggleViewVisibilityAndRequestLayout()
	{
		StatefulAdapter adapter = (StatefulAdapter) getAdapter();
		int state = adapter.getState();
		toggleViewVisibility(state);
		requestLayout();
	}

	/**
	 * Internal method which determine what view should be displayed according to the given state.
	 * @param state The state to be referenced. It should be the current state of its adapter.
	 */
	private void toggleViewVisibility(final int state)
	{
		switch(state)
		{
		case StatefulAdapter.STATE_DISPLAY:
			showDisplayView();
			break;
		case StatefulAdapter.STATE_EMPTY:
			showEmptyView();
			break;
		case StatefulAdapter.STATE_LOADING:
			showLoadingView();
			break;
		}
	}

	private void showLoadingView()
	{
		if(loadingView != null) loadingView.setVisibility(View.VISIBLE);
		setVisibility(View.GONE);
		if(emptyView != null) emptyView.setVisibility(View.GONE);
	}

	private void showDisplayView()
	{
		if(loadingView != null) loadingView.setVisibility(View.GONE);
		setVisibility(VISIBLE);
		if(emptyView != null) emptyView.setVisibility(View.GONE);
	}

	private void showEmptyView()
	{
		if(loadingView != null) loadingView.setVisibility(View.GONE);
		setVisibility(View.GONE);
		if(emptyView != null) emptyView.setVisibility(View.VISIBLE);
	}
}
