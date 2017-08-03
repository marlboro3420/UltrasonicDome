package com.yan.ultrasonicdome;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * @author Yansj
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Handler mHandler = new Handler();
    private Gpio mTrigGpio;
    private Gpio mEchoGpio;

    private final String pinTrig = "BCM20";
    private final String pinEcho = "BCM26";

    private long startTime;
    private long endTime;

    private TextView text;

    /**
     * 超声波模块的工作原理为，先向TRIG脚输入至少10us的触发信号,
     * 该模块内部将发出 8 个 40kHz 周期电平并检测回波。
     * 一旦检测到有回波信号则ECHO输出高电平回响信号。
     * 回响信号的脉冲宽度与所测的距离成正比。
     * 由此通过发射信号到收到的回响信号时间间隔可以计算得到距离。
     * 公式: 距离=高电平时间*声速(340M/S)/2。
     * <p>
     * <p>
     * VCC,超声波模块电源脚，接5V电源即可
     * Trig，超声波发送脚，高电平时发送出40KHZ出超声波
     * Echo，超声波接收检测脚，当接收到返回的超声波时，输出高电平
     * GND，超声波模块GND
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "Starting MainActivity");
        text = (TextView) findViewById(R.id.text);

        PeripheralManagerService trigService = new PeripheralManagerService();
        PeripheralManagerService echoService = new PeripheralManagerService();

        try {
            InitTrigGpio(trigService);
            InitEchoGpio(echoService);
            mHandler.post(mSendRunnable);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    /**
     * 初始化输出信号针脚
     *
     * @param service
     * @throws IOException
     */
    private void InitTrigGpio(PeripheralManagerService service) throws IOException {
        mTrigGpio = service.openGpio(pinTrig);//Trig针脚
        mTrigGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);//将引脚初始化为低电平输出
        mTrigGpio.setActiveType(Gpio.ACTIVE_HIGH);//输出电压设置为高电压
    }

    /**
     * 初始化输入信号针脚
     *
     * @param service
     * @throws IOException
     */
    private void InitEchoGpio(PeripheralManagerService service) throws IOException {
        Log.i(TAG, "InitEchoGpio");
        mEchoGpio = service.openGpio(pinEcho);//Echo针脚
        mEchoGpio.setDirection(Gpio.DIRECTION_IN);//将引脚初始化为输入
        mEchoGpio.setActiveType(Gpio.ACTIVE_HIGH);//设置收到高电压是有效的结果
        //注册状态更改监听类型 EDGE_NONE（无更改，默认）EDGE_RISING（从低到高）EDGE_FALLING（从高到低）
        mEchoGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
        mEchoGpio.registerGpioCallback(mGpioCallback);//注册回调
    }

    private boolean needMasure;
    private int masureCount;
    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            // 读取高电平有效状态
            try {
                if (gpio.getValue()) {//发现高电平开始计时
                    if (needMasure) {//第N次发现高电平计时中
                        Log.e("收到高电平", "计时中");
                        text.setText("masureCount:" + masureCount + ",Result : In measurement");
                    } else {//第一次发现高电平开始计时
                        startTime = System.currentTimeMillis();
                        needMasure = true;
                    }
                } else {//如果计时中发现低电平信号结束测量，如果没有计时中忽略掉
                    if (needMasure) {
                        endTime = System.currentTimeMillis();
                        double time = (endTime - startTime) / 1000.0;
                        Log.e("第" + masureCount + "次测量时间：", "经过时间：" + time);
                        //测量结果，距离=（声音飞行时间 * 声音速度） / 2 因为是声音来回的时间。
                        Log.e("第" + masureCount + "次测量，距离为：", " " + (time * 340 / 2) + " M");

                        text.setText("masureCount:" + masureCount + ",Result : " + (time * 340 / 2) + " M");
                        needMasure = false;
                    } else {
                        Log.e("第" + masureCount + "次测量", "未接收到高电平");
                        text.setText("masureCount:" + masureCount + ",Result : No Found Hign pin");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;//返回true 接受更多的状态
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };


    private Runnable mSendRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (mTrigGpio == null) {
                return;
            }
            try {
                masureCount++;
                mTrigGpio.setValue(true);//输出高电平
                Log.d(TAG, "State set to High");
                Thread.sleep((long) 0.025);//高电平输出25US
//                Log.d(TAG, "过了25US");
                mTrigGpio.setValue(false);//恢复低电平
//                Log.d(TAG, "State set to Low");

                mHandler.postDelayed(mSendRunnable, 3000);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mSendRunnable);
        if (mTrigGpio != null) {
            try {
                mTrigGpio.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close mTrigGpio", e);
            } finally {
                mTrigGpio = null;
            }
        }

        if (mEchoGpio != null) {
            try {
                mEchoGpio.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close mEchoGpio", e);
            } finally {
                mEchoGpio = null;
            }
        }
        mEchoGpio.unregisterGpioCallback(mGpioCallback);
    }
}
