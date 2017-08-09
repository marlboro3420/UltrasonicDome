package com.yan.hongwai;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Gpio mInputGpio;
    private final String pinInput = "BCM4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "Starting MainActivity");

        PeripheralManagerService peripheralManagerService = new PeripheralManagerService();
        try {
            mInputGpio = peripheralManagerService.openGpio(pinInput);//Echo针脚
            mInputGpio.setDirection(Gpio.DIRECTION_IN);//将引脚初始化为输入
            mInputGpio.setActiveType(Gpio.ACTIVE_HIGH);//设置收到高电压是有效的结果
            //注册状态更改监听类型 EDGE_NONE（无更改，默认）EDGE_RISING（从低到高）EDGE_FALLING（从高到低）
            mInputGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mInputGpio.registerGpioCallback(mGpioCallback);//注册回调
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    Log.e("有人来了", gpio.getValue() + ":1111111111111");
                } else {
                    Log.e("没有人", gpio.getValue() + ":222222222222");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            super.onGpioError(gpio, error);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInputGpio != null) {
            try {
                mInputGpio.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close mEchoGpio", e);
            } finally {
                mInputGpio = null;
            }
        }
        mInputGpio.unregisterGpioCallback(mGpioCallback);
    }
}
