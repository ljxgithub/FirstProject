package com.ljx.github.firstproject.widget;

import android.view.View;

/**
 * Created by ljx on 2015/2/4.
 */
interface Page {
	public int getPageChildCount();
	public View getChildOnPageAt(int i);
	public void removeAllViewsOnPage();
	public void removeViewOnPageAt(int i);
	public int indexOfChildOnPage(View v);
}