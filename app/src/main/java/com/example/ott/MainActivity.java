package com.example.ott;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Dummy launcher activity required by Android TV.
 * The actual TV experience runs through Live Channels → OttTvInputService.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView status = findViewById(R.id.tv_status);
        status.setText(R.string.main_status_message);
    }
}
