package net.cafox.test;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.cafox.navigation.CachedSceneProvider;
import net.cafox.navigation.Scene;
import net.cafox.navigation.SceneNavigator;
import net.cafox.navigation.SceneNavigator.SceneHandler;
import net.cafox.navigation.SceneNavigator.SimpleSceneHandler;
import net.cafox.navigation.SceneProvider;

public class SceneNavigatorTest extends AppCompatActivity
{
	private final static int SCENE_A = 0;

	private final static int SCENE_B = 1;

	private final static int SCENE_C = 2;

	private class AView extends TextView implements Scene
	{
		public AView(Context context)
		{
			super(context);
			setGravity(Gravity.CENTER);
			setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			setText("A View");
		}

		@NonNull
		@Override
		public View getView()
		{
			return this;
		}

		@Override
		public int getSceneId()
		{
			return SCENE_A;
		}

		@Override
		public void onHide()
		{
		}

		@Override
		public void onShow()
		{
		}

		@Override
		public boolean onBack()
		{
			return false;
		}

		@Override
		public void onSetArgument(Object argument)
		{
		}
	}

	private class BView extends TextView implements Scene
	{
		public BView(Context context)
		{
			super(context);
			setGravity(Gravity.CENTER);
			setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			setText("B View");
		}

		@NonNull
		@Override
		public View getView()
		{
			return this;
		}

		@Override
		public int getSceneId()
		{
			return SCENE_B;
		}

		@Override
		public void onHide()
		{
		}

		@Override
		public void onShow()
		{
		}

		@Override
		public boolean onBack()
		{
			return false;
		}

		@Override
		public void onSetArgument(Object argument)
		{
		}
	}

	private class CView extends TextView implements Scene
	{
		public CView(Context context)
		{
			super(context);
			setGravity(Gravity.CENTER);
			setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			setText("C View");
		}

		@NonNull
		@Override
		public View getView()
		{
			return this;
		}

		@Override
		public int getSceneId()
		{
			return SCENE_C;
		}

		@Override
		public void onHide()
		{
		}

		@Override
		public void onShow()
		{
		}

		@Override
		public boolean onBack()
		{
			return false;
		}

		@Override
		public void onSetArgument(Object argument)
		{
		}
	}

	private SceneNavigator<Scene> sceneNavigator;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scene_navigator_test);
		final FrameLayout root = (FrameLayout) findViewById(R.id.root);
		SceneProvider<Scene> provider = new CachedSceneProvider<>(root, new Scene[]
				{
						new AView(this),
						new BView(this),
						new CView(this)
				});
		SceneHandler<Scene> sceneHandler = new SimpleSceneHandler<>(provider);
		sceneNavigator = new SceneNavigator<>();
		sceneNavigator.showDefaultScene(sceneHandler, SCENE_A);
	}

	@Override
	public void onBackPressed()
	{
		if(!sceneNavigator.back()) super.onBackPressed();
	}
}