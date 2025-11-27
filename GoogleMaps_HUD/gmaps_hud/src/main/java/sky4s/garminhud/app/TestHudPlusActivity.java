package sky4s.garminhud.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;
import java.util.ArrayList;

import sky4s.garminhud.hud.HUDInterface;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public class TestHudPlusActivity extends Activity {

    private final ArrayList<HudCommand> commands = new ArrayList<>();
    private HUDInterface mHud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get HUD instance
        mHud = NotificationMonitor.sHud;

        initCommands();

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setPadding(30, 30, 30, 30);
        grid.setBackgroundColor(0xFF111111);

        for (HudCommand cmd : commands) {
            Button btn = new Button(this);
            btn.setText(String.format("%s\n%s", cmd.codeStr, cmd.name));
            btn.setTextColor(0xFF00FF00);
            btn.setBackgroundColor(0xFF222222);
            btn.setTextSize(13);
            btn.setPadding(10, 30, 10, 30);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(15, 15, 15, 15);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> executeCommand(cmd));
            grid.addView(btn);
        }

        // Кнопка «Тест всех подряд»
        Button btnTestAll = new Button(this);
        btnTestAll.setText("ТЕСТ ВСЕХ\nпо 1 сек");
        btnTestAll.setTextColor(0xFFFFFFFF);
        btnTestAll.setBackgroundColor(0xFFCC0000);
        btnTestAll.setTextSize(16);
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.columnSpec = GridLayout.spec(0, 3);
        p.setMargins(20, 40, 20, 40);
        btnTestAll.setLayoutParams(p);
        btnTestAll.setOnClickListener(v -> testAllSequentially());
        grid.addView(btnTestAll);

        setContentView(grid);
        setTitle("Garmin HUD+ — Тест команд");
    }

    private void initCommands() {
        // Стрелки
        commands.add(new HudCommand("0x01", "Прямо", () -> mHud.setDirection(eOutAngle.Straight)));
        commands.add(new HudCommand("0x02", "Направо", () -> mHud.setDirection(eOutAngle.Right)));
        commands.add(new HudCommand("0x03", "Налево", () -> mHud.setDirection(eOutAngle.Left)));
        commands.add(new HudCommand("0x04", "Плавно направо", () -> mHud.setDirection(eOutAngle.EasyRight)));
        commands.add(new HudCommand("0x05", "Плавно налево", () -> mHud.setDirection(eOutAngle.EasyLeft)));
        commands.add(new HudCommand("0x06", "Круто направо", () -> mHud.setDirection(eOutAngle.SharpRight)));
        commands.add(new HudCommand("0x07", "Круто налево", () -> mHud.setDirection(eOutAngle.SharpLeft)));
        commands.add(new HudCommand("0x08", "Разворот", () -> mHud.setDirection(eOutAngle.LeftDown)));

        // Дополнительные
        commands.add(new HudCommand("0x10", "Камера", () -> mHud.setCameraIcon(true)));
        commands.add(new HudCommand("0x1A", "Пробка", () -> mHud.setRemainTime(0, 0, true)));

        // Очистка
        commands.add(new HudCommand("0x40", "ОЧИСТИТЬ", () -> {
            mHud.clearDistance();
            mHud.clearTime();
            mHud.clearSpeedAndWarning();
            mHud.setCameraIcon(false);
            mHud.setGpsLabel(false);
        }));

        // Тест скорости
        commands.add(new HudCommand("0x60", "Скорость 88", () -> mHud.setSpeed(88, true)));

        // Новые тестовые элементы
        commands.add(new HudCommand("0x61", "Ограничение 60", () -> mHud.setSpeedWarning(0, 60, false, true, true)));
        commands.add(new HudCommand("0x62", "Дистанция 150м", () -> mHud.setDistance(150, eUnits.Metres)));
        commands.add(new HudCommand("0x63", "Оставшееся время 5 мин", () -> mHud.setRemainTime(0, 5, false)));
        commands.add(new HudCommand("0x64", "GPS включен", () -> mHud.setGpsLabel(true)));
    }

    private void executeCommand(HudCommand cmd) {
        if (mHud == null) {
            Toast.makeText(this, "HUD not initialized!", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                if (cmd.action != null) {
                    cmd.action.run();
                    runOnUiThread(() -> Toast.makeText(TestHudPlusActivity.this,
                            "Выполнено: " + cmd.name, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(TestHudPlusActivity.this,
                            "Команда не реализована: " + cmd.name, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(TestHudPlusActivity.this,
                        "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void testAllSequentially() {
        new Thread(() -> {
            for (HudCommand cmd : commands) {
                if (!cmd.name.contains("ОЧИСТИТЬ")) {
                    executeCommand(cmd);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            // Финальная очистка
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            if (mHud != null) {
                mHud.clearDistance();
                mHud.clearTime();
                mHud.clearSpeedAndWarning();
            }
            runOnUiThread(() -> Toast.makeText(this, "Автотест завершён", Toast.LENGTH_LONG).show());
        }).start();
    }

    private static class HudCommand {
        final String codeStr;
        final String name;
        final Runnable action;

        HudCommand(String codeStr, String name, Runnable action) {
            this.codeStr = codeStr;
            this.name = name;
            this.action = action;
        }
    }
}
