package io.github.f401.jbplayer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CharacterSideBarView extends View {
    private static final String TAG = "CharacterSideBarView";
    private static final String[] CHARACTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
            "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private final Paint mTextPaint;

    public CharacterSideBarView(Context context) {
        this(context, null);
    }

    public CharacterSideBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CharacterSideBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTextPaint.getFontMetrics(font);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec), width = MeasureSpec.getSize(widthMeasureSpec),
                heightMode = MeasureSpec.getMode(heightMeasureSpec), height = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthMode = MeasureSpec.EXACTLY;
            width = Math.round(mTextPaint.measureText(CHARACTERS[0]) + mTextPaint.getFontSpacing());
        }
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(width, widthMode),
                MeasureSpec.makeMeasureSpec(height, heightMode));
    }

    private Paint.FontMetrics font = new Paint.FontMetrics();

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float itemH = (float) getMeasuredHeight() / CHARACTERS.length;
        mTextPaint.getFontMetrics(font);
        float singleLetterH = font.descent - font.ascent;
        float leftStart = getPaddingLeft();
        for (int i = 0; i < CHARACTERS.length; ++i) {
            canvas.drawText(CHARACTERS[i], leftStart, getPaddingTop() + itemH * i + singleLetterH, mTextPaint);
        }
    }
}
