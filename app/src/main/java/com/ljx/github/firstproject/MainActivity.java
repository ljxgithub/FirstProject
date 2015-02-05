package com.ljx.github.firstproject;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

/**
 * Created by ljx on 2015/2/2.
 */
public class MainActivity extends Activity {
	private ViewPager mViewPager;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mViewPager.setOffscreenPageLimit(3);
	}

	private class TabsAdapter extends FragmentPagerAdapter implements  ViewPager.OnPageChangeListener {
		Context sContext;
		ViewPager sViewPager;

		public TabsAdapter(Activity activity,ViewPager viewPager) {
			super(activity.getFragmentManager());
			sContext = activity;
			sViewPager = viewPager;
			sViewPager.setAdapter(this);
			sViewPager.setOnPageChangeListener(this);
		}

		@Override
		public Fragment getItem(int i) {
			return null;
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			//Do noting
		}

		@Override
		public void onPageSelected(int position) {
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			//Do noting
		}

		@Override
		public int getCount() {
			return 0;
		}
	}


	}
