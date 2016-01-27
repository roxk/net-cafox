package net.cafox.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.cafox.widget.ScrollForwarderView;
import net.cafox.widget.StatefulRecyclerView;

public class ScrollForwarderViewTest extends Activity
{
	private class TextViewHolder extends RecyclerView.ViewHolder
	{
		public TextViewHolder(TextView itemView) { super(itemView); }
	}

	private class DummyAdapter extends StatefulRecyclerView.StatefulAdapter<TextViewHolder>
	{
		private final static int ITEM_COUNT = 100;

		@Override
		public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
		{
			TextView itemView = new TextView(ScrollForwarderViewTest.this);
			itemView.setLayoutParams(new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));
			itemView.setGravity(Gravity.CENTER);
			itemView.setTextSize(20);
			itemView.setTextColor(Color.BLACK);
			itemView.setBackgroundColor(Color.argb((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
			itemView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Toast.makeText(ScrollForwarderViewTest.this, "clicked: " + ((TextView) v).getText(), Toast.LENGTH_SHORT).show();
				}
			});

			return new TextViewHolder(itemView);
		}

		@Override
		public void onBindViewHolder(TextViewHolder holder, int position)
		{
			TextView text = (TextView) holder.itemView;
			text.setText("" + position);
		}

		@Override
		public int getItemCount()
		{
			return ITEM_COUNT;
		}
	}

	private final static int CONTENT_COUNT = 4;

	private final static int[] TAB_ID = {
											R.id.tab_1,
											R.id.tab_2,
											R.id.tab_3,
											R.id.tab_4
										};

	private final static int[] CONTENT_ID = 	{
													R.id.content_1,
													R.id.content_2,
													R.id.content_3,
													R.id.content_4
												};

	private DummyAdapter adapter = new DummyAdapter();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scroll_forwarder_view_test);
		ScrollForwarderView scrollForwarderView = (ScrollForwarderView) findViewById(R.id.root);
		final ScrollView scrollView = (ScrollView) scrollForwarderView.findViewById(R.id.single_forwardee_as_container);
		for(int i = 0; i < CONTENT_COUNT; ++i)
		{
			StatefulRecyclerView recyclerView = (StatefulRecyclerView) scrollForwarderView.findViewById(CONTENT_ID[i]);
			recyclerView.setAdapter(adapter);
			recyclerView.setLayoutManager(new LinearLayoutManager(this));
		}
		adapter.notifyDataSetChanged();

		// Set up the first forwardee, which is at the same also a container.
		// Note how forwardee return the same id as what is set as container in
		// setForwardeeInContainer(Forwardee, int).
		ScrollForwarderView.Forwardee firstForwardee = new ScrollForwarderView.Forwardee()
		{
			@Override
			public int getViewId()
			{
				return scrollView.getId();
			}

			@Override
			public int getScrollX()
			{
				return scrollView.getScrollX();
			}

			@Override
			public int getScrollY()
			{
				return scrollView.getScrollY();
			}

			@Override
			public int scrollHorizontally(int dx)
			{
				return 0;
			}

			@Override
			public int scrollVertically(int dy)
			{
				if(scrollView.getChildCount() == 0) return dy;

				final int remainderDy;
				if(dy < 0)
				{
					final int scrollY = scrollView.getScrollY();
					final int newScrollY = scrollY + dy;
					remainderDy = newScrollY < 0 ? newScrollY : 0;
					dy -= remainderDy;
				}
				else if(dy > 0)
				{
					final int screenBottom = scrollView.getScrollY() + scrollView.getMeasuredHeight() - scrollView.getPaddingBottom();
					final int newScreenBottom = screenBottom + dy;
					final View child = scrollView.getChildAt(0);
					ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
					final int boundary = child.getMeasuredHeight() + lp.bottomMargin;
					final int correction = newScreenBottom - boundary;
					remainderDy = correction > 0 ? correction : 0;
					dy -= remainderDy;
				}
				else return dy;

				scrollView.scrollBy(0, dy);
				return remainderDy;
			}
		};
		scrollForwarderView.setForwardeeInContainer(firstForwardee, R.id.single_forwardee_as_container);

		// Set up the second forwardee. This time it is a view inside a view group.
		// Note how forwardee return the scrollable view's id, which is different
		// from what is set as container in setForwardeeInContainer(Forwardee, int).
		ScrollForwarderView.Forwardee secondForwardee = new ScrollForwarderView.Forwardee()
		{
			@Override
			public int getViewId()
			{
				return R.id.content_1;
			}

			@Override
			public int getScrollX()
			{
				return 0;
			}

			@Override
			public int getScrollY()
			{
				return 0;
			}

			@Override
			public int scrollHorizontally(int dx)
			{
				return dx;
			}

			@Override
			public int scrollVertically(int dy)
			{
				return dy;
			}
		};
		scrollForwarderView.setForwardeeInContainer(secondForwardee, R.id.multiple_forwardee_container);
	}
}
