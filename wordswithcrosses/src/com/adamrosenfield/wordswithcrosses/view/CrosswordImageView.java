package com.adamrosenfield.wordswithcrosses.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import com.adamrosenfield.wordswithcrosses.WordsWithCrossesApplication;
import com.adamrosenfield.wordswithcrosses.puz.Playboard;
import com.adamrosenfield.wordswithcrosses.puz.Playboard.Position;
import com.adamrosenfield.wordswithcrosses.puz.Playboard.Word;

public class CrosswordImageView extends TouchImageView
{
    private Playboard board;
    private int boardWidth = 1;
    private int boardHeight = 1;

    private float renderScale = 1.0f;
    private float minRenderScale = 1.0f;
    private float maxRenderScale = 1.0f;

    private ClickListener clickListener;
    private RenderScaleListener renderScaleListener;

    public CrosswordImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setBoard(Playboard board, DisplayMetrics metrics)
    {
        this.board = board;
        boardWidth = board.getWidth();
        boardHeight = board.getHeight();

        // TODO: Get the max texture size from OpenGL instead of hard-coding it
        minRenderScale = 0.12f * 160 * metrics.density / PlayboardRenderer.BOX_SIZE;
        maxRenderScale = 2047.0f / (PlayboardRenderer.BOX_SIZE * Math.max(boardWidth, boardHeight));
    }

    public float zoomIn()
    {
        return setRenderScale(renderScale * 1.5f);
    }

    public float zoomOut()
    {
        return setRenderScale(renderScale / 1.5f);
    }

    public float fitToScreen()
    {
        float horzScale = (float)getWidth() / (boardWidth * PlayboardRenderer.BOX_SIZE);
        float vertScale = (float)getHeight() / (boardHeight * PlayboardRenderer.BOX_SIZE);
        return setRenderScale(Math.min(horzScale, vertScale));
    }

    public float setRenderScale(float newScale)
    {
        float oldBoxSize = PlayboardRenderer.BOX_SIZE * renderScale;
        Position highlight = board.getHighlightLetter();
        float pivotX = ((float)highlight.across + 0.5f) * oldBoxSize;
        float pivotY = ((float)highlight.down + 0.5f) * oldBoxSize;

        return setRenderScale(newScale, pivotX, pivotY);
    }

    private float setRenderScale(float newScale, float pivotX, float pivotY)
    {
        // Clamp the scale to our range.  If it didn't change appreciably, don't
        // do anything
        newScale = Math.min(Math.max(newScale, minRenderScale), maxRenderScale);
        if (Math.abs(newScale - renderScale) < 0.0001f)
        {
            return renderScale;
        }

        PointF bmPivot = pixelToBitmapPos(pivotX, pivotY);
        float relScale = newScale / renderScale;
        float tx = pivotX - relScale * bmPivot.x;
        float ty = pivotY - relScale * bmPivot.y;

        renderScale = newScale;
        render();

        // Reset the TouchImageView scale back to 1.0 so we can re-render at
        // the new proper resolution
        setMinScale(minRenderScale / renderScale);
        setMaxScale(maxRenderScale / renderScale);
        setScaleAndTranslate(1.0f, tx, ty);

        if (renderScaleListener != null)
        {
            renderScaleListener.onRenderScaleChanged(renderScale);
        }

        return renderScale;
    }

    public float getRenderScale()
    {
        return renderScale;
    }

    public void setClickListener(ClickListener listener)
    {
        clickListener = listener;
    }

    public void setRenderScaleListener(RenderScaleListener listener)
    {
        renderScaleListener = listener;
    }

    public void ensureVisible(Position pos)
    {
        Matrix m = getImageMatrix();

        float boxSize = PlayboardRenderer.BOX_SIZE * renderScale;
        float x = pos.across * boxSize;
        float y = pos.down * boxSize;

        float[] p = new float[]{x, y};
        m.mapPoints(p);

        float dx = 0.0f, dy = 0.0f;
        if (p[0] < 0.0f)
        {
            dx = -p[0];
        }
        else if (p[0] > getWidth() - boxSize)
        {
            dx = getWidth() - boxSize - p[0];
        }

        if (p[1] < 0.0f)
        {
            dy = -p[1];
        }
        else if (p[1] > getHeight() - boxSize)
        {
            dy = getHeight() - boxSize - p[1];
        }

        if (dx != 0.0f || dy != 0.0f)
        {
            translate(dx, dy);
        }
    }

    private enum ClickEvent
    {
        CLICK,
        DOUBLE_CLICK,
        LONG_CLICK
    };

    @Override
    protected void onClick(PointF pos)
    {
        onClickEvent(ClickEvent.CLICK, pos);
    }

    @Override
    protected void onDoubleClick(PointF pos)
    {
        onClickEvent(ClickEvent.DOUBLE_CLICK, pos);
    }

    @Override
    protected void onLongClick(PointF pos)
    {
        onClickEvent(ClickEvent.LONG_CLICK, pos);
    }

    private void onClickEvent(ClickEvent event, PointF pos)
    {
        if (clickListener == null)
        {
            return;
        }

        float boxSize = PlayboardRenderer.BOX_SIZE * renderScale;
        int x = (int)(pos.x / boxSize);
        int y = (int)(pos.y / boxSize);
        Position crosswordPos = null;
        if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
            crosswordPos = new Position(x, y);
        }

        switch (event)
        {
        case CLICK:
            clickListener.onClick(crosswordPos);
            break;

        case DOUBLE_CLICK:
            clickListener.onDoubleClick(crosswordPos);
            break;

        case LONG_CLICK:
            clickListener.onLongClick(crosswordPos);
        }
    }

    @Override
    protected void onScaleEnd(float newScale) {
        // Re-render at the new scale and set the TouchImageView scale back to
        // 1.0
        setRenderScale(renderScale * newScale);
    }

    public void render()
    {
        render(null);
    }

    public void render(Word prevWord)
    {
        Bitmap bitmap = WordsWithCrossesApplication.RENDERER.draw(prevWord, renderScale);
        setImageBitmap(bitmap);
    }

    public interface ClickListener
    {
        public void onClick(Position pos);

        public void onDoubleClick(Position pos);

        public void onLongClick(Position pos);
    }

    public interface RenderScaleListener
    {
        public void onRenderScaleChanged(float renderScale);
    }
}