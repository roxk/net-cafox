package net.cafox.navigation;

import android.support.annotation.NonNull;
import android.view.ViewGroup;

public abstract class AllocateSceneProvider<S extends Scene> implements SceneProvider<S>
{
	private ViewGroup container;

	public AllocateSceneProvider(ViewGroup container)
	{
		this.container = container;
	}

	@Override
	public void showScene(@NonNull S scene)
	{
		container.addView(scene.getView());
	}

	@Override
	public void hideScene(@NonNull S scene)
	{
		container.removeView(scene.getView());
	}
}
