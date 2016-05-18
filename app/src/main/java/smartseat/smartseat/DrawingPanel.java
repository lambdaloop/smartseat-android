package smartseat.smartseat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by pierre on 5/8/16.
 */
public class DrawingPanel extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = DrawingPanel.class.getSimpleName();

    private PanelThread _thread;
    Paint paint = new Paint();
    double[] sensorValues;

    private int gridNum;
    private int minGap;

    public DrawingPanel(Context context) {
        super(context);
        init();
    }

    public DrawingPanel(Context context, AttributeSet attrs) {
        super(context);
        init();
    }

    public DrawingPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        sensorValues = new double[MainActivity.NUM_SENSORS];
        gridNum = (int) Math.ceil(Math.sqrt(MainActivity.NUM_SENSORS)-0.001);
        Log.i(TAG, String.format("gridNum: %d", gridNum));
        minGap = 90;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int height = canvas.getHeight();
        int width = canvas.getWidth();

        int rectWidth, rectHeight;
        rectHeight = rectWidth = (Math.min(width,height)-minGap*(gridNum+1))/gridNum;

        int gapX = (width - rectWidth*gridNum)/(gridNum+1);
        int gapY = (height - rectHeight*gridNum)/(gridNum+1);

        // fill with white
//        paint.setARGB(255, 250, 250, 250);
//        canvas.drawPaint(paint);



        for(int i=0; i<sensorValues.length; i+=1) {
            int x = i % gridNum;
            int y = i / gridNum;
            double value = sensorValues[i];
            paint.setARGB(255, (int)(255*value), (int)(255*(1-value)), 0);

            int left = gapX + x*(rectWidth+minGap);
            int top = gapY + y*(rectHeight+minGap);

            canvas.drawRect(left, top, rectWidth + left, rectHeight + top, paint);  // left, top, right, bottom
        }


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setWillNotDraw(false);

        _thread = new PanelThread(getHolder(), this); //Start the thread that
        _thread.setRunning(true);                     //will make calls to 
        _thread.start();                              //onDraw()
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            _thread.setRunning(false);                //Tells thread to stop
            _thread.join();                           //Removes thread from mem.
        } catch (InterruptedException e) {}
    }

    // sensor value should be in [0,1]
    public void updateSensor(int sensor, double value) {
        Log.i(TAG, String.format("%d %.3f", sensor, value));
        sensorValues[sensor] = value;
    }
}


class PanelThread extends Thread {
    private SurfaceHolder _surfaceHolder;
    private DrawingPanel _panel;
    private boolean _run = false;


    public PanelThread(SurfaceHolder surfaceHolder, DrawingPanel panel) {
        _surfaceHolder = surfaceHolder;
        _panel = panel;
    }


    public void setRunning(boolean run) { //Allow us to stop the thread
        _run = run;
    }


    @Override
    public void run() {
        Canvas c;
        while (_run) {     //When setRunning(false) occurs, _run is
            c = null;      //set to false and loop ends, stopping thread


            try {


                c = _surfaceHolder.lockCanvas(null);
                synchronized (_surfaceHolder) {


                    //Insert methods to modify positions of items in onDraw()
                    _panel.postInvalidate();


                }
            } finally {
                if (c != null) {
                    _surfaceHolder.unlockCanvasAndPost(c);
                }
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

