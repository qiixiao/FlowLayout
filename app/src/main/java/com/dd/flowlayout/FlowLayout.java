package com.dd.flowlayout;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式布局
 */
public class FlowLayout extends ViewGroup {
    private final int mHorizontalSpacing = dp2px(16);
    private final int mVerticalSpacing = dp2px(8);

    /**
     * 防止多次测量
     */
    private boolean isMeasure = false;
    // 分行存储View
    private List<List<View>> allLines = new ArrayList<>();
    // 保存每一行高
    private List<Integer> lineHeights = new ArrayList<>();

    /**
     * 代码构建-> new FlowLayout(context)
     *
     * @param context
     */
    public FlowLayout(Context context) {
        super(context);
    }

    /**
     * xml中使用
     *
     * @param context
     * @param attrs
     */
    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 同上，使用自定义Style时要重写
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 度量所有的子View
        int childCount = getChildCount();

        // 父View的padding
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        // ViewGroup即系的宽高
        int selfWidth = MeasureSpec.getSize(widthMeasureSpec);
        int selfHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 保存一行中的所有View
        List<View> lineViews = new ArrayList();
        int lineWidthUsed = paddingLeft + paddingRight; // 记录每行已经使用的宽的大小
        int lineMaxHeight = 0; // 行高

        // 过程中，子View要求父ViewGroup的宽高
        int parentNeededWidth = 0;
        int parentNeededHeight = 0;

        if (!isMeasure) {
            isMeasure = true;
        } else {
            for (int i = 0; i < childCount; i++) {
                View childView = getChildAt(i);
                measureChildWithMargins(childView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                MarginLayoutParams childLP = (MarginLayoutParams) childView.getLayoutParams();

                // 获取子View的宽高
                int childWidth = childView.getMeasuredWidth() + childLP.leftMargin + childLP.rightMargin;
                int childHeight = childView.getMeasuredHeight() + childLP.topMargin + childLP.bottomMargin;

                // 需要换行了
                if (lineWidthUsed + childWidth + mHorizontalSpacing > selfWidth - paddingRight) {
                    // 一旦换行，更新需要的宽高
                    // 高度-累加；宽度-取最大值
                    parentNeededWidth = Math.max(parentNeededWidth, lineWidthUsed - mHorizontalSpacing);
                    parentNeededHeight += lineMaxHeight + mVerticalSpacing;

                    // 保存上一行的数据
                    allLines.add(lineViews);
                    lineHeights.add(lineMaxHeight);

                    // 置空，下一行开始
                    lineViews = new ArrayList<>();
                    lineWidthUsed = childWidth;
                    lineMaxHeight = childHeight;
                    lineViews.add(childView);
                } else { // 不涉及换行
                    //每行都会有自己的宽和高
                    lineWidthUsed += childWidth + mHorizontalSpacing;
                    lineMaxHeight = Math.max(lineMaxHeight, childHeight);

                    // View是分行Layout，按行保存，方便layout布局
                    lineViews.add(childView);
                }
                // 最后一行
                if (i == childCount - 1) {
                    allLines.add(lineViews);
                    lineHeights.add(lineMaxHeight);

                    parentNeededHeight += lineMaxHeight;
                    parentNeededWidth = Math.max(parentNeededWidth, lineWidthUsed);
                }
            }
        }

        // 度量和保存自己的size
        // 计算模式（通过爷爷ViewGroup提供的数据进行计算）
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        parentNeededWidth += paddingLeft + paddingRight;
        parentNeededHeight += paddingTop + paddingBottom;
        // 这里不考虑UNSPECIFIED的场景，整体由系统决定
        int realWidth = (widthMode == MeasureSpec.EXACTLY) ? selfWidth : parentNeededWidth;
        int realHeight = (heightMode == MeasureSpec.EXACTLY) ? selfHeight : parentNeededHeight;
        setMeasuredDimension(realWidth, realHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int lineCount = allLines.size();
        // 从父ViewGroup的padding内部开始布局
        int curLeft = getPaddingLeft();
        int curTop = getPaddingTop();

        for (int i = 0; i < lineCount; i++) {
            List<View> lineVies = allLines.get(i);
            for (int j = 0; j < lineVies.size(); j++) {
                View view = lineVies.get(j);
                MarginLayoutParams layoutParams = (MarginLayoutParams) view.getLayoutParams();

                // 每一行的第一个的左侧坐标是 == paddingLeft
                int left = curLeft + layoutParams.leftMargin;
                int top = curTop + layoutParams.topMargin;

                int right = left + view.getMeasuredWidth(); // 不能使用getWidth哦
                int bottom = top + view.getMeasuredHeight();
                view.layout(left, top, right, bottom);

                curLeft += view.getMeasuredWidth() + mHorizontalSpacing + layoutParams.leftMargin + layoutParams.rightMargin;
            }
            // Top 增加
            curTop += lineHeights.get(i) + mVerticalSpacing;
            // 重置Left
            curLeft = getPaddingLeft();
        }

        allLines.clear();
        lineHeights.clear();
    }

    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }
}
