package com.lovely3x.weightmeasurer.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.lovely3x.view.HeightView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HeightView hv = (HeightView) findViewById(R.id.hv_activity_main);
        hv.setOnItemChangedListener(new HeightView.OnItemChangedListener() {
            @Override
            public void onItemChanged(int index, int value) {
                Log.i(TAG,String.format("onItemChanged index == %d value == %d ",index,value));
            }
        });
    }
}
