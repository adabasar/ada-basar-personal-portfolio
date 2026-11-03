package com.example.gridlayout;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Intent intent = getIntent();
        int seconds = intent.getIntExtra(MainActivity.EXTRA_SECONDS, 0);
        boolean won = intent.getBooleanExtra(MainActivity.EXTRA_WON, false);

        TextView timeTv = findViewById(R.id.resultTime);
        TextView messageTv = findViewById(R.id.resultMessage);
        Button playAgain = findViewById(R.id.playAgainButton);

        timeTv.setText(getString(R.string.result_time, seconds));
        messageTv.setText(won ? R.string.result_won : R.string.result_lost);
        playAgain.setOnClickListener(this::onClickPlayAgain);
    }

    // start a brand-new game and close this page
    public void onClickPlayAgain(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
