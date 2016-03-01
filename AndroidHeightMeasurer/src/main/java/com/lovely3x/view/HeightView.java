package com.lovely3x.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;

/**
 * 身高体重测量控件
 * 这个控件是为了项目中的身高体重选择而设计编码的
 * 不适合多数据情况，因为没有使用任何的回收机制
 * Created by lovely3x on 16-2-26.
 */
public class HeightView extends View {

    /**
     * 水平方向
     */
    public static final int HORIZONTAL = 1;
    /**
     * 垂直方向
     */
    public static final int VERTICAL = 2;
    public static final int ZERO = 0;
    /**
     * 默认的阻尼系数
     */
    public static final float DEFAULT_RATIO = 2.0f;
    private static final String TAG = "HeightView";
    /**
     * 阻尼系数，在fling下的阻力，阻力越大就飞的就约慢
     */
    private float ratio = DEFAULT_RATIO;


    /**
     * 需要绘制多少行
     */
    private int mLines = 240;
    /**
     * 突出行 就是长线 那一行
     */
    private int mOutSideLine = 10;
    /**
     * 每个outSideLine的步进值
     * 比如：
     * 步进值是 5 那么就是  0,5,10,15,20
     * 步进值是 10 那么就是 0,10,20,30,40
     */
    private int mSetupValue = 10;
    /**
     * 当前选中的行
     */
    private int mCurrentLine;
    /**
     * 每个格子的间距
     */
    private int space;
    /**
     * 用于普通绘制的画笔
     */
    private Paint mPaint;
    /**
     * 绘制文本的画笔
     */
    private Paint mTextPaint;
    /**
     * 短线的长度
     */
    private float mShortLineLength;
    /**
     * 长线的长度
     */
    private float mLongLineLength;
    /**
     * 高亮画笔，绘制选中线的
     */
    private Paint mHighlightPaint;

    /**
     * Marker画笔
     */
    private Paint mMarkerPaint;

    /**
     * scroller 用于滚动的辅助类
     */
    private OverScroller mOverScroller;
    /**
     * 手势探测器 用于探测手势
     */
    private GestureDetector mGestureDetector;
    /**
     * 高亮色选择中的颜色
     */
    private int mHighLightColor;
    /**
     * 文本颜色
     */
    private int mTextColor;
    /**
     * 文本大小
     */
    private float mTextSize;
    /**
     * 高亮的线的宽度
     */
    private float mHighlightWidth;
    /**
     * 普通的线的宽度
     */
    private float mLineWidth;

    /**
     * 线条的颜色
     */
    private int mLineColor;
    /**
     * 上一次是否是fling模式
     */
    private boolean mPreviousIsFling = false;
    /**
     * 开始行
     * 如果开始行为 0，步进值为10
     * 则 0,10,20,30,40
     * 如果开始行为 10，步进值为10
     * 则 100,110,120,130
     */
    private int mStartLine = 0;
    /**
     * 保存线位置的数组
     */
    private float[] mLinesArr = new float[4];

    /**
     * 重力方向
     */
    private int mOrientation = VERTICAL;

    /**
     * 指示器的宽度
     */
    private int mMarkerWidth = 45;

    /**
     * 指示器和长线的距离
     */
    private int mMarkerSpace = 20;

    /**
     * 背景色
     */
    private int mBackgroundColor = Color.parseColor("#03b7ee");

    /**
     * 标记物路径
     */
    private Path mMarkerPath;
    /**
     * Marker颜色
     */
    private int mMarkerColor;

    private OnItemChangedListener mOnItemChangedListener;


    public HeightView(Context context) {
        super(context);
        init();
    }

    public HeightView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.HeightView, defStyleAttr, 0);
            mOrientation = attributes.getInt(R.styleable.HeightView_orientation, VERTICAL);
            mBackgroundColor = attributes.getColor(R.styleable.HeightView_backgroundColor, mBackgroundColor);
            mTextColor = attributes.getColor(R.styleable.HeightView_textColor, mTextColor);
            mTextPaint.setColor(mTextColor);
            mTextSize = attributes.getDimension(R.styleable.HeightView_textSize, mTextSize);
            mTextPaint.setTextSize(mTextSize);

            mHighLightColor = attributes.getColor(R.styleable.HeightView_highlightColor, mHighLightColor);
            mHighlightPaint.setColor(mHighLightColor);
            mHighlightWidth = attributes.getDimension(R.styleable.HeightView_highlightLineWidth, mHighlightWidth);
            mHighlightPaint.setStrokeWidth(mHighlightWidth);

            mMarkerColor = attributes.getColor(R.styleable.HeightView_markerColor, mMarkerColor);
            mMarkerPaint.setColor(mMarkerColor);

            mLineColor = attributes.getColor(R.styleable.HeightView_lineColor, mLineColor);
            mPaint.setColor(mLineColor);

            mLineWidth = attributes.getDimension(R.styleable.HeightView_lineWidth, mLineWidth);
            mPaint.setStrokeWidth(mLineWidth);

            ratio = attributes.getFloat(R.styleable.HeightView_ratio, ratio);
            mMarkerSpace = attributes.getDimensionPixelOffset(R.styleable.HeightView_markerSpace, mMarkerSpace);

            mLines = attributes.getInt(R.styleable.HeightView_lines, mLines);
            mStartLine = attributes.getInt(R.styleable.HeightView_startLine, mStartLine);

            mMarkerWidth = attributes.getDimensionPixelOffset(R.styleable.HeightView_markerSize, mMarkerWidth);

            attributes.recycle();
        }
    }


    /**
     * 初始化所需条件
     */
    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        space = (int) (getResources().getDisplayMetrics().density * 7);
        mHighLightColor = Color.parseColor("#1e7d9e");
        mTextColor = mMarkerColor = Color.WHITE;
        mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        mShortLineLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        mLongLineLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        mHighlightWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, getResources().getDisplayMetrics());
        mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());


        mMarkerPath = new Path();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mLineColor = Color.WHITE);
        mPaint.setStrokeWidth(mLineWidth);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setColor(mHighLightColor);
        mHighlightPaint.setStrokeWidth(mHighlightWidth);

        mMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setStyle(Paint.Style.FILL);
        mHighlightPaint.setColor(mMarkerColor);


        mOverScroller = new OverScroller(getContext());

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                switch (mOrientation) {
                    case VERTICAL: {
                        //往上滚动
                        if (distanceY > 0) {
                            //最大滚动到Marker位置
                            if (getScrollY() + distanceY > (getHeight() >> 1) - getPaddingBottom() - mStartLine * space) {
                                scrollTo(0, (getHeight() >> 1) - getPaddingBottom() - mStartLine * space);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            } else if (getScrollY() + distanceY < (getHeight() >> 1) - getPaddingBottom() - mStartLine * space) {
                                scrollBy(0, (int) distanceY);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            }
                            //往下滚动
                        } else if (distanceY < 0) {
                            int minDistance = ((mLines/* - mStartLine*/) * space - (getHeight() >> 1)) + getPaddingBottom()/* + mStartLine * space*/;
                            if (getScrollY() < -minDistance) {
                                scrollTo(0, -minDistance);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            } else if (getScrollY() > -minDistance) {
                                scrollBy(0, (int) distanceY);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            }
                        }
                    }
                    break;
                    case HORIZONTAL: {
                        //往左滚动
                        if (distanceX > 0) {
                            int maxX = (mLines /*+ mStartLine*/) * space - (getWidth() >> 1) + getPaddingLeft();
                            if (getScrollX() + distanceX > maxX) {
                                scrollTo(maxX, 0);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            } else if (getScrollX() + distanceX <= maxX) {
                                scrollBy((int) distanceX, 0);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            }
                            //往右滚动
                        } else if (distanceX < 0) {
                            int minX = -((getWidth() >> 1) - getPaddingLeft())  + mStartLine * space;
                            if (getScrollX() + distanceX < minX) {
                                scrollTo(minX, 0);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            } else if (getScrollX() + distanceX >= minX) {
                                scrollBy((int) distanceX, 0);
                                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                            }
                        }
                    }
                    break;
                }

                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                switch (mOrientation) {
                    case VERTICAL: {
                        int minDistance = ((mLines) * space - (getHeight() >> 1)) + getPaddingBottom() ;
                        int maxDistance = (getHeight() >> 1) - getPaddingBottom() - mStartLine * space;
                        mOverScroller.fling(0, getScrollY(), 0, (int) (-velocityY / ratio), 0, 0, -minDistance, maxDistance, 0, 100);
                    }
                    break;
                    case HORIZONTAL: {
                        int minX = -((getWidth() >> 1) - getPaddingLeft() - mStartLine * space);
                        int maxX = (mLines /*+ mStartLine*/) * space - (getWidth() >> 1) + getPaddingLeft();
                        mOverScroller.fling(getScrollX(), 0, (int) (-velocityX / ratio), 0, minX, maxX, 0, 0, 100, 0);
                    }
                    break;
                }
                ViewCompat.postInvalidateOnAnimation(HeightView.this);
                return true;
            }
        });
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * 测量控件所需宽度
     */
    public int measureWidth(int widthMeasureSpec) {
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED: {
                switch (mOrientation) {
                    case HORIZONTAL: {
                        return mLines * space;
                    }
                    case VERTICAL:
                    default: {
                        float textWidth = mTextPaint.measureText(String.valueOf(mLines / mSetupValue));
                        float width = textWidth + mLongLineLength + mMarkerWidth + mMarkerSpace + getPaddingLeft() + getPaddingRight();
                        return (int) width;
                    }
                }
            }
            case MeasureSpec.EXACTLY:
            default:
                return MeasureSpec.getSize(widthMeasureSpec);
        }

    }

    /**
     * 测量空间所需高度
     */
    public int measureHeight(int heightMeasureSpec) {
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED: {
                switch (mOrientation) {
                    case HORIZONTAL: {
                        float height = mTextPaint.getTextSize() + mLongLineLength + mMarkerWidth + mMarkerSpace + getPaddingTop() + getPaddingBottom();
                        return (int) height;
                    }
                    case VERTICAL:
                    default: {
                        return mLines * space;
                    }
                }
            }
            case MeasureSpec.EXACTLY:
            default:
                return MeasureSpec.getSize(heightMeasureSpec);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (!mOverScroller.isFinished()) mOverScroller.abortAnimation();
                //adjustMarker(true);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                adjustMarker(true);
                break;
        }
        mGestureDetector.onTouchEvent(event);
        return true;
    }


    /**
     * 调整目前选择中的条目
     */
    public void adjustMarker(boolean adjustPosition) {

        final int previous = mCurrentLine;

        switch (mOrientation) {
            case VERTICAL: {
                int startY = (getHeight() >> 1) - getPaddingBottom();
                int scrollY = getScrollY();
                int progress = scrollY - startY;
                mCurrentLine = -progress / space;

                if (mCurrentLine > mLines) mCurrentLine = mLines;
                else if (mCurrentLine < mStartLine) mCurrentLine = mStartLine;

                int expectY = space * -mCurrentLine + startY;
                if (adjustPosition && scrollY != expectY) {
                    //scrollTo(0, expectY);
                    mOverScroller.startScroll(0, getScrollY(), 0, expectY - scrollY, 0);
                }
            }
            break;
            case HORIZONTAL: {
                int startX = -((getWidth() >> 1) - getPaddingLeft());
                int scrollX = getScrollX();
                int progress = startX - scrollX;
                mCurrentLine = -progress / space;
                if (mCurrentLine > mLines) mCurrentLine = mLines;
                else if (mCurrentLine < mStartLine) mCurrentLine = mStartLine;

                int expectX = space * mCurrentLine + startX;

                if (adjustPosition && scrollX != expectX) {
                    //scrollTo(0, expectY);
                    mOverScroller.startScroll(getScrollX(), 0, expectX - scrollX, 0, 0);
                }
            }
            break;
        }
        ViewCompat.postInvalidateOnAnimation(this);
        if (previous != mCurrentLine) onValueChanged();
    }

    /**
     * 当值可能发生变化后执行
     */
    public void onValueChanged() {
        if (mCurrentLine >= mStartLine && mCurrentLine <= mLines) {
            int index = mCurrentLine - mStartLine;
            int value = ((mStartLine + (index / mSetupValue)) * mSetupValue) + index % mSetupValue;
            if (mOnItemChangedListener != null) mOnItemChangedListener.onItemChanged(index, value);
        }
    }

    @Override
    public void computeScroll() {
        if (mOverScroller.computeScrollOffset()) {
            mPreviousIsFling = true;
            switch (mOrientation) {
                case VERTICAL:
                    scrollTo(0, mOverScroller.getCurrY());
                    break;
                case HORIZONTAL:
                    scrollTo(mOverScroller.getCurrX(), 0);
                    break;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (mPreviousIsFling) {
                mPreviousIsFling = false;
                adjustMarker(true);
            }
        }
    }

    /**
     * 重置线组
     */
    private void resetLinesArr() {
        if (mLinesArr.length < (mLines - mStartLine + 1) * 4) {
            //需要重新创建数组
            mLinesArr = new float[(mLines - mStartLine + 1) * 4];
        } else {
            Arrays.fill(mLinesArr, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        adjustMarker(false);
        canvas.drawColor(mBackgroundColor);
        switch (mOrientation) {
            case VERTICAL:
                drawVertical(canvas);
                break;
            case HORIZONTAL:
                drawHorizontal(canvas);
                break;
        }
    }

    /**
     * 绘制 方向为垂直方向时的布局
     *
     * @param canvas 画布
     */
    protected void drawVertical(Canvas canvas) {
        //vertical mode
        // bottom start position
        int bottom = getHeight() - getPaddingBottom();
        int left = getPaddingLeft();
        float maxTextWidth = mTextPaint.measureText(String.valueOf(mLines / mOutSideLine * mSetupValue));

        int shakeCenter = (getHeight() >> 1) + getScrollY();

        //绘制三角形
        //如果想使用图片，可以自行绘制图片
        mMarkerPath.reset();
        mMarkerPath.moveTo(left + maxTextWidth + mLongLineLength + mMarkerSpace, shakeCenter);
        mMarkerPath.lineTo(left + maxTextWidth + mLongLineLength + mMarkerSpace + mMarkerWidth, shakeCenter - mMarkerWidth);
        mMarkerPath.lineTo(left + maxTextWidth + mLongLineLength + mMarkerSpace + mMarkerWidth, shakeCenter + mMarkerWidth);
        mMarkerPath.lineTo(left + maxTextWidth + mLongLineLength + mMarkerSpace, shakeCenter);
        canvas.drawPath(mMarkerPath, mMarkerPaint);

        resetLinesArr();

        for (int i = mStartLine, j = mStartLine, k = 0; i <= mLines; i++, k++) {
            float lineLength;
            switch (i % mOutSideLine) {
                case ZERO: {
                    float currentTextWidth = mTextPaint.measureText(String.valueOf(i));
                    canvas.drawText(String.valueOf(j++ * mSetupValue), left + (maxTextWidth - currentTextWidth) / 2, bottom - i * space, mTextPaint);
                    lineLength = mLongLineLength;
                }
                break;
                default:
                    lineLength = mShortLineLength;
                    break;
            }
            mLinesArr[k * 4] = left + maxTextWidth;
            mLinesArr[k * 4 + 1] = bottom - i * space;
            mLinesArr[k * 4 + 2] = left + maxTextWidth + lineLength;
            mLinesArr[k * 4 + 3] = bottom - i * space;
        }

        //绘制线
        canvas.drawLines(mLinesArr, mPaint);
        //绘制高亮线
        canvas.drawLine(left + maxTextWidth, bottom - mCurrentLine * space, left + maxTextWidth + (mCurrentLine % mOutSideLine == 0 ? mLongLineLength : mShortLineLength), bottom - mCurrentLine * space, mHighlightPaint);
    }

    /**
     * 绘制水平方向上的布局
     *
     * @param canvas 画布
     */
    protected void drawHorizontal(Canvas canvas) {

        //Horizontal mode
        // bottom position
        int bottom = getHeight() - getPaddingBottom();
        int left = getPaddingLeft();

        //中心
        int shakeCenter = (getWidth() >> 1) + getScrollX();

        //三角形顶点
        float vertexY = getHeight() - getPaddingBottom() - mTextPaint.getTextSize() - mLongLineLength - mMarkerSpace;

        //绘制三角形
        mMarkerPath.reset();
        mMarkerPath.moveTo(shakeCenter, vertexY);
        mMarkerPath.lineTo(shakeCenter - mMarkerWidth, vertexY - mMarkerWidth);
        mMarkerPath.lineTo(shakeCenter + mMarkerWidth, vertexY - mMarkerWidth);
        mMarkerPath.lineTo(shakeCenter, vertexY);
        canvas.drawPath(mMarkerPath, mMarkerPaint);
        //验证线组
        resetLinesArr();

        //生成线组
        for (int i = mStartLine, j = mStartLine, k = 0; i <= mLines; i++, k++) {
            float lineLength;
            switch (i % mOutSideLine) {
                case ZERO: {
                    canvas.drawText(String.valueOf(j++ * mSetupValue), left + i * space, bottom, mTextPaint);
                    lineLength = mLongLineLength;
                }
                break;
                default:
                    lineLength = mShortLineLength;
                    break;
            }
            /*startX*/
            mLinesArr[k * 4] = left + i * space;
            /*startY*/
            mLinesArr[k * 4 + 1] = bottom - mTextPaint.getTextSize();
            /*stopX*/
            mLinesArr[k * 4 + 2] = left + i * space;
            /*stopY*/
            mLinesArr[k * 4 + 3] = bottom - (mTextPaint.getTextSize() + lineLength);
        }

        //绘制线组
        canvas.drawLines(mLinesArr, mPaint);

        //绘制当前选中的线条
        canvas.drawLine(left + mCurrentLine * space, (bottom - mTextPaint.getTextSize()), left + mCurrentLine * space,
                (bottom - mTextPaint.getTextSize()) - (mCurrentLine % mOutSideLine == 0 ? mLongLineLength : mShortLineLength), mHighlightPaint);

    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        adjustMarker(true);
    }

    public int getLines() {
        return mLines;
    }

    public void setLines(int mLines) {
        this.mLines = mLines;
        requestLayout();
    }

    public int getOutSideLine() {
        return mOutSideLine;
    }

    public void setOutSideLine(int mOutSideLine) {
        this.mOutSideLine = mOutSideLine;
        invalidate();
    }

    public int getSetupValue() {
        return mSetupValue;
    }

    public void setSetupValue(int mSetupValue) {
        this.mSetupValue = mSetupValue;
        invalidate();
    }

    public int getCurrentLine() {
        return mCurrentLine;
    }

    public void setCurrentLine(int mCurrentLine) {
        this.mCurrentLine = mCurrentLine;
        invalidate();
        adjustMarker(true);
    }

    public int getSpace() {
        return space;
    }

    public void setSpace(int space) {
        this.space = space;
        requestLayout();
    }


    public float getShortLineLength() {
        return mShortLineLength;
    }

    public void setShortLineLength(float mShortLineLength) {
        this.mShortLineLength = mShortLineLength;
        invalidate();
    }

    public float getLongLineLength() {
        return mLongLineLength;
    }

    public void setLongLineLength(float mLongLineLength) {
        this.mLongLineLength = mLongLineLength;
        invalidate();
    }


    public int getHighLightColor() {
        return mHighLightColor;
    }

    public void setHighLightColor(int highLightColor) {
        this.mHighLightColor = highLightColor;
        mHighlightPaint.setColor(highLightColor);
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mTextPaint.setColor(textColor);
        invalidate();
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        this.mTextSize = textSize;
        mTextPaint.setTextSize(textSize);
        requestLayout();
    }

    public float getHighlightWidth() {
        return mHighlightWidth;
    }

    public void setHighlightWidth(float highlightWidth) {
        this.mHighlightWidth = highlightWidth;
        mHighlightPaint.setStrokeWidth(highlightWidth);
        requestLayout();
    }

    public float getLineWidth() {
        return mLineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.mLineWidth = lineWidth;
        mPaint.setStrokeWidth(lineWidth);
        requestLayout();
    }

    public int getLineColor() {
        return mLineColor;
    }

    public void setLineColor(int lineColor) {
        this.mLineColor = lineColor;
        mPaint.setColor(lineColor);
        invalidate();
    }


    public int getStartLine() {
        return mStartLine;
    }

    public void setStartLine(int startLine) {
        this.mStartLine = startLine;
        requestLayout();
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
        requestLayout();
    }

    public int getMarkerWidth() {
        return mMarkerWidth;
    }

    public void setMarkerWidth(int markerWidth) {
        this.mMarkerWidth = markerWidth;
        requestLayout();
    }

    public int getMarkerSpace() {
        return mMarkerSpace;
    }

    public void setMarkerSpace(int markerSpace) {
        this.mMarkerSpace = markerSpace;
        requestLayout();
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
        invalidate();
    }

    public int getMarkerColor() {
        return mMarkerColor;
    }

    public void setMarkerColor(int markerColor) {
        this.mMarkerColor = markerColor;
        mMarkerPaint.setColor(markerColor);
        invalidate();
    }

    public float getRatio() {
        return ratio;
    }

    /**
     * 设置阻尼系数
     *
     * @param ratio 需要设置的阻尼系数，默认的是 {@link #DEFAULT_RATIO}
     */
    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    /**
     * 设置item变化监听器
     *
     * @param listener 需要设置item变化监听器
     */
    public void setOnItemChangedListener(OnItemChangedListener listener) {
        this.mOnItemChangedListener = listener;
    }


    /**
     * 条目变化监听器
     */
    public interface OnItemChangedListener {
        /**
         * 当条目发生变化后调用
         *
         * @param index 当前的选择中的条目的下表
         * @param value 选择中的条目的值
         */
        void onItemChanged(int index, int value);
    }

}
