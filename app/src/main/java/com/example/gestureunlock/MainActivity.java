package com.example.gestureunlock;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * <pre>
 *     author : Clement
 *     time   : 2018/05/08
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class MainActivity extends AppCompatActivity {

    private String mPassword;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GestureUnlockView mUnlockView = (GestureUnlockView) findViewById(R.id.unlock);
        mUnlockView.setMode(UnlockMode.CREATE);
        mUnlockView.setGestureListener(new GestureUnlockView.GestureListener() {
            @Override public boolean onGestureFinish(UnlockMode mode, String result) {
                switch (mode) {
                    case CREATE:
                        mPassword = result;
                        Toast.makeText(MainActivity.this, "Set Gesture Succeeded!",
                            Toast.LENGTH_SHORT).show();
                        mUnlockView.setMode(UnlockMode.CHECK);
                        return false;
                    case CHECK:
                        boolean checkSuccess = result.equals(mPassword);
                        if (checkSuccess) {
                            Toast.makeText(MainActivity.this, "成功：" + result, Toast.LENGTH_SHORT)
                                .show();
                        } else {
                            Toast.makeText(MainActivity.this, "失败：" + result, Toast.LENGTH_SHORT)
                                .show();
                        }
                        return checkSuccess;
                }
                return true;
            }
        });
    }
}
