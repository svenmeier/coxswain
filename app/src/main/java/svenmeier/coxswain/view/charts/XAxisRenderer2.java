package svenmeier.coxswain.view.charts;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Support for rendering {@link LimitArea}.
 */
public class XAxisRenderer2 extends XAxisRenderer {

    private Paint mLimitAreaPaint;

    public XAxisRenderer2(LineChart chartView) {
        super(chartView.getViewPortHandler(), chartView.getXAxis(), chartView.getTransformer(YAxis.AxisDependency.LEFT));

        mLimitAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLimitAreaPaint.setStyle(Paint.Style.FILL);
    }

    protected float[] mRenderLimitAreaBuffer = new float[2];
    private Path mLimitAreaPath = new Path();

    public void renderLimitLineLine(Canvas c, LimitLine limitLine, float[] position1) {

        if (limitLine instanceof LimitArea) {
            float[] position2 = mRenderLimitAreaBuffer;
            position2[0] = ((LimitArea) limitLine).getMinimum();
            position2[1] = 0.f;

            mTrans.pointValuesToPixel(position2);

            mLimitAreaPath.reset();
            mLimitAreaPath.moveTo(position2[0],  mViewPortHandler.contentTop());
            mLimitAreaPath.lineTo(position1[0], mViewPortHandler.contentTop());
            mLimitAreaPath.lineTo(position1[0], mViewPortHandler.contentBottom());
            mLimitAreaPath.lineTo(position2[0],  mViewPortHandler.contentBottom());
            mLimitAreaPath.close();

            mLimitAreaPaint.setColor(limitLine.getLineColor());

            c.drawPath(mLimitAreaPath, mLimitAreaPaint);
        } else {
            super.renderLimitLineLine(c, limitLine, position1);
        }
    }
}
