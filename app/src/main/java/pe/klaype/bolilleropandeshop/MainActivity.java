package pe.klaype.bolilleropandeshop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
    private static final int HITS_TO_WIN = 4;
    private static final String PREFS = "bolillero_game";

    private final SecureRandom random = new SecureRandom();
    private final List<Integer> history = new ArrayList<>();
    private final List<Integer> winnerOrder = new ArrayList<>();
    private final int[] counts = new int[101];

    private EditText fromInput;
    private EditText toInput;
    private TextView currentNumber;
    private TextView drawCounter;
    private TextView repeatedNumbers;
    private TextView winners;
    private TextView historyView;
    private BingoGrid grid;
    private int activeFrom = 1;
    private int activeTo = 100;

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
        titleBox.addView(label("Bingo con repetición · 4 aciertos", 14, GREEN, true));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        TextView fairness = label("Cada tirada es independiente y todos los números del rango tienen la misma probabilidad.", 13, MUTED, false);
        fairness.setPadding(0, dp(10), 0, dp(12));
        root.addView(fairness);

        LinearLayout settingsCard = card();
        settingsCard.addView(label("Rango del sorteo", 18, INK, true));
        LinearLayout range = row();
        range.setPadding(0, dp(8), 0, 0);
        fromInput = numberInput("Desde", "1");
        toInput = numberInput("Hasta", "100");
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(56), 1f);
        half.setMargins(0, 0, dp(6), 0);
        range.addView(fromInput, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(56), 1f);
        half2.setMargins(dp(6), 0, 0, 0);
        range.addView(toInput, half2);
        settingsCard.addView(range);
        root.addView(settingsCard, spaced());

        drawCounter = label("0 tiradas", 15, MUTED, true);
        drawCounter.setGravity(Gravity.CENTER);
        root.addView(drawCounter);

        LinearLayout stage = new LinearLayout(this);
        boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        stage.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        stage.setGravity(Gravity.CENTER);

        currentNumber = label("TOCA\nPARA SORTEAR", 27, Color.WHITE, true);
        currentNumber.setGravity(Gravity.CENTER);
        currentNumber.setContentDescription("Tocar para sortear una bolilla");
        currentNumber.setBackground(ballBackground());
        currentNumber.setElevation(dp(10));
        currentNumber.setOnClickListener(v -> drawBall());
        LinearLayout.LayoutParams ballParams = new LinearLayout.LayoutParams(dp(220), dp(220));
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
        Button undo = actionButton("Deshacer", false);
        undo.setOnClickListener(v -> undoLast());
        Button reset = actionButton("Nueva partida", true);
        reset.setOnClickListener(v -> confirmReset());
        LinearLayout.LayoutParams control = new LinearLayout.LayoutParams(0, dp(52), 1f);
        control.setMargins(0, 0, dp(5), 0);
        controls.addView(undo, control);
        LinearLayout.LayoutParams control2 = new LinearLayout.LayoutParams(0, dp(52), 1f);
        control2.setMargins(dp(5), 0, 0, 0);
        controls.addView(reset, control2);
        root.addView(controls);

        winners = section(root, "Ganadores", "Aún no hay ganadores.");
        historyView = section(root, "Historial de tiradas", "Aún no se ha sorteado ninguna bolilla.");

        TextView boardTitle = label("Tablero · cuatro bolitas por número", 19, INK, true);
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
        if (history.isEmpty()) {
            int[] range = readRange();
            if (range == null) return;
            activeFrom = range[0];
            activeTo = range[1];
            lockRange(true);
        }
        int value = activeFrom + random.nextInt(activeTo - activeFrom + 1);
        history.add(value);
        counts[value]++;
        if (counts[value] == HITS_TO_WIN) {
            winnerOrder.add(value);
            showWinner(value, winnerOrder.size());
        }
        currentNumber.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        saveGame();
        refreshUi();
    }

    private void undoLast() {
        if (history.isEmpty()) {
            Toast.makeText(this, "No hay tiradas para deshacer.", Toast.LENGTH_SHORT).show();
            return;
        }
        int value = history.remove(history.size() - 1);
        if (counts[value] == HITS_TO_WIN) winnerOrder.remove(Integer.valueOf(value));
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
        fromInput.setText("1");
        toInput.setText("100");
        lockRange(false);
        saveGame();
        refreshUi();
    }

    private void showWinner(int number, int place) {
        String title = place == 1 ? "¡Tenemos ganador!" : "¡Nuevo ganador!";
        String position = place == 1 ? "primer lugar" : "puesto " + place;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("El número " + number + " completó sus cuatro bolitas y obtiene el " + position + ".\n\nEl sorteo puede continuar para encontrar más ganadores.")
                .setPositiveButton("Continuar", null)
                .show();
    }

    private int[] readRange() {
        try {
            int from = Integer.parseInt(fromInput.getText().toString().trim());
            int to = Integer.parseInt(toInput.getText().toString().trim());
            if (from < 1 || to > 100 || from > to) throw new NumberFormatException();
            return new int[]{from, to};
        } catch (NumberFormatException error) {
            Toast.makeText(this, "Usa un rango válido entre 1 y 100.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void lockRange(boolean locked) {
        fromInput.setEnabled(!locked);
        toInput.setEnabled(!locked);
        fromInput.setAlpha(locked ? 0.65f : 1f);
        toInput.setAlpha(locked ? 0.65f : 1f);
    }

    private void refreshUi() {
        int last = history.isEmpty() ? -1 : history.get(history.size() - 1);
        currentNumber.setText(last < 0 ? "TOCA\nPARA SORTEAR" : String.valueOf(last));
        currentNumber.setTextSize(last < 0 ? 27 : 68);
        drawCounter.setText(history.size() + (history.size() == 1 ? " tirada" : " tiradas") + " · con repetición");

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
                history.isEmpty() ? safeInput(toInput, 100) : activeTo);
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
                .putString("history", values.toString())
                .apply();
    }

    private void loadGame() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        activeFrom = prefs.getInt("from", 1);
        activeTo = prefs.getInt("to", 100);
        fromInput.setText(String.valueOf(activeFrom));
        toInput.setText(String.valueOf(activeTo));
        String saved = prefs.getString("history", "");
        if (saved != null && !saved.isEmpty()) {
            for (String part : saved.split(",")) {
                try {
                    int value = Integer.parseInt(part);
                    if (value >= 1 && value <= 100) {
                        history.add(value);
                        counts[value]++;
                        if (counts[value] == HITS_TO_WIN) winnerOrder.add(value);
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

    private EditText numberInput(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value);
        input.setTextSize(18);
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

    private GradientDrawable ballBackground() {
        GradientDrawable ball = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(255, 127, 178), PINK, Color.rgb(155, 8, 69)});
        ball.setShape(GradientDrawable.OVAL);
        ball.setStroke(dp(5), Color.WHITE);
        return ball;
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
        private static final int COLUMNS = 5;

        BingoGrid(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setGameRange(int newFrom, int newTo) {
            from = Math.max(1, Math.min(100, newFrom));
            to = Math.max(from, Math.min(100, newTo));
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
                boolean won = counts[number] >= HITS_TO_WIN;
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

                float circleY = top + dp(49);
                float startX = left + dp(10);
                float spacing = Math.max(dp(13), (cellWidth - dp(20)) / 4f);
                for (int hit = 0; hit < HITS_TO_WIN; hit++) {
                    float cx = startX + hit * spacing;
                    paint.setColor(hit < Math.min(counts[number], HITS_TO_WIN) ? PINK : Color.WHITE);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx, circleY, dp(5), paint);
                    paint.setColor(PINK);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1.2f));
                    canvas.drawCircle(cx, circleY, dp(5), paint);
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
}