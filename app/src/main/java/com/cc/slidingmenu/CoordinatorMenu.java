package com.cc.slidingmenu;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import timber.log.Timber;

/**
 * Created by chasonchen on 2017/2/6.
 */

public class CoordinatorMenu extends FrameLayout {

    private final static String TAG = "CoordinatorMenu";

    private final static float TOUCH_SLOP_SENSITIVITY = 1.0f;
    private final static int TRIGGER_DIS = 50;
    private final static int LEFT_TO_RIGHT = 100;
    private final static int RIGHT_TO_LEFT = 101;

    private static final int MENU_CLOSED = 1;
    private static final int MENU_OPENED = 2;
    private int mMenuState = MENU_CLOSED;

    private View mMenu;
    private View mContent;

    private int mMenuWidth;
    private int mDragDirection;
    private int mScreenWidth;
    private int mScreenHeight;

    private int mMenuOffset = 300;
    private String mShadowOpacity = "00";

    private ViewDragHelper mViewDragHelper;

    public CoordinatorMenu(Context context) {
        super(context);
        init(context);
    }

    public CoordinatorMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CoordinatorMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        mScreenWidth = point.x;
        mScreenHeight = point.y;

        mViewDragHelper = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return mContent == child || mMenu == child;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                if (left < 0) {
                    left = 0;
                } else if (left > mMenuWidth) {
                    left = mMenuWidth;
                }
                return left;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                if (dx > 0) {
                    mDragDirection = LEFT_TO_RIGHT;
                } else {
                    mDragDirection = RIGHT_TO_LEFT;
                }

                int hex = Math.round(((float) left / (float) mScreenWidth) * 255);
                mShadowOpacity = hex < 16 ? "0" + Integer.toHexString(hex) : Integer.toHexString(hex);

                float scale = (float) (mMenuWidth - mMenuOffset) / (float) mMenuWidth;
                int menuLeft = (int) (left - (scale * left + mMenuOffset));
                mMenu.layout(menuLeft, mMenu.getTop(), menuLeft + mMenuWidth, mMenu.getBottom());
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                if (mDragDirection == LEFT_TO_RIGHT) {
                    if (mContent.getLeft() < TRIGGER_DIS) {
                        closeMenu();
                    } else {
                        openMenu();
                    }
                } else if (mDragDirection == RIGHT_TO_LEFT) {
                    if (mContent.getLeft() < mMenuWidth - TRIGGER_DIS) {
                        closeMenu();
                    } else {
                        openMenu();
                    }
                }
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                if (capturedChild == mMenu) {
                    mViewDragHelper.captureChildView(mContent, activePointerId);
                }
            }
        });
    }

    private void openMenu() {
        mViewDragHelper.smoothSlideViewTo(mContent, mMenuWidth, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void closeMenu() {
        mViewDragHelper.smoothSlideViewTo(mContent, 0, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int restoreCount = canvas.save();//保存画布当前的剪裁信息
        final int height = getHeight();
        final int clipLeft = 0;
        int clipRight = mContent.getLeft();
        if (child == mMenu) {
            canvas.clipRect(clipLeft, 0, clipRight, height);//剪裁显示的区域
        }
        boolean result = super.drawChild(canvas, child, drawingTime);//绘制当前view
        //恢复画布之前保存的剪裁信息
        //以正常绘制之后的view
        canvas.restoreToCount(restoreCount);

        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#"+mShadowOpacity+"777777"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(mContent.getLeft(),0,mScreenWidth,mScreenHeight,paint);
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        MarginLayoutParams menuParams = (MarginLayoutParams) mMenu.getLayoutParams();
        menuParams.width = mMenuWidth;
        mMenu.setLayoutParams(menuParams);
        if (mMenuState == MENU_OPENED) {
            mMenu.layout(0, 0, mMenuWidth, bottom);
            mContent.layout(mMenuWidth, 0, mMenuWidth + mScreenWidth, bottom);
            return;
        }
        mMenu.layout(-mMenuOffset, top, mMenuWidth - mMenuOffset, bottom);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mMenu = getChildAt(0);
        mContent = getChildAt(1);

        mMenuWidth = mMenu.getLayoutParams().width;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //将触摸事件传递给ViewDragHelper，此操作必不可少
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);//处理刷新，实现平滑移动
        }

        if (mContent.getLeft() == 0) {
            mMenuState = MENU_CLOSED;
        } else if (mContent.getLeft() == mMenuWidth) {
            mMenuState = MENU_OPENED;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
//        Timber.d(TAG+"-onSaveInstanceState");
        final Parcelable superState = super.onSaveInstanceState();
        final CoordinatorMenu.SavedState ss = new CoordinatorMenu.SavedState(superState);
        ss.menuState = mMenuState;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
//        Timber.d(TAG+"-onRestoreInstanceState");
        if (!(state instanceof CoordinatorMenu.SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final CoordinatorMenu.SavedState ss = (CoordinatorMenu.SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.menuState == MENU_OPENED) {
            openMenu();
        }
    }

    protected static class SavedState extends AbsSavedState {
        int menuState;

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            Timber.d(TAG+"-SavedState(Parcel in, ClassLoader loader)");
            menuState = in.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
            Timber.d(TAG+"-SavedState(Parcel superState)");
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            Timber.d(TAG+"-writeToParcel(Parcel dest, int flags) ");
            dest.writeInt(menuState);
        }

        public static final Creator<CoordinatorMenu.SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public CoordinatorMenu.SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        Timber.d(TAG+"-Creator：createFromParcel(Parcel in, ClassLoader loader)");
                        return new CoordinatorMenu.SavedState(in, loader);
                    }

                    @Override
                    public CoordinatorMenu.SavedState[] newArray(int size) {
                        Timber.d(TAG+"-Creator：newArray(int size)");
                        return new CoordinatorMenu.SavedState[size];
                    }
                });
    }
}
