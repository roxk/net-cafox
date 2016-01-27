package net.cafox.navigation;

import android.support.annotation.NonNull;

/**
 * A class which encapsulates scene providing and scene's basic manipulating on views, namely, showing
 * and hiding.
 * @param <S> Sub class of <code>Scene</code> which provides scene-specific information, such as scene title.
 */
public interface SceneProvider<S extends Scene>
{
	/**
	 * Get a scene that matches the given scene id. This is meant to be called by the framework.
	 * @param sceneId The scene id of the scene.
	 * @return The scene.
	 */
	@NonNull S getScene(int sceneId);

	/**
	 * Get the number of scenes.
	 * @return The number of scenes.
	 */
	int getSceneCount();

	/**
	 * Show the given scene. This is meant to be called during scene transition.
	 * <p>
	 * <b>NOTE:</b>This class makes <b>NO</b> attempt to guarantee that the given scene belongs to this
	 * provider.
	 * @param scene The scene to show.
	 */
	void showScene(@NonNull S scene);

	/**
	 * Hide the given scene. This is meant to be called during scene transition.
	 * <p>
	 * <b>NOTE:</b>This class makes <b>NO</b> attempt to guarantee that the given scene belongs to this
	 * provider.
	 * @param scene The scene to hide.
	 */
	void hideScene(@NonNull S scene);
}