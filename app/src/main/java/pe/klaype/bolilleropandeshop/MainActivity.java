package pe.klaype.bolilleropandeshop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends android.app.Activity {
    private static final int PINK = Color.rgb(236, 47, 131);
    private static final int GREEN = Color.rgb(87, 153, 43);
    private static final int INK = Color.rgb(35, 28, 33);
    private static final int MUTED = Color.rgb(108, 91, 101);
    private static final int CREAM = Color.rgb(255, 248, 245);
    private static final String PREFS = "bolillero_game";

    private final SecureRandom random = new SecureRandom();
    private final List<Integer> history = new ArrayList<>();
    private final List<Integer> winnerOrder = new ArrayList<>();
    private final int[] counts = new int[101];

    private EditText fromInput;
    private EditText toInput;
    private EditText hitsInput;
    private Switch allowWinnerRepeatSwitch;
    private LotteryBall currentNumber;
    private TextView drawCounter;
    private TextView repeatedNumbers;
    private TextView winners;
    private TextView historyView;
    private TextView boardTitle;
    private BingoGrid grid;
    private Button undoButton;
    private Button resetButton;
    private ValueAnimator drawAnimator;
    private boolean drawCancelled;
    private int activeFrom = 1;
    private int activeTo = 100;
    private int activeHits = 4;
    private boolean activeAllowWinnerRepeat = true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        loadGame();
        refreshUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(14), dp(16), dp(34));
        root.setBackgroundColor(CREAM);
        scroll.addView(root);

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_pandeshop);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setBackgroundResource(R.drawable.logo_frame);
        logo.setClipToOutline(true);
        header.addView(logo, new LinearLayout.LayoutParams(dp(74), dp(74)));
        LinearLayout titleBox = column();
        titleBox.setPadding(dp(12), 0, 0, 0);
        titleBox.addView(label("Bolillero PandeShop", 24, PINK, true));
        titleBox.addView(label("Bingo con repetición · aciertos configurables", 14, GREEN, true));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        TextView fairness = label("Cada tirada es independiente y todos los números del rango tienen la misma probabilidad.", 13, MUTED, false);
        fairness.setPadding(0, dp(10), 0, dp(12));
        root.addView(fairness);

        LinearLayout settingsCard = card();
        settingsCard.addView(label("Rango y aciertos para ganar", 18, INK, true));
        LinearLayout range = row();
        range.setPadding(0, dp(8), 0, 0);
        fromInput = numberInput("1");
        toInput = numberInput("100");
        hitsInput = numberInput("4");
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(56), 1f);
        half.setMargins(0, 0, dp(4), 0);
        range.addView(numberField("Desde", fromInput), half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(56), 1f);
        half2.setMargins(dp(4), 0, dp(4), 0);
        range.addView(numberField("Hasta", toInput), half2);
        LinearLayout.LayoutParams third = new LinearLayout.LayoutParams(0, dp(56), 1f);
        third.setMargins(dp(4), 0, 0, 0);
        range.addView(numberField("Aciertos", hitsInput), third);
        settingsCard.addView(range);

        allowWinnerRepeatSwitch = new Switch(this);
        allowWinnerRepeatSwitch.setText("Permitir que un número ganador vuelva a salir");
        allowWinnerRepeatSwitch.setTextColor(INK);
        allowWinnerRepeatSwitch.setTextSize(14);
        allowWinnerRepeatSwitch.setChecked(true);
        allowWinnerRepeatSwitch.setPadding(0, dp(12), 0, 0);
        settingsCard.addView(allowWinnerRepeatSwitch);
        TextView repeatHelp = label("Desactívalo para retirar un número del bolillero cuando complete sus aciertos.", 12, MUTED, false);
        repeatHelp.setPadding(0, dp(3), 0, 0);
        settingsCard.addView(repeatHelp);
        root.addView(settingsCard, spaced());

        drawCounter = label("0 tiradas", 15, MUTED, true);
        drawCounter.setGravity(Gravity.CENTER);
        root.addView(drawCounter);

        LinearLayout stage = new LinearLayout(this);
        boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        stage.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        stage.setGravity(Gravity.CENTER);

        currentNumber = new LotteryBall(this);
        currentNumber.setDisplayText("TOCA\nPARA SORTEAR", -1);
        currentNumber.setContentDescription("Tocar para sortear una bolilla");
        currentNumber.setElevation(dp(10));
        currentNumber.setOnClickListener(v -> drawBall());
        LinearLayout.LayoutParams ballParams = new LinearLayout.LayoutParams(dp(232), dp(232));
        ballParams.gravity = Gravity.CENTER_HORIZONTAL;
        ballParams.setMargins(0, dp(10), 0, dp(14));
        stage.addView(currentNumber, ballParams);

        LinearLayout repeatedCard = card();
        repeatedCard.addView(label("Números repetidos", 17, INK, true));
        repeatedNumbers = label("Aún no hay repeticiones.", 15, MUTED, false);
        repeatedNumbers.setPadding(0, dp(6), 0, 0);
        repeatedCard.addView(repeatedNumbers);
        LinearLayout.LayoutParams repeatedParams = landscape
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        repeatedParams.setMargins(landscape ? dp(14) : 0, landscape ? 0 : dp(4), 0, dp(10));
        stage.addView(repeatedCard, repeatedParams);
        root.addView(stage);

        LinearLayout controls = row();
        undoButton = actionButton("Deshacer", false);
        undoButton.setOnClickListener(v -> undoLast());
        resetButton = actionButton("Nueva partida", true);
        resetButton.setOnClickListener(v -> confirmReset());
        LinearLayout.LayoutParams control = new LinearLayout.LayoutParams(0, dp(52), 1f);
        control.setMargins(0, 0, dp(5), 0);
        controls.addView(undoButton, control);
        LinearLayout.LayoutParams control2 = new LinearLayout.LayoutParams(0, dp(52), 1f);
        control2.setMargins(dp(5), 0, 0, 0);
        controls.addView(resetButton, control2);
        root.addView(controls);

        winners = section(root, "Ganadores", "Aún no hay ganadores.");
        historyView = section(root, "Historial de tiradas", "Aún no se ha sorteado ninguna bolilla.");

        boardTitle = label("Tablero · 4 aciertos por número", 19, INK, true);
        boardTitle.setPadding(0, dp(18), 0, dp(8));
        root.addView(boardTitle);
        grid = new BingoGrid(this);
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView footer = label("PandeShop · Estilo que te acompaña", 13, PINK, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(20), 0, 0);
        root.addView(footer);
        setContentView(scroll);
    }

    private void drawBall() {
        if (drawAnimator != null && drawAnimator.isRunning()) return;
        if (history.isEmpty()) {
            int[] settings = readSettings();
            if (settings == null) return;
            activeFrom = settings[0];
            activeTo = settings[1];
            activeHits = settings[2];
            activeAllowWinnerRepeat = allowWinnerRepeatSwitch.isChecked();
            lockRange(true);
        }
        final int value = randomEligibleNumber();
        if (value < 0) {
            Toast.makeText(this, "Todos los números del rango ya completaron sus aciertos.", Toast.LENGTH_LONG).show();
            return;
        }
        animateDraw(value);
    }

    private int randomEligibleNumber() {
        if (activeAllowWinnerRepeat) return activeFrom + random.nextInt(activeTo - activeFrom + 1);
        int eligible = 0;
        for (int number = activeFrom; number <= activeTo; number++) {
            if (counts[number] < activeHits) eligible++;
        }
        if (eligible == 0) return -1;
        int selected = random.nextInt(eligible);
        for (int number = activeFrom; number <= activeTo; number++) {
            if (counts[number] < activeHits && selected-- == 0) return number;
        }
        return -1;
    }

    private void animateDraw(final int finalValue) {
        drawCancelled = false;
        setDrawingState(true);
        drawCounter.setText("Sorteando…");
        final long[] lastChange = {0L};
        drawAnimator = ValueAnimator.ofFloat(0f, 1f);
        drawAnimator.setDuration(1500L);
        drawAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            long now = SystemClock.uptimeMillis();
            if (now - lastChange[0] >= 75L && progress < 0.94f) {
                int preview = randomEligibleNumber();
                if (preview < 0) preview = finalValue;
                currentNumber.setDisplayText(String.valueOf(preview), preview);
                lastChange[0] = now;
            }
            float wobble = (float) Math.sin(progress * Math.PI * 12f);
            float pulse = 1f + 0.055f * Math.abs(wobble);
            currentNumber.setScaleX(pulse);
            currentNumber.setScaleY(pulse);
            currentNumber.setRotation(wobble * 8f);
            currentNumber.setTranslationX(wobble * dp(7) * (1f - progress * 0.45f));
        });
        drawAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator animation) {
                drawCancelled = true;
            }

            @Override public void onAnimationEnd(Animator animation) {
                currentNumber.setScaleX(1f);
                currentNumber.setScaleY(1f);
                currentNumber.setRotation(0f);
                currentNumber.setTranslationX(0f);
                if (!drawCancelled) commitDraw(finalValue);
                setDrawingState(false);
            }
        });
        drawAnimator.start();
    }

    private void commitDraw(int value) {
        history.add(value);
        counts[value]++;
        if (counts[value] == activeHits) {
            winnerOrder.add(value);
            showWinner(value, winnerOrder.size());
        }
        currentNumber.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        saveGame();
        refreshUi();
    }

    private void undoLast() {
        if (drawAnimator != null && drawAnimator.isRunning()) return;
        if (history.isEmpty()) {
            Toast.makeText(this, "No hay tiradas para deshacer.", Toast.LENGTH_SHORT).show();
            return;
        }
        int value = history.remove(history.size() - 1);
        if (counts[value] == activeHits) winnerOrder.remove(Integer.valueOf(value));
        counts[value]--;
        if (history.isEmpty()) lockRange(false);
        saveGame();
        refreshUi();
        Toast.makeText(this, "Se deshizo la bolilla " + value, Toast.LENGTH_SHORT).show();
    }

    private void confirmReset() {
        if (history.isEmpty()) {
            resetGame();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("¿Iniciar una nueva partida?")
                .setMessage("Se borrarán las tiradas, repeticiones y ganadores actuales.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Reiniciar", (dialog, which) -> resetGame())
                .show();
    }

    private void resetGame() {
        history.clear();
        winnerOrder.clear();
        for (int i = 0; i < counts.length; i++) counts[i] = 0;
        activeFrom = 1;
        activeTo = 100;
        activeHits = 4;
        activeAllowWinnerRepeat = true;
        fromInput.setText("1");
        toInput.setText("100");
        hitsInput.setText("4");
        allowWinnerRepeatSwitch.setChecked(true);
        lockRange(false);
        saveGame();
        refreshUi();
    }

    private void showWinner(int number, int place) {
        String title = place == 1 ? "¡Tenemos ganador!" : "¡Nuevo ganador!";
        String position = place == 1 ? "primer lugar" : "puesto " + place;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("El número " + number + " completó " + activeHits + " aciertos y obtiene el " + position + ".\n\nEl sorteo puede continuar para encontrar más ganadores.")
                .setPositiveButton("Continuar", null)
                .show();
    }

    private int[] readSettings() {
        try {
            int from = Integer.parseInt(fromInput.getText().toString().trim());
            int to = Integer.parseInt(toInput.getText().toString().trim());
            int hits = Integer.parseInt(hitsInput.getText().toString().trim());
            if (from < 1 || to > 100 || from > to) throw new NumberFormatException();
            if (hits < 1 || hits > 20) throw new NumberFormatException();
            return new int[]{from, to, hits};
        } catch (NumberFormatException error) {
            Toast.makeText(this, "Usa un rango entre 1 y 100 y entre 1 y 20 aciertos.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void lockRange(boolean locked) {
        fromInput.setEnabled(!locked);
        toInput.setEnabled(!locked);
        hitsInput.setEnabled(!locked);
        allowWinnerRepeatSwitch.setEnabled(!locked);
        fromInput.setAlpha(locked ? 0.65f : 1f);
        toInput.setAlpha(locked ? 0.65f : 1f);
        hitsInput.setAlpha(locked ? 0.65f : 1f);
        allowWinnerRepeatSwitch.setAlpha(locked ? 0.65f : 1f);
    }

    private void setDrawingState(boolean drawing) {
        currentNumber.setEnabled(!drawing);
        undoButton.setEnabled(!drawing);
        resetButton.setEnabled(!drawing);
        undoButton.setAlpha(drawing ? 0.45f : 1f);
        resetButton.setAlpha(drawing ? 0.45f : 1f);
        if (drawing) {
            fromInput.setEnabled(false);
            toInput.setEnabled(false);
            hitsInput.setEnabled(false);
            allowWinnerRepeatSwitch.setEnabled(false);
        } else {
            lockRange(!history.isEmpty());
        }
    }

    private void refreshUi() {
        int last = history.isEmpty() ? -1 : history.get(history.size() - 1);
        currentNumber.setDisplayText(last < 0 ? "TOCA\nPARA SORTEAR" : String.valueOf(last), last);
        drawCounter.setText(history.size() + (history.size() == 1 ? " tirada" : " tiradas") + " · con repetición");
        int displayedHits = history.isEmpty() ? safeInput(hitsInput, 4) : activeHits;
        boardTitle.setText("Tablero · " + displayedHits + (displayedHits == 1 ? " acierto" : " aciertos") + " por número");

        if (winnerOrder.isEmpty()) {
            winners.setText("Aún no hay ganadores.");
        } else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < winnerOrder.size(); i++) {
                if (i > 0) text.append("  ·  ");
                text.append(i + 1).append(".º: Nº ").append(winnerOrder.get(i));
            }
            winners.setText(text.toString());
        }

        StringBuilder repeats = new StringBuilder();
        for (int n = activeFrom; n <= activeTo; n++) {
            if (counts[n] >= 2) {
                if (repeats.length() > 0) repeats.append("   ");
                repeats.append(n).append(" × ").append(counts[n]);
            }
        }
        repeatedNumbers.setText(repeats.length() == 0 ? "Aún no hay repeticiones." : repeats.toString());

        StringBuilder recent = new StringBuilder();
        int start = Math.max(0, history.size() - 40);
        for (int i = history.size() - 1; i >= start; i--) {
            if (recent.length() > 0) recent.append("  ·  ");
            recent.append(history.get(i));
        }
        historyView.setText(recent.length() == 0 ? "Aún no se ha sorteado ninguna bolilla." : recent.toString());
        grid.setGameRange(history.isEmpty() ? safeInput(fromInput, 1) : activeFrom,
                history.isEmpty() ? safeInput(toInput, 100) : activeTo,
                displayedHits);
        grid.invalidate();
    }

    private int safeInput(EditText input, int fallback) {
        try {
            int value = Integer.parseInt(input.getText().toString().trim());
            return Math.max(1, Math.min(100, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void saveGame() {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) values.append(',');
            values.append(history.get(i));
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt("from", activeFrom)
                .putInt("to", activeTo)
                .putInt("hits", activeHits)
                .putBoolean("allow_winner_repeat", activeAllowWinnerRepeat)
                .putString("history", values.toString())
                .apply();
    }

    private void loadGame() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        activeFrom = prefs.getInt("from", 1);
        activeTo = prefs.getInt("to", 100);
        activeHits = Math.max(1, Math.min(20, prefs.getInt("hits", 4)));
        activeAllowWinnerRepeat = prefs.getBoolean("allow_winner_repeat", true);
        fromInput.setText(String.valueOf(activeFrom));
        toInput.setText(String.valueOf(activeTo));
        hitsInput.setText(String.valueOf(activeHits));
        allowWinnerRepeatSwitch.setChecked(activeAllowWinnerRepeat);
        String saved = prefs.getString("history", "");
        if (saved != null && !saved.isEmpty()) {
            for (String part : saved.split(",")) {
                try {
                    int value = Integer.parseInt(part);
                    if (value >= 1 && value <= 100) {
                        history.add(value);
                        counts[value]++;
                        if (counts[value] == activeHits) winnerOrder.add(value);
                    }
                } catch (NumberFormatException ignored) { }
            }
        }
        lockRange(!history.isEmpty());
    }

    private TextView section(LinearLayout root, String title, String empty) {
        LinearLayout box = card();
        TextView heading = label(title, 17, INK, true);
        TextView body = label(empty, 15, MUTED, false);
        body.setPadding(0, dp(6), 0, 0);
        box.addView(heading);
        box.addView(body);
        root.addView(box, spaced());
        return body;
    }

    private LinearLayout card() {
        LinearLayout box = column();
        box.setPadding(dp(15), dp(14), dp(15), dp(14));
        box.setBackground(rounded(Color.WHITE, 18, Color.rgb(248, 193, 215), 1));
        box.setElevation(dp(2));
        return box;
    }

    private LinearLayout numberField(String title, EditText input) {
        LinearLayout field = column();
        TextView fieldTitle = label(title, 12, MUTED, true);
        fieldTitle.setGravity(Gravity.CENTER);
        field.addView(fieldTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        field.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
        return field;
    }

    private EditText numberInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setTextSize(17);
        input.setTextColor(INK);
        input.setHintTextColor(Color.rgb(160, 140, 151));
        input.setGravity(Gravity.CENTER);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        input.setBackground(rounded(Color.rgb(255, 250, 252), 14, PINK, 2));
        return input;
    }

    private Button actionButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(primary ? Color.WHITE : PINK);
        button.setBackground(rounded(primary ? PINK : Color.WHITE, 16, PINK, 2));
        return button;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        shape.setStroke(dp(strokeDp), strokeColor);
        return shape;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout.LayoutParams spaced() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, 0);
        return params;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class BingoGrid extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int from = 1;
        private int to = 100;
        private int hitsToWin = 4;
        private static final int COLUMNS = 5;

        BingoGrid(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setGameRange(int newFrom, int newTo, int newHitsToWin) {
            from = Math.max(1, Math.min(100, newFrom));
            to = Math.max(from, Math.min(100, newTo));
            hitsToWin = Math.max(1, Math.min(20, newHitsToWin));
            requestLayout();
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int rows = (int) Math.ceil((to - from + 1) / (float) COLUMNS);
            setMeasuredDimension(width, Math.max(dp(80), rows * dp(76)));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int total = to - from + 1;
            float gap = dp(5);
            float cellWidth = (getWidth() - gap * (COLUMNS - 1)) / COLUMNS;
            float cellHeight = dp(70);
            for (int index = 0; index < total; index++) {
                int number = from + index;
                int col = index % COLUMNS;
                int row = index / COLUMNS;
                float left = col * (cellWidth + gap);
                float top = row * dp(76);
                RectF cell = new RectF(left, top, left + cellWidth, top + cellHeight);
                boolean won = counts[number] >= hitsToWin;
                paint.setColor(won ? Color.rgb(226, 246, 210) : Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(cell, dp(11), dp(11), paint);
                paint.setColor(won ? GREEN : (number % 2 == 0 ? GREEN : PINK));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(won ? 3 : 1));
                canvas.drawRoundRect(cell, dp(11), dp(11), paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(dp(17));
                paint.setColor(won ? GREEN : INK);
                canvas.drawText(String.valueOf(number), left + dp(8), top + dp(22), paint);

                if (hitsToWin <= 8) {
                    float circleY = top + dp(49);
                    float available = cellWidth - dp(14);
                    float spacing = available / hitsToWin;
                    float radius = Math.min(dp(4.6f), spacing * 0.31f);
                    float startX = left + dp(7) + spacing / 2f;
                    for (int hit = 0; hit < hitsToWin; hit++) {
                        float cx = startX + hit * spacing;
                        paint.setColor(hit < Math.min(counts[number], hitsToWin) ? PINK : Color.WHITE);
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(cx, circleY, radius, paint);
                        paint.setColor(PINK);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(dp(1.1f));
                        canvas.drawCircle(cx, circleY, radius, paint);
                    }
                } else {
                    float barLeft = left + dp(8);
                    float barTop = top + dp(43);
                    float barWidth = cellWidth - dp(16);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(245, 218, 229));
                    canvas.drawRoundRect(new RectF(barLeft, barTop, barLeft + barWidth, barTop + dp(8)), dp(4), dp(4), paint);
                    paint.setColor(PINK);
                    float ratio = Math.min(counts[number], hitsToWin) / (float) hitsToWin;
                    canvas.drawRoundRect(new RectF(barLeft, barTop, barLeft + barWidth * ratio, barTop + dp(8)), dp(4), dp(4), paint);
                    paint.setColor(MUTED);
                    paint.setTextSize(dp(9));
                    canvas.drawText(Math.min(counts[number], hitsToWin) + "/" + hitsToWin, barLeft, top + dp(64), paint);
                }
                if (won) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(GREEN);
                    paint.setTextSize(dp(9));
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    canvas.drawText("GANÓ", left + cellWidth - dp(34), top + dp(20), paint);
                }
            }
        }
    }

    private final class LotteryBall extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String display = "TOCA\nPARA SORTEAR";
        private int paletteIndex;

        LotteryBall(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setDisplayText(String value, int number) {
            display = value;
            paletteIndex = number < 0 ? 0 : Math.abs(number) % 4;
            setContentDescription(number < 0 ? "Tocar para sortear una bolilla" : "Bolilla número " + number);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = size * 0.45f;
            int[][] palettes = {
                    {Color.rgb(179, 238, 52), Color.rgb(78, 164, 19), Color.rgb(26, 91, 9)},
                    {Color.rgb(68, 222, 255), Color.rgb(20, 121, 220), Color.rgb(22, 40, 139)},
                    {Color.rgb(255, 122, 177), Color.rgb(232, 35, 119), Color.rgb(127, 5, 56)},
                    {Color.rgb(255, 207, 66), Color.rgb(246, 126, 24), Color.rgb(150, 51, 10)}
            };
            int[] colors = palettes[paletteIndex];

            paint.clearShadowLayer();
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(105, 17, 12, 28));
            paint.setShadowLayer(dp(13), 0, dp(9), Color.argb(125, 0, 0, 0));
            canvas.drawCircle(cx, cy + dp(3), radius, paint);
            paint.clearShadowLayer();

            paint.setShader(new RadialGradient(cx - radius * 0.34f, cy - radius * 0.38f,
                    radius * 1.45f, colors, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(4));
            paint.setColor(Color.argb(220, 255, 255, 255));
            RectF outer = new RectF(cx - radius * 0.91f, cy - radius * 0.91f, cx + radius * 0.91f, cy + radius * 0.91f);
            canvas.drawArc(outer, 205, 62, false, paint);
            canvas.drawArc(outer, 25, 62, false, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(115, 255, 255, 255));
            canvas.drawOval(new RectF(cx - radius * 0.53f, cy - radius * 0.75f,
                    cx - radius * 0.08f, cy - radius * 0.48f), paint);

            float badgeRadius = radius * 0.48f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[2]);
            canvas.drawCircle(cx, cy, badgeRadius + dp(6), paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, badgeRadius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(colors[1]);
            canvas.drawCircle(cx, cy, badgeRadius - dp(4), paint);

            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            if (display.contains("\n")) {
                String[] lines = display.split("\\n");
                paint.setTextSize(dp(14));
                Paint.FontMetrics fm = paint.getFontMetrics();
                float lineHeight = fm.descent - fm.ascent;
                float firstY = cy - lineHeight * (lines.length - 1) / 2f - (fm.ascent + fm.descent) / 2f;
                for (int i = 0; i < lines.length; i++) canvas.drawText(lines[i], cx, firstY + i * lineHeight, paint);
            } else {
                paint.setTextSize(display.length() >= 3 ? dp(44) : dp(58));
                Paint.FontMetrics fm = paint.getFontMetrics();
                float baseline = cy - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(display, cx, baseline, paint);
            }
        }
    }

    @Override protected void onStop() {
        if (drawAnimator != null && drawAnimator.isRunning()) {
            drawCancelled = true;
            drawAnimator.cancel();
            refreshUi();
        }
        super.onStop();
    }
}