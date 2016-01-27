package net.cafox.navigation;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * An interface that provides necessary information for various scene handler to work. Developers should sub-class
 * this class to provide other scene-specific information and declare the sub-class when constructing various
 * scene navigator and scene handler.
 */
public interface Scene
{
	@NonNull View getView();

	int getSceneId();

	void onHide();

	void onShow();

	boolean onBack();

	void onSetArgument(Object argument);
}