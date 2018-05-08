package com.example.gestureunlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
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

    private int mRootX;//root position of the line which can move
    private int mRootY;//root position of the line which can move

    private ArrayList<Circle> mAllCircleList = new ArrayList<>();//store the circles on screen
    private ArrayList<Circle> mSelectedCircleList = new ArrayList<>();//store the selected circles

    private Paint cirNorPaint;//paint of normal state circles
    private Paint cirSelPaint;//paint of selected state circles
    private Paint smallCirSelPaint;//paint of selected state small circles
    private Paint cirErrPaint;//paint of error state circles
    private Paint smallcirErrPaint;//paint of error state small circles
    private Paint pathPaint;//paint of the lines
    private Path mPath;
    private Path tempPath;
    private int pathWidth = 3;//width of the paint of path
    private int normalR = 15;//radius of small circles;
    private int selectR = 30;//radius of big circles;
    private int strokeWidth = 2;//width of big circles;
    private int normalColor = Color.parseColor("#D5DBE8");//defalt color of normal state
    private int selectColor = Color.parseColor("#508CEE");//defalt color of selected state
    private int errorColor = Color.parseColor("#FF3153");//defalt color of error state

    private boolean isUnlocking;
    private boolean isShowError;
    private boolean hasNewCircles;
    private ArrayList<Integer> passList = new ArrayList<>();
    private OnUnlockListener listener;//the listener of unlock
    private CreateGestureListener createListener;//the listener of creating gesture

    /**
     * used for refresh the canvas after MotionEvent.ACTION_UP
     */
    private Handler handler = new Handler(new Handler.Callback() {
        @Override public boolean handleMessage(Message msg) {
            resetAll();
            invalidate();
            return true;
        }
    });

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
        strokeWidth = dip2px(context, strokeWidth);
        normalR = dip2px(context, normalR);
        selectR = dip2px(context, selectR);
        pathWidth = dip2px(context, pathWidth);
    }

    /**
     * reset all states
     */
    private void resetAll() {
        isShowError = false;
        isUnlocking = false;
        mPath.reset();
        tempPath.reset();
        mSelectedCircleList.clear();
        passList.clear();
        for (Circle circle : mAllCircleList) {
            circle.setState(CIRCLE_NORMAL);
        }
        pathPaint.setColor(selectColor);
        cirSelPaint.setColor(selectColor);
        smallCirSelPaint.setColor(selectColor);
        clearCanvas();
    }

    @Override protected void onDraw(Canvas canvas) {
        for (int i = 0; i < mAllCircleList.size(); i++) {
            drawCircles(mAllCircleList.get(i),canvas);
        }
        canvas.drawPath(mPath, pathPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (isShowError) return true;
        int curX = (int) event.getX();
        int curY = (int) event.getY();
        Circle circle = getOuterCircle(curX, curY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.resetAll();
                if (circle != null) {
                    mRootX = circle.getX();
                    mRootY = circle.getY();
                    circle.setState(CIRCLE_SELECTED);
                    mSelectedCircleList.add(circle);
                    tempPath.moveTo(mRootX, mRootY);
                    addItem(circle.getPosition() + 1);
                    isUnlocking = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isUnlocking) {
                    mPath.reset();
                    mPath.addPath(tempPath);
                    mPath.moveTo(mRootX, mRootY);
                    mPath.lineTo(curX, curY);
                    handleMove(circle);
                }
                break;
            case MotionEvent.ACTION_UP:
                isUnlocking = false;
                if (mSelectedCircleList.size() > 0) {
                    mPath.reset();
                    mPath.addPath(tempPath);
                    StringBuilder sb = new StringBuilder();
                    for (Integer num : passList) {
                        sb.append(num);
                    }

                    switch (mMode) {
                        case CREATE:
                            if (createListener != null) {
                                createListener.onGestureCreated(sb.toString());
                            }
                            break;
                        case CHECK:
                            if (listener != null) {
                                if (listener.isUnlockSuccess(sb.toString())) {
                                    listener.onSuccess();
                                } else {
                                    listener.onFailure();
                                    for (Circle circle1 : mSelectedCircleList) {
                                        circle1.setState(CIRCLE_ERROR);
                                    }
                                    pathPaint.setColor(errorColor);
                                }
                            }
                            break;
                    }

                    isShowError = true;
                    handler.postDelayed(new Runnable() {
                        @Override public void run() {
                            handler.sendEmptyMessage(0);
                        }
                    }, 1000);
                }
                break;
        }
        invalidate();
        return true;
    }

    private synchronized void handleMove(Circle c) {
        if (c != null && !(c.getState() == CIRCLE_SELECTED)) {
            c.setState(CIRCLE_SELECTED);
            mSelectedCircleList.add(c);
            mRootX = c.getX();
            mRootY = c.getY();
            tempPath.lineTo(mRootX, mRootY);
            addItem(c.getPosition() + 1);
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //init all path/paint
        mPath = new Path();
        tempPath = new Path();
        pathPaint = new Paint();
        pathPaint.setColor(selectColor);
        pathPaint.setDither(true);
        pathPaint.setAntiAlias(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeWidth(pathWidth);
        //普通状态小圆画笔
        cirNorPaint = new Paint();
        cirNorPaint.setAntiAlias(true);
        cirNorPaint.setDither(true);
        cirNorPaint.setColor(normalColor);
        //选中状态大圆画笔
        cirSelPaint = new Paint();
        cirSelPaint.setAntiAlias(true);
        cirSelPaint.setDither(true);
        cirSelPaint.setStyle(Paint.Style.STROKE);
        cirSelPaint.setStrokeWidth(strokeWidth);
        cirSelPaint.setColor(selectColor);
        //选中状态小圆画笔
        smallCirSelPaint = new Paint();
        smallCirSelPaint.setAntiAlias(true);
        smallCirSelPaint.setDither(true);
        smallCirSelPaint.setColor(selectColor);
        //出错状态大圆画笔
        cirErrPaint = new Paint();
        cirErrPaint.setAntiAlias(true);
        cirErrPaint.setDither(true);
        cirErrPaint.setStyle(Paint.Style.STROKE);
        cirErrPaint.setStrokeWidth(strokeWidth);
        cirErrPaint.setColor(errorColor);
        //出错状态小圆画笔
        smallcirErrPaint = new Paint();
        smallcirErrPaint.setAntiAlias(true);
        smallcirErrPaint.setDither(true);
        smallcirErrPaint.setColor(errorColor);

        //init all circles
        int hor = mWidth / 6;
        int ver = mHeight / 6;
        if (!hasNewCircles) {
            for (int i = 0; i < 9; i++) {
                int tempX = (i % 3 + 1) * 2 * hor - hor;
                int tempY = (i / 3 + 1) * 2 * ver - ver;
                Circle circle = new Circle(i, tempX, tempY, CIRCLE_NORMAL);
                mAllCircleList.add(circle);
            }
        }
        hasNewCircles = true;
    }

    /**
     * called in onDraw for drawing all circles
     */
    private void drawCircles(Circle circle,Canvas canvas) {
        switch (circle.getState()) {
            case CIRCLE_NORMAL:
                canvas.drawCircle(circle.getX(), circle.getY(), normalR, cirNorPaint);
                break;
            case CIRCLE_SELECTED:
                canvas.drawCircle(circle.getX(), circle.getY(), selectR, cirSelPaint);
                canvas.drawCircle(circle.getX(), circle.getY(), normalR, smallCirSelPaint);
                break;
            case CIRCLE_ERROR:
                canvas.drawCircle(circle.getX(), circle.getY(), selectR, cirErrPaint);
                canvas.drawCircle(circle.getX(), circle.getY(), normalR, smallcirErrPaint);
                break;
        }
    }

    /**
     * clear canvas
     */
    private void clearCanvas() {
        mPath.reset();
        tempPath.reset();
    }

    /**
     * J U S T  A  T O O L !
     *
     * @param context Context
     * @param dipValue value of dp
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * check whether the point is in a circle
     */
    @Nullable private Circle getOuterCircle(int x, int y) {
        for (int i = 0; i < mAllCircleList.size(); i++) {
            Circle circle = mAllCircleList.get(i);
            if ((x - circle.getX()) * (x - circle.getX()) + (y - circle.getY()) * (y
                - circle.getY()) <= normalR * normalR) {
                if (circle.getState() != CIRCLE_SELECTED) {
                    return circle;
                }
            }
        }
        return null;
    }

    /**
     * check whether the password list contains the number
     */
    private boolean arrContainsInt(int num) {
        for (Integer value : passList) {
            if (num == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * put the num into password list
     */
    private void addItem(Integer num) {
        if (!arrContainsInt(num)) {
            passList.add(num);
        }
    }

    /**
     * Create Mode Listener
     */
    interface CreateGestureListener {
        void onGestureCreated(String result);
    }

    public void setGestureListener(CreateGestureListener listener) {
        this.createListener = listener;
    }

    /**
     * Check Mode Listener
     */
    interface OnUnlockListener {
        boolean isUnlockSuccess(String result);

        void onSuccess();

        void onFailure();
    }

    public void setOnUnlockListener(OnUnlockListener listener) {
        this.listener = listener;
    }

    public void setPathWidth(int pathWidth) {
        this.pathWidth = pathWidth;
    }

    public void setNormalR(int normalR) {
        this.normalR = normalR;
    }

    public void setSelectR(int selectR) {
        this.selectR = selectR;
    }

    public void setNormalColor(int normalColor) {
        this.normalColor = normalColor;
    }

    public void setSelectColor(int selectColor) {
        this.selectColor = selectColor;
    }

    public void setErrorColor(int errorColor) {
        this.errorColor = errorColor;
    }

    public void setmMode(UnlockMode mMode) {
        this.mMode = mMode;
    }
}
