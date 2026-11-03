package com.example.gridlayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int COLUMN_COUNT = 10;
    private static final int ROW_COUNT = 10;
    private static final int MINE_COUNT = 6;
    private static final int SAFE_CELLS = ROW_COUNT * COLUMN_COUNT - MINE_COUNT;   // 94

    // keys for passing the result to ResultActivity
    public static final String EXTRA_SECONDS = "com.example.gridlayout.SECONDS";
    public static final String EXTRA_WON = "com.example.gridlayout.WON";

    // cell colors (match the handout's example screenshots)
    private static final int UNREVEALED_COLOR = Color.parseColor("#7CC540");
    private static final int REVEALED_COLOR = Color.LTGRAY;
    private static final int NUMBER_COLOR = Color.DKGRAY;

    // save the TextViews of all cells in an array, so later on,
    // when a TextView is clicked, we know which cell it is
    private ArrayList<TextView> cell_tvs;

    // top bar and mode button
    private TextView flagCountTv;
    private TextView timerTv;
    private TextView modeButton;

    // game state
    private boolean[][] isMine;
    private boolean[][] isRevealed;
    private boolean[][] isFlagged;
    private int[][] adjacent;        // number of mines in the 8 neighbouring cells

    private boolean flagMode = false; // game must start in dig mode
    private boolean gameOver = false;
    private boolean won = false;
    private int flagsLeft = MINE_COUNT;
    private int revealedCount = 0;

    // timer (same idea as the StopWatch example app)
    private int clock = 0;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (running) {
                clock++;
                updateTimerText();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flagCountTv = findViewById(R.id.flagCount);
        timerTv = findViewById(R.id.timer);
        modeButton = findViewById(R.id.modeButton);
        modeButton.setOnClickListener(this::onClickMode);

        cell_tvs = new ArrayList<TextView>();

        // create the 10x10 grid of cells
        GridLayout grid = (GridLayout) findViewById(R.id.gridLayout01);

        LayoutInflater li = LayoutInflater.from(this);
        for (int i = 0; i < ROW_COUNT; i++) {
            for (int j = 0; j < COLUMN_COUNT; j++) {
                TextView tv = (TextView) li.inflate(R.layout.custom_cell_layout, grid, false);
                tv.setBackgroundColor(UNREVEALED_COLOR);
                tv.setOnClickListener(this::onClickTV);

                GridLayout.LayoutParams lp = (GridLayout.LayoutParams) tv.getLayoutParams();
                lp.rowSpec = GridLayout.spec(i);
                lp.columnSpec = GridLayout.spec(j);

                grid.addView(tv, lp);

                cell_tvs.add(tv);
            }
        }

        newGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);   // don't keep ticking after the screen is gone
    }

    // ---------- game setup ----------

    private void newGame() {
        isMine = new boolean[ROW_COUNT][COLUMN_COUNT];
        isRevealed = new boolean[ROW_COUNT][COLUMN_COUNT];
        isFlagged = new boolean[ROW_COUNT][COLUMN_COUNT];
        adjacent = new int[ROW_COUNT][COLUMN_COUNT];
        flagMode = false;
        gameOver = false;
        won = false;
        flagsLeft = MINE_COUNT;
        revealedCount = 0;

        placeMines();
        computeAdjacentCounts();

        modeButton.setText(R.string.pick);
        updateFlagCountText();
        startTimer();

        // TEMPORARY: print the board to Logcat for testing (filter by "BOARD").
        // Remove before submitting.
        for (int r = 0; r < ROW_COUNT; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < COLUMN_COUNT; c++) {
                row.append(isMine[r][c] ? "* " : adjacent[r][c] + " ");
            }
            Log.d("BOARD", row.toString());
        }
    }

    // shuffle all 100 positions and take the first 6, so mines never overlap
    private void placeMines() {
        ArrayList<Integer> positions = new ArrayList<>();
        for (int n = 0; n < ROW_COUNT * COLUMN_COUNT; n++) {
            positions.add(n);
        }
        Collections.shuffle(positions);
        for (int k = 0; k < MINE_COUNT; k++) {
            int n = positions.get(k);
            isMine[n / COLUMN_COUNT][n % COLUMN_COUNT] = true;
        }
    }

    // for every cell, count the mines among its 8 neighbours
    private void computeAdjacentCounts() {
        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c < COLUMN_COUNT; c++) {
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (inBounds(nr, nc) && isMine[nr][nc]) count++;
                    }
                }
                adjacent[r][c] = count;
            }
        }
    }

    // ---------- timer ----------

    private void startTimer() {
        clock = 0;
        running = true;
        updateTimerText();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 1000);
    }

    private void stopTimer() {
        running = false;
        handler.removeCallbacks(tick);
    }

    private void updateTimerText() {
        timerTv.setText(getString(R.string.clock) + " " + clock);
    }

    // ---------- click handling ----------

    // tap on the pick/flag icon below the grid
    public void onClickMode(View view) {
        if (gameOver) return;
        flagMode = !flagMode;
        modeButton.setText(flagMode ? R.string.flag : R.string.pick);
    }

    public void onClickTV(View view){
        TextView tv = (TextView) view;
        int n = findIndexOfCellTextView(tv);
        int r = n / COLUMN_COUNT;
        int c = n % COLUMN_COUNT;

        // after the game ends, the next tap on any cell goes to the result page
        if (gameOver) {
            goToResults();
            return;
        }
        if (isRevealed[r][c]) return;   // revealed cells can't be dug again or flagged

        if (flagMode) {
            toggleFlag(r, c);
        } else {
            if (isFlagged[r][c]) return;   // flagged cells can't be dug
            if (isMine[r][c]) {
                endGame(false);
                cellAt(r, c).setBackgroundColor(Color.RED);   // highlight the mine that was hit
            } else {
                reveal(r, c);
                if (revealedCount == SAFE_CELLS) {
                    endGame(true);
                }
            }
        }
    }

    // place or remove a flag; the counter is allowed to go negative
    private void toggleFlag(int r, int c) {
        TextView tv = cellAt(r, c);
        if (isFlagged[r][c]) {
            isFlagged[r][c] = false;
            tv.setText("");
            flagsLeft++;
        } else {
            isFlagged[r][c] = true;
            tv.setText(R.string.flag);
            flagsLeft--;
        }
        updateFlagCountText();
    }

    private void updateFlagCountText() {
        flagCountTv.setText(getString(R.string.flag) + " " + flagsLeft);
    }

    // reveal a cell; if it has no adjacent mines, transitively reveal its neighbours too
    private void reveal(int r, int c) {
        if (!inBounds(r, c)) return;
        if (isRevealed[r][c] || isFlagged[r][c] || isMine[r][c]) return;

        revealCell(r, c);

        if (adjacent[r][c] == 0) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    reveal(r + dr, c + dc);
                }
            }
        }
    }

    // turn a single cell gray and show its contents
    private void revealCell(int r, int c) {
        TextView tv = cellAt(r, c);
        isRevealed[r][c] = true;
        revealedCount++;
        tv.setBackgroundColor(REVEALED_COLOR);

        if (isMine[r][c]) {
            tv.setText(R.string.mine);
        } else if (adjacent[r][c] > 0) {
            tv.setText(String.valueOf(adjacent[r][c]));
            tv.setTextColor(NUMBER_COLOR);
        } else {
            tv.setText("");   // 0 adjacent mines: leave blank
        }
    }

    // ---------- end of game ----------

    // called once, on the move that wins or loses
    private void endGame(boolean didWin) {
        gameOver = true;
        won = didWin;
        stopTimer();
        revealAll();
    }

    // at the end of the game every cell is shown, including flagged ones
    private void revealAll() {
        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c < COLUMN_COUNT; c++) {
                if (!isRevealed[r][c]) {
                    revealCell(r, c);
                }
            }
        }
    }

    private void goToResults() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(EXTRA_SECONDS, clock);
        intent.putExtra(EXTRA_WON, won);
        startActivity(intent);
        finish();   // close this board so Play Again starts a fresh game
    }

    // ---------- helpers ----------

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROW_COUNT && c >= 0 && c < COLUMN_COUNT;
    }

    private TextView cellAt(int r, int c) {
        return cell_tvs.get(r * COLUMN_COUNT + c);
    }

    private int findIndexOfCellTextView(TextView tv) {
        for (int n=0; n<cell_tvs.size(); n++) {
            if (cell_tvs.get(n) == tv)
                return n;
        }
        return -1;
    }
}