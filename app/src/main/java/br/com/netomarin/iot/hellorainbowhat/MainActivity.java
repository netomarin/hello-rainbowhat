/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.netomarin.iot.hellorainbowhat;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int LED_DIRECTION_ASC = 1;
    private static final int LED_DIRECTION_DESC = 0;
    private static final int LED_STRIP_SIZE = 7;
    private static final long INTERVAL_BETWEEN_BLINKS_MS = 500;

    private Handler mHandler = new Handler();

    private ButtonInputDriver mButtonA;
    private ButtonInputDriver mButtonB;
    private ButtonInputDriver mButtonC;
    private AlphanumericDisplay mDisplay;
    private Apa102 mLedStrip;
    private int[] mLedColors = new int[LED_STRIP_SIZE];
    private Speaker mSpeaker;

    private int mLedCyle;
    private int mLedDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        try {
            mButtonA = new ButtonInputDriver("BCM21",
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_A);
            mButtonA.register();

            mButtonB = new ButtonInputDriver("BCM20",
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_B);
            mButtonB.register();

            mButtonC = new ButtonInputDriver("BCM16",
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_C);
            mButtonC.register();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mDisplay = new AlphanumericDisplay("I2C1");
            mDisplay.setEnabled(true);
            mDisplay.clear();
            mDisplay.display('H', 1, false);
            mDisplay.display('I', 2, false);
        } catch (IOException e) {
            Log.e(TAG, "Erro inicializando display");
            mDisplay = null;
        }

        try {
            mLedStrip = new Apa102("SPI0.0", Apa102.Mode.RGB);
            mLedStrip.setBrightness(1);
            //Iniciando ciclos do Led
            mLedCyle = 0;
            // Direcao ascedente
            mLedDirection = LED_DIRECTION_ASC;
        } catch (IOException e) {
            Log.e(TAG, "Erro inicializando leds");
            e.printStackTrace();
            mLedStrip = null;
        }

        mHandler.post(mLedsRunnable);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            mDisplay.clear();

            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    mDisplay.clear();
                    mDisplay.display('A', 0, false);
                    break;
                case KeyEvent.KEYCODE_B:
                    mDisplay.clear();
                    mDisplay.display('B', 1, false);
                    break;
                case KeyEvent.KEYCODE_C:
                    mDisplay.clear();
                    mDisplay.display('C', 2, false);
                    break;
                default:
                    mDisplay.display("!!!!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            mDisplay.clear();
            mDisplay.display('H', 1, false);
            mDisplay.display('I', 2, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mButtonA != null) {
            try {
                mButtonA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonA = null;
        }

        if(mButtonB != null) {
            try {
                mButtonB.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonB = null;
        }

        if (mButtonC != null) {
            try {
                mButtonC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonC = null;
        }

        if (mLedStrip != null) {
            try {
                mLedStrip.write(new int[7]);
                mLedStrip.setBrightness(0);
                mLedStrip.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mLedStrip = null;
            }
        }
    }

    private Runnable mLedsRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLedStrip == null) {
                return;
            }

            for (int i = 0; i < LED_STRIP_SIZE; i++) {
                if (i <= mLedCyle) {
                    mLedColors[i] = Color.GREEN;
                } else {
                    mLedColors[i] = 0;
                }
            }

            try {
                mLedStrip.write(mLedColors);
            } catch (IOException e) {
                Log.e(TAG, "Erro ao ligar LEDs");
                return;
            }

            if (mLedDirection == LED_DIRECTION_ASC) {
                mLedCyle++;
                if (mLedCyle == LED_STRIP_SIZE) {
                    mLedCyle--;
                    mLedDirection = LED_DIRECTION_DESC;
                }
            } else {
                mLedCyle--;
                if (mLedCyle < 0) {
                    mLedCyle = 0;
                    mLedDirection = LED_DIRECTION_ASC;
                }
            }

            mHandler.postDelayed(mLedsRunnable, INTERVAL_BETWEEN_BLINKS_MS);
        }
    };
}
