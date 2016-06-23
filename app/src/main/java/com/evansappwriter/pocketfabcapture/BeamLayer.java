package com.evansappwriter.pocketfabcapture;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * Created by markevans on 6/23/16.
 */
public class BeamLayer extends View {
    private final static String VORTEX_KEY = "vortexdKey";
    Messenger myService = null;
    boolean isBound;
    private Context mContext;
    private FrameLayout mFrameLayout;
    private WindowManager mWindowManager;
    private int mShots = 0;

    public BeamLayer(Context context) {
        super(context);
        mContext = context;
        mFrameLayout = new FrameLayout(mContext);

        addToWindowManager();
    }

    private void addToWindowManager() {
        Intent intent = new Intent();
        intent.setClassName("com.pocket.doorway", "com.pocket.doorway.input.TractorBeamTargetingService");
        mContext.bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(mFrameLayout, params);

        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Here is the place where you can inject whatever layout you want.
        layoutInflater.inflate(R.layout.beam_layer, mFrameLayout);

        final LinearLayout parent = (LinearLayout) mFrameLayout.findViewById(R.id.parent);
        parent.setOnTouchListener(new View.OnTouchListener() {
            private int initX, initY;
            private int initTouchX, initTouchY;

            public boolean onTouch(View v, MotionEvent event) {
                int x = (int)event.getRawX();
                int y = (int)event.getRawY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        initTouchX = x;
                        initTouchY = y;

                        return true;

                    case MotionEvent.ACTION_UP:
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initX + (x - initTouchX);
                        params.y = initY + (y - initTouchY);

                        Log.i("BeamLayer", "Fired at " + x + ", " + y);
                        fireVortex(x,y);

                        // Invalidate layout
                        mWindowManager.updateViewLayout(mFrameLayout, params);
                        return true;
                }
                return false;
            }
        });
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            myService = new Messenger(service);
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            myService = null;
            isBound = false;
        }
    };

    public void fireVortex(int x, int y)
    {
        if (!isBound) return;
        mShots++;

        Message msg = Message.obtain();
        msg.what = getVortex();
        msg.arg1 = x;
        msg.arg2 = y;

        try {
            myService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (mShots==50) {
            mShots = 0;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int getVortex() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(mContext);
        String color = SP.getString(VORTEX_KEY,"1");
        return Color.parseColor(color);
    }

    /**
     * Removes the view from window manager.
     */
    public void destroy() {
        if (isBound) {
            mContext.unbindService(myConnection);
        }

        mWindowManager.removeView(mFrameLayout);
    }
}
