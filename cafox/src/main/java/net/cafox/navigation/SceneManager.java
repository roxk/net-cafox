package net.cafox.navigation;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The base class of all scene navigator. This class manages back stack and navigation locking mechanism.
 * <p>
 * By design there must be at least one scene, the default scene, in the back stack. See {@link #popScene()},
 * {@link #pushScene(Scene)}, {@link #replaceCurrentScene(Scene)} for more information on scene stack management.
 * <p>
 * When this manager is locked by calling {@link #setIsLocked(boolean)}, any attempt to modify the scene stack causes an exception to be thrown.
 * Subclasses should always check whether navigation is locked by calling {@link #isLocked()} before modifying the scene stack. They should also
 * always make sure their navigation behavior is locked accordingly.
 * @param <S> Sub class of <code>Scene</code> which provides scene-specific information, such as scene title.
 */
abstract class SceneManager<S extends Scene>
{
	private final static int MIN_SCENE_STACK_COUNT = 1;

	private List<S> sceneStack = new ArrayList<>();
	private boolean isLocked;
	private boolean isDefaultScenePushed;

	final void pushDefaultScene(S defaultScene)
	{
		isDefaultScenePushed = true;
		sceneStack.add(defaultScene);
	}

	/**
	 * Get the number of back stack of scenes.
	 * @return The number of back stack of scenes.
	 */
	final int getSceneStackCount() { return sceneStack.size(); }

	/**
	 * Pop a scene at the top of the stack. Since by design there must be at least one scene in the back stack, this method will not pop
	 * the default scene. Use {@link #replaceCurrentScene(Scene)} instead when developers want to set the default scene.
	 * @return <code>true</code> when it successfully pops a scene, <code>false</code> when it cannot pop a scene (i.e. the current scene
	 * is the default scene.
	 * @throws IllegalStateException When navigation is locked. See {@link #isLocked()}.
	 */
	final boolean popScene()
	{
		validate();
		if(sceneStack.size() <= MIN_SCENE_STACK_COUNT) return false;
		sceneStack.remove(sceneStack.size() - 1);
		return true;
	}

	/**
	 * Push a scene.
	 * @param scene The scene to push.
	 * @throws IllegalStateException When navigation is locked. See {@link #isLocked()}.
	 */
	final void pushScene(S scene)
	{
		validate();
		sceneStack.add(scene);
	}

	/**
	 * Switch the current scene in scene stack. Regardless of whether the current scene is the default scene, the given scene will
	 * replace the current scene.
	 * @throws IllegalStateException When navigation is locked. See {@link #isLocked()}.
	 */
	final void replaceCurrentScene(S scene)
	{
		// Assume there is always at least one scene.
		validate();
		sceneStack.set(sceneStack.size() - 1, scene);
	}

	/**
	 * Check whether<br>
	 * 1. it is not locked.<br>
	 * 2. the default is pushed to the stack.
	 * @throws IllegalStateException When any one of the above is false.
	 */
	private void validate()
	{
		checkIsLocked();
		checkIsDefaultScenePushed();
	}

	/**
	 * @throws IllegalStateException When it is locked.
	 */
	private void checkIsLocked()
	{
		if(isLocked()) throw new IllegalStateException("attempt to manipulate scene stack when navigation is locked, call setIsLocked(false) first");
	}

	/**
	 * @throws IllegalStateException When the default scene is not shown.
	 */
	private void checkIsDefaultScenePushed() { if(!isDefaultScenePushed) throw new IllegalStateException("attempt to manipulate scene stack without pushing default scene, call pushDefaultScene(S) first"); }

	/**
	 * Check whether navigation is locked. For details of locking navigation, see {@link #setIsLocked(boolean)}.
	 * @return <code>true</code> if navigation is locked, <code>false</code> otherwise.
	 */
	final protected boolean isLocked() { return isLocked; }

	/**
	 * Get the current scene. It is discouraged that developers manipulate the scene directly but this method is still
	 * provided for the sake of flexibility and completeness.
	 * @return The current scene.
	 */
	final public @NonNull S getCurrentScene() { return sceneStack.get(sceneStack.size() - 1); }

	/**
	 * Set whether navigation is locked. When navigation is locked, all navigation methods should return immediately.
	 * @param isLocked Passing <code>true</code> will lock navigation, <code>false</code> will unlock
	 *                 navigation.
	 */
	final public void setIsLocked(boolean isLocked) { this.isLocked = isLocked; }

	/**
	 * Call the current scene's {@link Scene#onHide()} method. This is meant to be used in either
	 * <code>onPause()</code> or <code>onStop()</code>.
	 */
	final public void onHide()
	{
		final S scene = getCurrentScene();
		scene.onHide();
	}

	/**
	 * Call the current scene's {@link Scene#onShow()} method. This is meant to be used in either
	 * <code>onResume()</code> or <code>onStart()</code>.
	 */
	final public void onShow()
	{
		final S scene = getCurrentScene();
		scene.onShow();
	}
}