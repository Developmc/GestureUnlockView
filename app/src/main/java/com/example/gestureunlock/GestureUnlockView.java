package com.example.gestureunlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

/**
 * <pre>
 *     author : Clement
 *     time   : 2018/05/08
 *     desc   : 手势解锁
 *     version: 1.0
 * </pre>
 */
public class GestureUnlockView extends View {

    public static final int CIRCLE_NORMAL = 1;//normal state of circle
    public static final int CIRCLE_SELECTED = 2;//selected state of circle
    public static final int CIRCLE_ERROR = 3;//error state of circle

    private UnlockMode mMode;//define the mode
    private int mWidth;//the width of screen,valued in onMeasure
    private int mHeight;//the height of screen,valued in onMeasure

    private int mLastCircleX;//上一次circle中心点的X
    private int mLastCircleY;//上一次circle中心点的Y

    private ArrayList<Circle> mAllCircleList = new ArrayList<>();//store the circles on screen
    private ArrayList<Circle> mSelectedCircleList = new ArrayList<>();//store the selected circles

    private Paint mNormalPaint;//paint of normal state circles
    private Paint mSelectedPaint;//paint of selected state circles
    private Paint mSmallSelectedPaint;//paint of selected state small circles
    private Paint mErrorPaint;//paint of error state circles
    private Paint mSmallErrorPaint;//paint of error state small circles
    private Paint mPathPaint;//paint of the lines

    private Path mPath;
    private Path mTempPath;
    private int mPathWidth = 3;//width of the paint of path
    private int mNormalR = 16;//radius of small circles;
    private int mBigR = 30;//radius of big circles;
    private int mBigCircleStrokeWidth = 2;//width of big circles;

    private int mNormalColor = Color.parseColor("#D5DBE8");//default color of normal state
    private int mSelectedColor = Color.parseColor("#508CEE");//default color of selected state
    private int mErrorColor = Color.parseColor("#FF3153");//default color of error state

    //手指抬起后，会继续显示ui，持续一段时间
    private boolean isActionUpProcessing = false;
    //是否已经初始化circles
    private boolean mHasInitCircles;
    private GestureListener mGestureListener;//the listener of gesture

    public GestureUnlockView(Context context) {
        this(context, null);
    }

    public GestureUnlockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureUnlockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        mBigCircleStrokeWidth = dip2px(context, mBigCircleStrokeWidth);
        mNormalR = dip2px(context, mNormalR);
        mBigR = dip2px(context, mBigR);
        mPathWidth = dip2px(context, mPathWidth);
        //初始化画笔
        mPathPaint = new Paint();
        mPathPaint.setColor(mSelectedColor);
        mPathPaint.setDither(true);
        mPathPaint.setAntiAlias(true);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        mPathPaint.setStrokeWidth(mPathWidth);
        //普通状态小圆画笔
        mNormalPaint = new Paint();
        mNormalPaint.setAntiAlias(true);
        mNormalPaint.setDither(true);
        mNormalPaint.setColor(mNormalColor);
        //选中状态大圆画笔
        mSelectedPaint = new Paint();
        mSelectedPaint.setAntiAlias(true);
        mSelectedPaint.setDither(true);
        mSelectedPaint.setStyle(Paint.Style.STROKE);
        mSelectedPaint.setStrokeWidth(mBigCircleStrokeWidth);
        mSelectedPaint.setColor(mSelectedColor);
        //选中状态小圆画笔
        mSmallSelectedPaint = new Paint();
        mSmallSelectedPaint.setAntiAlias(true);
        mSmallSelectedPaint.setDither(true);
        mSmallSelectedPaint.setColor(mSelectedColor);
        //出错状态大圆画笔
        mErrorPaint = new Paint();
        mErrorPaint.setAntiAlias(true);
        mErrorPaint.setDither(true);
        mErrorPaint.setStyle(Paint.Style.STROKE);
        mErrorPaint.setStrokeWidth(mBigCircleStrokeWidth);
        mErrorPaint.setColor(mErrorColor);
        //出错状态小圆画笔
        mSmallErrorPaint = new Paint();
        mSmallErrorPaint.setAntiAlias(true);
        mSmallErrorPaint.setDither(true);
        mSmallErrorPaint.setColor(mErrorColor);

        //初始化path
        mPath = new Path();
        mTempPath = new Path();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        //宽高相等，取较小值
        if (mWidth > mHeight) {
            mWidth = mHeight;
        } else {
            mHeight = mWidth;
        }
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //onLayout会多次回调
        initCircles();
    }

    @Override protected void onDraw(Canvas canvas) {
        for (int i = 0; i < mAllCircleList.size(); i++) {
            drawCircles(mAllCircleList.get(i), canvas);
        }
        canvas.drawPath(mPath, mPathPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        //如果还在显示上一次绘制的界面，则忽略
        if (isActionUpProcessing) {
            return true;
        }
        int curX = (int) event.getX();
        int curY = (int) event.getY();
        Circle circle = getTouchCircle(curX, curY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //重置数据
                this.resetAll();
                if (circle != null) {
                    addSelectedCircle(circle);
                    mTempPath.moveTo(mLastCircleX, mLastCircleY);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mSelectedCircleList.isEmpty()) {
                    mPath.reset();
                    mPath.addPath(mTempPath);
                    mPath.moveTo(mLastCircleX, mLastCircleY);
                    mPath.lineTo(curX, curY);
                    if (circle != null && !(circle.getState() == CIRCLE_SELECTED)) {
                        addSelectedCircle(circle);
                        mTempPath.lineTo(mLastCircleX, mLastCircleY);
                    }
                } else {
                    //如果还没有选中circle
                    if (circle != null) {
                        addSelectedCircle(circle);
                        mTempPath.moveTo(mLastCircleX, mLastCircleY);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!mSelectedCircleList.isEmpty()) {
                    mPath.reset();
                    mPath.addPath(mTempPath);
                    StringBuilder sb = new StringBuilder();
                    for (Circle tempCircle : mSelectedCircleList) {
                        sb.append(tempCircle.getPosition());
                    }

                    //解锁图案是否正确由外部决定
                    if (mGestureListener != null) {
                        boolean isSuccess =
                            mGestureListener.onGestureFinish(mMode, sb.toString());
                        //如果验证不成功
                        if (!isSuccess) {
                            mPathPaint.setColor(mErrorColor);
                            for (Circle circle1 : mSelectedCircleList) {
                                circle1.setState(CIRCLE_ERROR);
                            }
                        }
                    }

                    //松手后，延迟一秒清空画面
                    this.postDelayed(new Runnable() {
                        @Override public void run() {
                            resetAll();
                            invalidate();
                        }
                    }, 1000);
                    isActionUpProcessing = true;
                }
                break;
        }
        invalidate();
        return true;
    }

    /**
     * 初始化circles
     */
    private void initCircles() {
        int hor = mWidth / 6;
        int ver = mHeight / 6;
        if (!mHasInitCircles) {
            for (int i = 0; i < 9; i++) {
                int tempX = (i % 3 + 1) * 2 * hor - hor;
                int tempY = (i / 3 + 1) * 2 * ver - ver;
                Circle circle = new Circle(i + 1, tempX, tempY, CIRCLE_NORMAL);
                mAllCircleList.add(circle);
            }
        }
        mHasInitCircles = true;
    }

    /**
     * reset all states
     */
    private void resetAll() {
        isActionUpProcessing = false;
        mPath.reset();
        mTempPath.reset();
        mSelectedCircleList.clear();
        for (Circle circle : mAllCircleList) {
            circle.setState(CIRCLE_NORMAL);
        }
        mPathPaint.setColor(mSelectedColor);
        mSelectedPaint.setColor(mSelectedColor);
        mSmallSelectedPaint.setColor(mSelectedColor);
    }

    /**
     * called in onDraw for drawing all circles
     */
    private void drawCircles(Circle circle, Canvas canvas) {
        switch (circle.getState()) {
            case CIRCLE_NORMAL:
                canvas.drawCircle(circle.getX(), circle.getY(), mNormalR, mNormalPaint);
                break;
            case CIRCLE_SELECTED:
                canvas.drawCircle(circle.getX(), circle.getY(), mBigR, mSelectedPaint);
                canvas.drawCircle(circle.getX(), circle.getY(), mNormalR, mSmallSelectedPaint);
                break;
            case CIRCLE_ERROR:
                canvas.drawCircle(circle.getX(), circle.getY(), mBigR, mErrorPaint);
                canvas.drawCircle(circle.getX(), circle.getY(), mNormalR, mSmallErrorPaint);
                break;
        }
    }

    /**
     * @param context Context
     * @param dipValue value of dp
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 检查是否触碰到circles,如果是则返回该circle，否则返回null
     */
    @Nullable private Circle getTouchCircle(int x, int y) {
        for (int i = 0; i < mAllCircleList.size(); i++) {
            Circle circle = mAllCircleList.get(i);
            //为增大触摸范围，这里的半径以大圈的半径作为标准
            if ((x - circle.getX()) * (x - circle.getX()) + (y - circle.getY()) * (y
                - circle.getY()) <= mBigR * mBigR) {
                if (circle.getState() != CIRCLE_SELECTED) {
                    return circle;
                }
            }
        }
        return null;
    }

    /**
     * 添加已选中的circle
     */
    private void addSelectedCircle(Circle circle) {
        //检查是否已添加
        boolean hasAdd = false;
        for (Circle tempCircle : mSelectedCircleList) {
            if (tempCircle.getPosition() == circle.getPosition()) {
                hasAdd = true;
            }
        }
        //如果还没有添加，则添加进去
        if (!hasAdd) {
            //更改状态为已选中
            circle.setState(CIRCLE_SELECTED);
            //记录上一次的circle的中心X,Y
            mLastCircleX = circle.getX();
            mLastCircleY = circle.getY();
            mSelectedCircleList.add(circle);
        }
    }

    public void setMode(UnlockMode mode) {
        this.mMode = mode;
    }

    /**
     * Gesture Listener
     */
    interface GestureListener {
        boolean onGestureFinish(UnlockMode mode, String result);
    }

    public void setGestureListener(GestureListener listener) {
        this.mGestureListener = listener;
    }
}
