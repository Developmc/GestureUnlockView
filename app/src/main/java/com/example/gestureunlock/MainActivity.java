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

    private String pwd;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GestureUnlockView mUnlockView = (GestureUnlockView) findViewById(R.id.unlock);
        mUnlockView.setMode(UnlockMode.CREATE);
        mUnlockView.setGestureListener(new GestureUnlockView.CreateGestureListener() {
            @Override public void onGestureCreated(String result) {
                pwd = result;
                Toast.makeText(MainActivity.this, "Set Gesture Succeeded!", Toast.LENGTH_SHORT)
                    .show();
                mUnlockView.setMode(UnlockMode.CHECK);
            }
        });
        mUnlockView.setOnUnlockListener(new GestureUnlockView.OnUnlockListener() {
            @Override public boolean isUnlockSuccess(String result) {
                if (result.equals(pwd)) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override public void onSuccess() {
                Toast.makeText(MainActivity.this, "Check Succeeded!", Toast.LENGTH_SHORT).show();
            }

            @Override public void onFailure() {
                Toast.makeText(MainActivity.this, "Check Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
