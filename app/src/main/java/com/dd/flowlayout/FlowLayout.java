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
    private final int mVerticalSpacing = dp2px(16);

    // 分行存储View
    private List<List<View>> allLines;
    // 保存每一行高
    private List<Integer> lineHeights;

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initMeasureParam() {
        if (allLines == null) {
            allLines = new ArrayList<>();
        } else {
            allLines.clear();
        }
        if (lineHeights == null) {
            lineHeights = new ArrayList<>();
        } else {
            lineHeights.clear();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 根据生命周期，这里要在这里初始化
        initMeasureParam();

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
        int lineWidthUsed = 0; // 记录每行已经使用的宽的大小
        int lineHeight = 0; // 行高

        // 过程中，子View要求父ViewGroup的宽高
        int parentNeededWidth = 0;
        int parentNeededHeight = 0;

        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            LayoutParams childLP = childView.getLayoutParams();
            // layoutParams-> MeasureSpec（注意参数：父View的MeasureSpec+Padding，自身大小）
            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    paddingLeft + paddingRight, childLP.width);
            int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    paddingTop + paddingBottom, childLP.height);
            // 子View度量
            childView.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            // 获取子View的宽高
            int childMeasuredWidth = childView.getMeasuredWidth();
            int childMeasuredHeight = childView.getMeasuredHeight();

            // 需要换行了
            if (lineWidthUsed + childMeasuredWidth + mHorizontalSpacing > selfWidth) {
                allLines.add(lineViews);
                lineHeights.add(lineHeight);

                // 一旦换行，更新需要的宽高
                parentNeededHeight = parentNeededHeight + lineHeight + mVerticalSpacing;
                parentNeededWidth = Math.max(parentNeededWidth, lineWidthUsed + mHorizontalSpacing);

                // 置空，下一行开始
                lineViews = new ArrayList<>();
                lineWidthUsed = 0;
                lineHeight = 0;
            }

            // View是分行Layout，按行保存，方便layout布局
            lineViews.add(childView);

            //每行都会有自己的宽和高
            lineWidthUsed = lineWidthUsed + childMeasuredWidth + mHorizontalSpacing;
            lineHeight = Math.max(lineHeight, childMeasuredHeight);

            // 最后一行
            if (i == childCount - 1) {
                allLines.add(lineViews);
                lineHeights.add(lineHeight);
                parentNeededHeight = parentNeededHeight + lineHeight;
                parentNeededWidth = Math.max(parentNeededWidth, lineWidthUsed + mHorizontalSpacing);
            }
        }

        // 度量和保存自己的size
        // 计算模式（通过爷爷ViewGroup提供的数据进行计算）
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        Log.w("TAG", "MeasureSpec = " + widthMode + ", " + heightMode);

        // 这里不考虑UNSPECIFIED的场景，整体由系统决定
        int realWidth = (widthMode == MeasureSpec.EXACTLY) ? selfWidth : parentNeededWidth;
        int realHeight = (heightMode == MeasureSpec.EXACTLY) ? selfHeight : parentNeededHeight;
        Log.w("TAG", "final size=" + realWidth + ", " + realHeight + ", parentCalc=" + parentNeededWidth + ", " + parentNeededHeight);
        setMeasuredDimension(realWidth, realHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int lineCount = allLines.size();
        // 从父ViewGroup的padding内部开始布局
        int curL = getPaddingLeft();
        int curT = getPaddingTop();

        for (int i = 0; i < lineCount; i++) {
            // 获取这一行的高度
            int lineHeight = lineHeights.get(i);

            List<View> lineVies = allLines.get(i);
            for (int j = 0; j < lineVies.size(); j++) {
                View view = lineVies.get(j);
                int left = curL; // 每一行的第一个的左侧坐标是 == paddingLeft
                int top = curT;

                int right = left + view.getMeasuredWidth(); // 不能使用getWidth哦
                int bottom = top + view.getMeasuredHeight();
                view.layout(left, top, right, bottom);

                curL = right + mHorizontalSpacing;
            }
            // Top 增加
            curT = curT + lineHeight + mVerticalSpacing;
            // 重置Left
            curL = getPaddingLeft();// 每一行的curL要重新清零
        }
    }

    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }
}
