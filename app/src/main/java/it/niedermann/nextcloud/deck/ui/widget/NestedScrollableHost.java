package it.niedermann.nextcloud.deck.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;

/**
 * Layout to wrap a scrollable component inside a ViewPager2. Provided as a solution to the problem
 * where pages of ViewPager2 have nested scrollable elements that scroll in the same direction as
 * ViewPager2. The scrollable element needs to be the immediate and only child of this host layout.
 * <p>
 * This solution has limitations when using multiple levels of nested scrollable elements
 * (e.g. a horizontal RecyclerView in a vertical RecyclerView in a horizontal ViewPager2).
 */
public class NestedScrollableHost extends FrameLayout {

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private int touchSlop = 0;
    private float initialX = 0f;
    private float initialY = 0f;

    public ViewPager2 getParentViewPager() {
        View v = (View) getParent();
        while (v != null && !(v instanceof ViewPager2)) {
            v = (View) v.getParent();
        }
        return (ViewPager2) v;
    }

    public View getChild() {
        if (getChildCount() > 0) {
            return getChildAt(0);
        } else {
            return null;
        }
    }

    private boolean canChildScroll(int orientation, float delta) {
        int direction = (int) delta; // -delta.sign.toInt();
        if (orientation == 0) {
            return getChild().canScrollHorizontally(direction);
        } else if (orientation == 1) {
            return getChild().canScrollVertically(direction);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        handleInterceptTouchEvent(e);
        return super.onInterceptTouchEvent(e);
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        int orientation = getParentViewPager().getOrientation();

        // Early return if child can't scroll in same direction as parent
        if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f)) {
            return;
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = e.getX();
            initialY = e.getY();
            requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - initialX;
            float dy = e.getY() - initialY;
            boolean isVpHorizontal = orientation == ORIENTATION_HORIZONTAL;

            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            float scaledDx = dx * (isVpHorizontal ? .5f : 1f); // TODO dx absolute value
            float scaledDy = dy * (isVpHorizontal ? 1f : .5f); // TODO dy absolute value

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is perpendicular, allow all parents to intercept
                    requestDisallowInterceptTouchEvent(false);
                } else {
                    // Gesture is parallel, query child if movement in that direction is possible
                    if (canChildScroll(orientation, (isVpHorizontal) ? dx : dy)) {
                        // Child can scroll, disallow all parents to intercept
                        requestDisallowInterceptTouchEvent(true);
                    } else {
                        // Child cannot scroll, allow all parents to intercept
                        requestDisallowInterceptTouchEvent(false);
                    }
                }
            }
        }
    }
}