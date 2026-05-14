package com.zfile.manager;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Launcher activity. Stage 1 placeholder — Stage 2 wires this up to host
 * the {@code FileBrowserFragment} via {@code BottomNavigationView} +
 * Navigation Component.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
