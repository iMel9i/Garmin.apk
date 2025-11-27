package sky4s.garminhud.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.eOutType;

/**
 * Yandex Data Handler - registers broadcast receiver and sends data to HUD
 * Call registerYandexReceiver() in MainActivity.onCreate() after all other
 * receivers
 */
public class YandexDataHandler {

    private static final String TAG = "YandexDataHandler";
    private MainActivity mainActivity;
    private BroadcastReceiver yandexReceiver;

    public YandexDataHandler(MainActivity activity) {
        this.mainActivity = activity;
    }

    public void registerReceiver() {
        yandexReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String speedLimit = intent.getStringExtra("SPEED_LIMIT");
                String distance = intent.getStringExtra("DISTANCE");
                String eta = intent.getStringExtra("ETA");
                String remainTime = intent.getStringExtra("REMAIN_TIME");
                String maneuverDesc = intent.getStringExtra("MANEUVER_DESC");
                boolean isNavigating = intent.getBooleanExtra("IS_NAVIGATING", false);

                // Update navigation status
                mainActivity.mIsNavigating = isNavigating;

                if (mainActivity.mHud != null && isNavigating) {
                    // Parse distance
                    if (distance != null) {
                        try {
                            String[] parts = distance.split("\\s+");
                            if (parts.length >= 2) {
                                float distValue = Float.parseFloat(parts[0].replace(",", "."));
                                String unit = parts[1].toLowerCase();

                                eUnits hudUnit = unit.contains("км") || unit.equals("km") ? eUnits.Kilometres
                                        : eUnits.Metres;

                                mainActivity.mHud.setDistance(distValue, hudUnit);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing distance: " + e.getMessage());
                        }
                    }

                    // Parse remaining time
                    if (remainTime != null) {
                        try {
                            String numStr = remainTime.replaceAll("[^0-9]", "");
                            if (!numStr.isEmpty()) {
                                int minutes = Integer.parseInt(numStr);
                                int hours = minutes / 60;
                                int mins = minutes % 60;
                                mainActivity.mHud.setRemainTime(hours, mins, false);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing remain time: " + e.getMessage());
                        }
                    }

                    // Parse Speed Limit
                    if (speedLimit != null) {
                        try {
                            int limit = Integer.parseInt(speedLimit.replaceAll("[^0-9]", ""));
                            // Assuming current speed is not available here, passing 0 as current speed
                            // and false for speeding warning.
                            // If we want to show just the limit sign, setSpeedWarning might be used.
                            mainActivity.mHud.setSpeedWarning(0, limit, false, true, true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing speed limit: " + e.getMessage());
                        }
                    }

                    // Parse maneuver arrow
                    if (maneuverDesc != null) {
                        try {
                            updateHudDirection(maneuverDesc);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing maneuver: " + e.getMessage());
                        }
                    }
                }
            }

            private void updateHudDirection(String desc) {
                desc = desc.toUpperCase();
                sky4s.garminhud.eOutAngle angle = sky4s.garminhud.eOutAngle.Straight;
                sky4s.garminhud.eOutType type = sky4s.garminhud.eOutType.Lane;

                if (desc.contains("LEFT")) {
                    angle = sky4s.garminhud.eOutAngle.Left;
                    if (desc.contains("SHARP"))
                        angle = sky4s.garminhud.eOutAngle.SharpLeft;
                    if (desc.contains("EASY") || desc.contains("SLIGHT"))
                        angle = sky4s.garminhud.eOutAngle.EasyLeft;
                } else if (desc.contains("RIGHT")) {
                    angle = sky4s.garminhud.eOutAngle.Right;
                    if (desc.contains("SHARP"))
                        angle = sky4s.garminhud.eOutAngle.SharpRight;
                    if (desc.contains("EASY") || desc.contains("SLIGHT"))
                        angle = sky4s.garminhud.eOutAngle.EasyRight;
                } else if (desc.contains("UTURN")) {
                    angle = sky4s.garminhud.eOutAngle.LeftDown; // Standard U-Turn
                }

                mainActivity.mHud.setDirection(angle, type, sky4s.garminhud.eOutAngle.Straight);
            }
        };

        IntentFilter filter = new IntentFilter("sky4s.garminhud.app.YANDEX_NAVI_UPDATE");
        mainActivity.registerReceiver(yandexReceiver, filter);
    }

    public void unregisterReceiver() {
        if (yandexReceiver != null) {
            mainActivity.unregisterReceiver(yandexReceiver);
        }
    }
}
