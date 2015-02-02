package com.ljx.github.firstproject;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Created by ljx on 2015/2/2.
 */
public class PagedView extends ViewGroup implements ViewGroup.OnHierarchyChangeListener {

	private static final int FLING_THRESHOLD_VELOCITY = 500;
	private static final int MIN_SNAP_VELOCITY = 1500;
	private static final int MIN_FLING_VELOCITY = 250;
	private static final int INVALID_PAGE = -1;
	//内间距
	private int mPageLayoutPaddingTop;
	private int mPageLayoutPaddingBottom;
	private int mPageLayoutPaddingLeft;
	private int mPageLayoutPaddingRight;
	//TODO:间隙？
	private int mPageLayoutWidthGap;
	private int mPageLayoutHeightGap;

	private Scroller mScroller;
	private int mCurrentPage = 0;
	private boolean mCenterPagesVertically = true;

	//是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件
	private int mTouchSlop;
	//TODO:滑动的时候，手的移动要大于这个距离才开始移动页面？
	private int mPagingTouchSlop;
	//最大的速度
	private int mMaximumVelocity;
	private float mDensity;

	private ArrayList<Boolean> mDirtyPageContent;
	private int mFlingThresholdVelocity;
	private int mMinFlingVelocity;
	private int mMinSnapVelocity;
	private int mPageSpacing;
	private int[] mChildOffsets;
	private int[] mChildRelativeOffsets;
	private int[] mChildOffsetsWithLayoutScale;
	private boolean mForceScreenScrolled;
	private PageSwitchListener mPageSwitchListener;
	private boolean mIsDataIsReady;
	private int mNextPage;
	private float mLayoutScale;
	private boolean mScrollingPaused;
	private boolean misPageMoving;


	public interface PageSwitchListener {
		void onPageSwitch(View newPage, int newPageIndex);
	}

	public PagedView(Context context) {
		this(context, null);
	}

	public PagedView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PagedView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedView, defStyleAttr, 0);
		setPageSpacing(a.getDimensionPixelSize(R.styleable.PagedView_pageSpacing, 0));
		mPageLayoutPaddingTop = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingTop, 0);
		mPageLayoutPaddingBottom = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingBottom, 0);
		mPageLayoutPaddingLeft = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingLeft, 0);
		mPageLayoutPaddingRight = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingRight, 0);
		mPageLayoutWidthGap = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutWidthGap, 0);
		mPageLayoutHeightGap = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutHeightGap, 0);
		a.recycle();

		//设置是否在长按后需要触感反馈(震动)
		setHapticFeedbackEnabled(false);
		init();
	}

	private void init() {
		//TODO:页面的坏数据？
		mDirtyPageContent = new ArrayList<Boolean>();
		mDirtyPageContent.ensureCapacity(32);
		//滚动器
		mScroller = new Scroller(getContext(), new ScrollInterpolator());
		mCurrentPage = 0;
		mCenterPagesVertically = true;

		final ViewConfiguration configuration = ViewConfiguration.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		mDensity = getResources().getDisplayMetrics().density;

		mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
		mMinFlingVelocity = (int) (MIN_FLING_VELOCITY * mDensity);
		mMinSnapVelocity = (int) (MIN_SNAP_VELOCITY * mDensity);

		//用于监听ViewGroup的层次变化
		setOnHierarchyChangeListener(this);

	}

	public void setPageSwitchListener(PageSwitchListener pageSwitchListener) {
		mPageSwitchListener = pageSwitchListener;
		if (mPageSwitchListener != null) {
			mPageSwitchListener.onPageSwitch(getPageAt(mCurrentPage), mCurrentPage);
		}
	}

	public boolean isLayoutRtl() {
		return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
	}

	protected void setDataisReady() {
		mIsDataIsReady = true;
	}

	protected boolean isDataReady() {
		return mIsDataIsReady;
	}

	int getCurrentPage() {
		return mCurrentPage;
	}

	int getNextPage() {
		return (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
	}

	int getPageCount() {
		return getChildCount();
	}

	View getPageAt(int index) {
		return getChildAt(index);
	}

	protected int indexToPage(int index) {
		return index;
	}

	protected void updateCurrentPageScroll() {
		int newX = 0;
		if (0 <= mCurrentPage && mCurrentPage < getPageCount()) {
			int offset = getChildOffset(mCurrentPage);
			int relOffset = getRelativeChildOffset(mCurrentPage);
			newX = offset = relOffset;
		}
		scrollTo(newX, 0);
		//设置mScroller最终停留的水平位置，没有动画效果，直接跳到目标位置
		mScroller.setFinalX(newX);
		//强行停止滚动
		mScroller.forceFinished(true);
	}

	void pauseScrolling() {
		mScroller.forceFinished(true);
		canclScrollingIndicatorAnimations();
		mScrollingPaused = true;
	}

	void resumeScrolling() {
		mScrollingPaused = false;
	}

	void setCurrentPage(int currentPage) {
		if (!mScroller.isFinished()) {
			//终止动画
			mScroller.abortAnimation();
		}

		if (getChildCount() == 0) {
			return;
		}

		mCurrentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
		updateCurrentPageScroll();
		updateScrollIndicator();
		notifyPageSwitchListener();
		invalidate();
	}

	private void notifyPageSwitchListener() {
		if(mPageSwitchListener != null){
			mPageSwitchListener.onPageSwitch(getPageAt(mCurrentPage),mCurrentPage);
		}
	}

	protected void pageBeginMoving(){
		if(!misPageMoving){

		}
	}

	private void updateScrollIndicator() {

	}

	private void canclScrollingIndicatorAnimations() {

	}

	protected int getChildOffset(int index) {
		final boolean isRtl = isLayoutRtl();
		int[] childOffsets = Float.compare(mLayoutScale, 1f) == 0 ? mChildOffsets : mChildOffsetsWithLayoutScale;

		if (childOffsets != null && childOffsets[index] != -1) {
			return childOffsets[index];
		} else {
			if (getChildCount() == 0) {
				return 0;
			}

			final int startIndex = isRtl ? getChildCount() - 1 : 0;
			final int endIndex = isRtl ? index : index;
			final int delta = isRtl ? -1 : 1;
			int offset = getRelativeChildOffset(startIndex);
			for (int i = startIndex; i != endIndex; i += delta) {
				offset += getScaledMeasureWidth(getPageAt(i)) + mPageSpacing;
			}
			if (childOffsets != null) {
				childOffsets[index] = offset;
			}
			return offset;
		}
	}

	private int getScaledMeasureWidth(View view) {
		return 0;
	}

	private int getRelativeChildOffset(int index) {
		return 0;
	}

	private void setPageSpacing(int pageSpacing) {
		mPageSpacing = pageSpacing;
		invalidateCachedOffsets();
	}

	private void invalidateCachedOffsets() {
		int count = getChildCount();
		if (count == 0) {
			mChildOffsets = null;
			mChildRelativeOffsets = null;
			mChildOffsetsWithLayoutScale = null;
			return;
		}

		mChildOffsets = new int[count];
		mChildRelativeOffsets = new int[count];
		mChildOffsetsWithLayoutScale = new int[count];

		for (int i = 0; i < count; i++) {
			mChildOffsets[i] = -1;
			mChildRelativeOffsets[i] = -1;
			mChildOffsetsWithLayoutScale[i] = -1;
		}
	}

	@Override
	public void onChildViewAdded(View parent, View child) {
		mForceScreenScrolled = true;
		invalidate();
		invalidateCachedOffsets();
	}

	@Override
	public void onChildViewRemoved(View parent, View child) {

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	}

	private class ScrollInterpolator implements Interpolator {
		public ScrollInterpolator() {

		}

		@Override
		public float getInterpolation(float input) {
			input -= 1.0f;
			return input * input * input * input * input + 1;
		}
	}
}
