package net.cafox.debug;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Process;

/**
 * An uncaught exception handler that start an activity with an intent which records the uncaught exception's
 * stack trace. The activity and the key to retrieve the stack trace in the intent is provided by the client.
 */
public class StartActivityHandler implements Thread.UncaughtExceptionHandler
{
	private Context context;
	private Class activityClass;
	private String stackTraceKey;

	/**
	 * Create a start activity handler. Client must supply a context to start the activity, the class of the
	 * activity to start, and a key that maps the stack trace string of the uncaught exception in the intent.
	 * @param context The context to the given activity.
	 * @param activityClass The class of the activity to start. No checking is performed on the validity of the
	 *                      class so supplying non-activity class causes undefined behavior.
	 * @param stackTraceKey The key used to map the stack trace inside the intent. This is the key that client
	 *                      will retrieve the stack trace in the supplied activity by calling
	 *                      <code>getIntent().getStringExtra(String)</code>.
	 * @see Intent#getStringExtra(String)
	 */
	public StartActivityHandler(Context context,  Class activityClass, String stackTraceKey)
	{
		this.context = context;
		this.activityClass = activityClass;
		this.stackTraceKey = stackTraceKey;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex)
	{
		String stackTrace = Log.getStackTraceString(ex);
		Log.e("", stackTrace);
		Intent intent = new Intent(context, activityClass);
		intent.putExtra(stackTraceKey, stackTrace);
		context.startActivity(intent);

		// Kill the current process. The activity will be started in a new process.
		Process.killProcess(Process.myPid());
		System.exit(12);
	}
}
