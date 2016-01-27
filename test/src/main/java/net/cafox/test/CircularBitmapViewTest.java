package net.cafox.test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import net.cafox.widget.CircularBitmapView;

/**
 *
 */
public class CircularBitmapViewTest extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_circular_bitmap_view_test);
		CircularBitmapView downScale = (CircularBitmapView) findViewById(R.id.down_scale);
		CircularBitmapView upScale = (CircularBitmapView) findViewById(R.id.up_scale);

		Bitmap bigBitmap = ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_height_larger)).getBitmap();
		Bitmap smallBitmap = ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();

		downScale.setImageBitmap(bigBitmap);
		upScale.setImageBitmap(smallBitmap);
	}
}
