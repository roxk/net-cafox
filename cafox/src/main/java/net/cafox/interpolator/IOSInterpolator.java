package net.cafox.interpolator;

import android.view.animation.Interpolator;

public class IOSInterpolator implements Interpolator
{
	@Override
	public float getInterpolation(float i)
	{
		i = 1.0f - i;
		return 1.0f - (float) Math.pow(i, (float) Math.pow(12, i) + 1.0f);
	}
}
