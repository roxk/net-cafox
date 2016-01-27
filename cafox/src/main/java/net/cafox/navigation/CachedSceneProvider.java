package net.cafox.navigation;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/**
 * A {@link SceneProvider} designed to reinforce caching of scenes so that memory allocation, view layout and measurement
 * can be reduced to minimum. Developers no longer implements {@link #getScene(int)} in its sub-class, but supplies an array
 * of {@link Scene} through {@link #CachedSceneProvider(ViewGroup, S[])}. It is the responsibility of developers to make sure
 * that the array index of a scene matches its scene id.
 * @param <S> Sub class of <code>Scene</code> which provides scene-specific information, such as scene title.
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class CachedSceneProvider<S extends Scene> implements SceneProvider<S>
{
	private S[] scenes;

	/**
	 * Construct a cached scene provider. The view group container is where all scenes will reside. All views of scenes
	 * will be added to the container one by one according to their position in the array of scenes, starting from smaller
	 * index to larger index. Please note that this provider makes no attempts to guarantee that the container has no other
	 * views.
	 * @param container The view group container of all scenes.
	 * @param scenes The array of scenes. Make sure the array index of each scene matches its scene id.
	 */
	public CachedSceneProvider(@NonNull ViewGroup container, @NonNull S[] scenes)
	{
		this.scenes = scenes;

		final int sceneCount = scenes.length;
		for(int i = 0; i < sceneCount; ++i)
		{
			final S scene = scenes[i];
			if(scene == null) throw new IllegalArgumentException("scenes cannot contain null element");
			if(scene.getSceneId() != i)
			{
				throw new IllegalStateException("scene id " + scene.getSceneId()
						+ " of the scene " + scene.getClass().getSimpleName() +" is not the same as its index " + i
						+ " in scenes");
			}

			final View view = scene.getView();
			view.setVisibility(View.INVISIBLE);
			container.addView(view);
		}
	}

	@Override
	final public @NonNull S getScene(int sceneId)
	{
		return scenes[sceneId];
	}

	@Override
	final public int getSceneCount() { return scenes.length; }

	@Override
	public void showScene(@NonNull S scene)
	{
		scene.getView().setVisibility(View.VISIBLE);
	}

	@Override
	public void hideScene(@NonNull S scene)
	{
		scene.getView().setVisibility(View.INVISIBLE);
	}
}
