package net.cafox.navigation;

import android.support.annotation.NonNull;

/**
 * A scene navigator which navigates between scenes sequentially and bidirectionally.  Since by design this class
 * work on tasks sequentially, the term <i>task index</i>, instead of <i>scene id</i>, will be used to called those
 * scene-identifying constants.
 */
public class TaskNavigator<S extends Scene> extends SceneManager<S>
{
	/**
	 * The task handler of {@link TaskNavigator} which handles the actual navigation transition, namely, animating
	 * scenes in and out according to different navigation commands.
	 * @param <S> Sub class of <code>Task</code> which provides scene-specific information, such as scene title.<br>
	 *           <b>NOTE:</b> This must be the same class as that passed to the <code>TaskNavigator</code> with which
	 *           it works.
	 */
	public interface TaskHandler<S extends Scene>
	{
		/**
		 * Get a task according to its index in a sequence of tasks. The first task has an index of <code>0</code>.
		 * @param taskIndex The index of the task.
		 * @return The task.
		 */
		@NonNull S getTask(int taskIndex);

		/**
		 * Get the number of tasks.
		 * @return The number of tasks.
		 */
		int getTaskCount();

		/**
		 * @param defaultTask The default task to show.
		 */
		void onShowDefaultTask(@NonNull S defaultTask);

		/**
		 * @param showTaskIndex The task index of the task which is going to be shown.
		 * @param showTask The task which is going to be shown.
		 * @param hideTaskIndex The task index of the task which is going to be hidden.
		 * @param hideTask The task which is going to be hidden.
		 */
		void onNext(int showTaskIndex, @NonNull S showTask, int hideTaskIndex, @NonNull S hideTask);

		/**
		 * @param showTaskIndex The task index of the task which is going to be shown.
		 * @param showTask The task which is going to be shown.
		 * @param hideTaskIndex The task index of the task which is going to be hidden.
		 * @param hideTask The task which is going to be hidden.
		 */
		void onPrevious(int showTaskIndex, @NonNull S showTask, int hideTaskIndex, @NonNull S hideTask);
	}

	/**
	 * A simple implementation of task handler which simply show and hide scenes without animation.
	 * @param <S>
	 */
	final public static class SimpleTaskHandler<S extends Scene> implements TaskHandler<S>
	{
		private SceneProvider<S> sceneProvider;

		public SimpleTaskHandler(SceneProvider<S> sceneProvider)
		{
			this.sceneProvider = sceneProvider;
		}

		@NonNull
		@Override
		public S getTask(int taskIndex)
		{
			return sceneProvider.getScene(taskIndex);
		}

		@Override
		public int getTaskCount()
		{
			return sceneProvider.getSceneCount();
		}

		@Override
		public void onShowDefaultTask(@NonNull S defaultTask)
		{
			sceneProvider.showScene(defaultTask);
		}

		@Override
		public void onNext(int showTaskIndex, @NonNull S showTask, int hideTaskIndex, @NonNull S hideTask)
		{
			sceneProvider.hideScene(hideTask);
			sceneProvider.showScene(showTask);
		}

		@Override
		public void onPrevious(int showTaskIndex, @NonNull S showTask, int hideTaskIndex, @NonNull S hideTask)
		{
			sceneProvider.hideScene(hideTask);
			sceneProvider.showScene(showTask);
		}
	}

	private TaskHandler<S> taskHandler;

	final public void showDefaultScene(@NonNull TaskHandler<S> taskHandler, int defaultTaskIndex)
	{
		if(this.taskHandler != null) throw new IllegalStateException("attempt to set task handler when it has already been set");
		this.taskHandler = taskHandler;

		final S defaultTask = taskHandler.getTask(defaultTaskIndex);
		defaultTask.onShow();
		taskHandler.onShowDefaultTask(defaultTask);

		pushDefaultScene(defaultTask);
	}

	/**
	 * Go to the next task. The current task will be hidden, then the next task will be shown. Nothing happens
	 * if the current task is the last task.
	 * <p>
	 * {@link TaskHandler#onNext(int, S, int, S)} will be used to hide and show the tasks.
	 * <p>
	 * {@link Scene#onHide()} and {@link Scene#onShow()} are called before hiding and showing the tasks.
	 * @return <code>true</code> when there is a next task, <code>false</code> otherwise. This can used to,
	 * for example, determine whether an activity should finish itself.
	 */
	public boolean next()
	{
		if(isLocked()) return true;

		if(getSceneStackCount() >= taskHandler.getTaskCount()) return false;

		final int incomingTaskIndex = getSceneStackCount();
		final int currentTaskIndex = incomingTaskIndex - 1;
		final S incomingTask = taskHandler.getTask(incomingTaskIndex);
		final S currentTask = getCurrentScene();

		currentTask.onHide();
		incomingTask.onShow();
		taskHandler.onNext(incomingTaskIndex, incomingTask, currentTaskIndex, currentTask);

		pushScene(incomingTask);
		return true;
	}

	/**
	 * Issue a back command. This method will return immediately when the back command is consumed.
	 *
	 * It will first delegate the back command to the current task. If the current task consumed the back
	 * command, so be it and nothing else happens. It the current task had not consumed the back command, this
	 * class tries to go back to a previous task. A previous task is the task the was hidden by a previous call
	 * to {@link #next()}, as record in an internal back stack. The back command is considered consumed when it
	 * does go back to a previous task, and not consumed when it does not. It does not go back to a previous
	 * task when the current task is the first task.
	 * <p>
	 * Whether or not the command is consumed is reflected in the return value. This can be used to, for example,
	 * determine whether an activity should finish itself.
	 * <p>
	 * {@link TaskHandler#onPrevious(int, S, int, S)} will be used to hide and show the tasks.
	 * <p>
	 * {@link Scene#onHide()} and {@link Scene#onShow()} are called before hiding and showing the tasks.
	 * @return <code>true</code> when the back command is consumed, <code>false</code> otherwise.
	 */
	public boolean previous()
	{
		if(isLocked()) return true;

		final S currentTask = getCurrentScene();
		if(currentTask.onBack()) return true;
		final int currentTaskIndex = getSceneStackCount() - 1;
		if(!popScene()) return false;

		final int previousTaskIndex = currentTaskIndex - 1;
		final S previousTask = getCurrentScene();
		currentTask.onHide();
		previousTask.onShow();
		taskHandler.onPrevious(previousTaskIndex, previousTask, currentTaskIndex, currentTask);
		return true;
	}
}
