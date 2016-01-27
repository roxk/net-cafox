package net.cafox.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A class that encapsulates navigation command and delegates actual command handling to {@link Scene} and
 * {@link SceneHandler}. Developers will typically call the three navigation commands, {@link #goTo(int, Object)},
 * {@link #back()}, and {@link #reset(int, Object)} to navigate between scenes.
 * <p>
 * Notice how in each call a <b>scene id</b>, implemented as an integer, is used as an identifier of a scene. Client
 * should always create constants which are human-readable and use supply the above calls with these constants.
 * Consider the following example:
 * <p>
 * <code>
 * public final static int SCENE_A = 0;
 * <br>
 * public final static int SCENE_B = 1;
 * <br>
 * ...
 * </code>
 * <p>
 * Suppose these constants are defined in a class <code>MainScene</code>. To go to scene A, one would then make
 * the following call: <br>
 * <code>sceneNavigator.goTo(MainScene.SCENE_A, null);</code>
 * @param <S> Sub class of <code>Scene</code> which provides scene-specific information, such as scene title.
 * @see Scene
 * @see SceneHandler
 */
public class SceneNavigator<S extends Scene> extends SceneManager<S>
{
	/**
	 * The scene handler of {@link SceneNavigator} which handles the actual navigation transition, namely, animating
	 * scenes in and out according to different navigation commands.
	 * @param <S> Sub class of <code>Scene</code> which provides scene-specific information, such as scene title.<br>
	 *           <b>NOTE:</b> This must be the same class as that passed to the <code>SceneNavigator</code> with which
	 *           it works.
	 */
	public interface SceneHandler<S extends Scene>
	{
		/**
		 * Get a scene that matches the given scene id.
		 * @param sceneId The scene id of the scene.
		 * @return The scene.
		 */
		@NonNull S getScene(int sceneId);

		/**
		 * @param defaultScene The default scene to show.
		 */
		void onShowDefaultScene(S defaultScene);

		/**
		 * @param showStackIndex The stack index of the scene that is going to be shown. This is always 0.
		 * @param showScene The show scene.
		 * @param hideStackIndex The stack index of the scene that is going to be hidden. Its range is 0 ~ scene stack
		 *                       size - 1.
		 * @param hideScene The hide scene.
		 * @param hideSceneCount The number of scenes to hide.
		 */
		void onResetHide(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene, int hideSceneCount);

		/**
		 * @param showStackIndex The stack index of the scene that is going to be shown. This is always 0.
		 * @param showScene The show scene.
		 * @param hideSceneCount The number of scenes to hide.
		 */
		void onResetShow(int showStackIndex, @NonNull S showScene, int hideSceneCount);

		/**
		 * @param showStackIndex The back stack index of the scene which is going to be shown.
		 * @param showScene The show scene.
		 * @param hideStackIndex The back stack index of the scene that is going to be hidden.
		 * @param hideScene The hide scene.
		 */
		void onReplace(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene);

		/**
		 * @param showStackIndex The back stack index of the scene which is going to be shown.
		 * @param showScene The show scene.
		 * @param hideStackIndex The back stack index of the scene that is going to be hidden.
		 * @param hideScene The hide scene.
		 */
		void onGoTo(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene);

		/**
		 * @param showStackIndex The stack index of the scene that is going to be shown.
		 * @param showScene The show scene.
		 * @param hideStackIndex The stack index of the scene that is going to be hidden.
		 * @param hideScene The hide scene.
		 */
		void onBack(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene);
	}

	/**
	 * A simple implementation of scene handler which simply hide and show scenes without animation.
	 * @param <S>
	 */
	public static class SimpleSceneHandler<S extends Scene> implements SceneHandler<S>
	{
		private SceneProvider<S> sceneProvider;

		public SimpleSceneHandler(@NonNull SceneProvider<S> sceneProvider)
		{
			this.sceneProvider = sceneProvider;
		}

		public @NonNull S getScene(int sceneId) { return sceneProvider.getScene(sceneId); }

		@Override
		public void onShowDefaultScene(S defaultScene)
		{
			sceneProvider.showScene(defaultScene);
		}

		@Override
		public void onResetHide(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene, int hideSceneCount)
		{
			sceneProvider.hideScene(hideScene);
		}

		@Override
		public void onResetShow(int showStackIndex, @NonNull S showScene, int hideSceneCount)
		{
			sceneProvider.showScene(showScene);
		}

		@Override
		public void onReplace(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene)
		{
			sceneProvider.hideScene(hideScene);
			sceneProvider.showScene(showScene);
		}

		@Override
		public void onGoTo(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene)
		{
			sceneProvider.hideScene(hideScene);
			sceneProvider.showScene(showScene);
		}

		@Override
		public void onBack(int showStackIndex, @NonNull S showScene, int hideStackIndex, @NonNull S hideScene)
		{
			sceneProvider.hideScene(hideScene);
			sceneProvider.showScene(showScene);
		}
	}

	private SceneHandler<S> sceneHandler;

	final public void showDefaultScene(@NonNull SceneHandler<S> sceneHandler, int defaultSceneId)
	{
		if(this.sceneHandler != null) throw new IllegalStateException("attempt to show default scene when it has already been shown");
		this.sceneHandler = sceneHandler;

		final S defaultScene = sceneHandler.getScene(defaultSceneId);
//		defaultScene.onShow();	TODO: Temporary bug fix.
		sceneHandler.onShowDefaultScene(defaultScene);

		pushDefaultScene(defaultScene);
	}

	/**
	 * Reset the default scene with the given scene. All existing scenes will be hidden and removed from the
	 * internal back stack, then the given scene will be shown as the default scene.
	 * <p>
	 * {@link SceneHandler#onResetHide(int, S, int, S, int)} and
	 * {@link SceneHandler#onResetShow(int, S, int)} will be used to hide and show the scenes.
	 * <p>
	 * {@link Scene#onHide()} and {@link Scene#onShow()} are called before hiding and show the scene.
	 * <p>
	 * {@link Scene#onSetArgument(Object)} is called before calling <code>onShow()</code>. It will be called
	 * regardless of whether the passed argument is null or not. Developers can hence check null argument and
	 * respond, for example, with an exception.
	 * @param sceneId The scene id of the default scene. It is encouraged that developers always use constants
	 *                instead of hardcoded integer.
	 * @param argument Optional argument supplied to the scene to reset. If it is <code>null</code> it will
	 *                 <b>NOT</b> be passed to the scene.
	 */
	final public void reset(int sceneId, @Nullable Object argument)
	{
		if(isLocked()) return;

		final int incomingSceneStackIndex = 0;
		final S incomingScene = sceneHandler.getScene(sceneId);
		final int hideSceneCount = getSceneStackCount();
		final int currentSceneStackIndex = hideSceneCount - 1;
		for(int i = currentSceneStackIndex; i >= 0; --i)
		{
			final S currentScene = getCurrentScene();
			currentScene.onHide();
			sceneHandler.onResetHide(incomingSceneStackIndex, incomingScene, i, currentScene, hideSceneCount);

			// Only pop non-default scene.
			if(i > 0) popScene();
			else replaceCurrentScene(incomingScene);
		}

		incomingScene.onSetArgument(argument);
		incomingScene.onShow();
		sceneHandler.onResetShow(incomingSceneStackIndex, incomingScene, hideSceneCount);
	}

	/**
	 * Replace the current scene with the given scene. The current will be hidden and the given scene will be
	 * shown.
	 * <p>
	 * {@link SceneHandler#onReplace(int, S, int, S)} will be used to hide and show the scenes.
	 * <p>
	 * {@link Scene#onHide()} and {@link Scene#onShow()} are called before hiding and show the scene.
	 * <p>
	 * {@link Scene#onSetArgument(Object)} is called before calling <code>onShow()</code>. It will be called
	 * regardless of whether the passed argument is null or not. Developers can hence check null argument and
	 * respond, for example, with an exception.
	 * @param sceneId The scene id of the scene which will replace the current scene. It is encouraged that
	 *                developers always use constants instead of hardcoded integer.
	 * @param argument Optional argument supplied to the scene to reset. If it is <code>null</code> it will
	 *                 <b>NOT</b> be passed to the scene.
	 */
	final public void replace(int sceneId, @Nullable Object argument)
	{
		if(isLocked()) return;

		final int currentSceneStackIndex = getSceneStackCount() - 1;
		final S incomingScene = sceneHandler.getScene(sceneId);
		final S currentScene = getCurrentScene();

		currentScene.onHide();
		incomingScene.onSetArgument(argument);
		incomingScene.onShow();
		sceneHandler.onReplace(currentSceneStackIndex, incomingScene, currentSceneStackIndex, currentScene);

		replaceCurrentScene(incomingScene);
	}

	/**
	 * Go to the scene with the given scene id. The current scene will be hidden, then the given scene will
	 * be added to an internal back stack, and the scene with the given scene will be shown.
	 * <p>
	 * {@link SceneHandler#onGoTo(int, S, int, S)} will be used to hide and show the scenes.
	 * <p>
	 * {@link Scene#onHide()} and {@link Scene#onShow()} are called before hiding and show the scene.
	 * <p>
	 * {@link Scene#onSetArgument(Object)} is called before calling <code>onShow()</code>. It will be called
	 * regardless of whether the passed argument is null or not. Developers can hence check null argument and
	 * respond, for example, with an exception.
	 * @param sceneId The scene id of the destination scene. It is encouraged that developers always use
	 *                constants instead of hardcoded integer.
	 * @param argument Optional argument supplied to the scene to go. If it is <code>null</code> it will
	 *                 <b>NOT</b> be passed to the scene.
	 */
	final public void goTo(int sceneId, Object argument)
	{
		if(isLocked()) return;

		final int incomingSceneStackIndex = getSceneStackCount();
		final int currentSceneStackIndex = incomingSceneStackIndex - 1;
		final S incomingScene = sceneHandler.getScene(sceneId);
		final S currentScene = getCurrentScene();

		currentScene.onHide();
		incomingScene.onSetArgument(argument);
		incomingScene.onShow();
		sceneHandler.onGoTo(incomingSceneStackIndex, incomingScene, currentSceneStackIndex, currentScene);

		pushScene(incomingScene);
	}

	/**
	 * Issue a back command. This method will return immediately when the back command is consumed.
	 * <p>
	 * It will first delegate the back command to the current scene. If the current scene consumed the back
	 * command, so be it and nothing else happens. It the current scene had not consumed the back command, this
	 * class tries to go back to a previous scene. A previous scene is the scene that was hidden by a previous
	 * call to {@link #goTo(int, Object)}, as recorded in an internal back stack. The back command is considered
	 * consumed when it does go back to a previous scene, and not consumed when it does not. It does not go back
	 * to a previous scene when the current scene is the default scene.
	 * <p>
	 * Whether or not the command is consumed is reflected in the return value. This can be used to, for example,
	 * determine whether an activity should finish itself.
	 * <p>
	 * {@link SceneHandler#onBack(int, S, int, S)} will be used to hide and show the scenes.
	 * <p>
	 * {@link Scene#onHide()} and {@link Scene#onShow()} are called before hiding and show the scene.
	 * @return <code>true</code> when the back command is consumed, <code>false</code> otherwise.
	 */
	final public boolean back()
	{
		if(isLocked()) return true;

		final S currentScene = getCurrentScene();
		if(currentScene.onBack()) return true;
		final int currentSceneStackIndex = getSceneStackCount() - 1;
		if(!popScene()) return false;

		final int previousSceneStackIndex = currentSceneStackIndex - 1;
		final S previousScene = getCurrentScene();
		currentScene.onHide();
		previousScene.onShow();
		sceneHandler.onBack(previousSceneStackIndex, previousScene, currentSceneStackIndex, currentScene);
		return true;
	}
}
