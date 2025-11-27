package sky4s.garminhud.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
import java.util.List;
import android.content.Intent;

/**
 * Accessibility Service for Yandex Navigator/Maps - extracts navigation data
 * and sends to HUD
 */
public class YandexNaviReaderService extends AccessibilityService {

    private static final String TAG = "YandexNaviReader";

    // Real resource IDs from Yandex Maps UI dump
    private static final String ID_SPEED_LIMIT = "ru.yandex.yandexmaps:id/text_speedlimit";
    private static final String ID_MANEUVER_DISTANCE = "ru.yandex.yandexmaps:id/text_maneuverballoon_distance";
    private static final String ID_MANEUVER_METRICS = "ru.yandex.yandexmaps:id/text_maneuverballoon_metrics";
    private static final String ID_ETA_ARRIVAL = "ru.yandex.yandexmaps:id/textview_eta_arrival";
    private static final String ID_ETA_TIME = "ru.yandex.yandexmaps:id/textview_eta_time";
    private static final String ID_ETA_DISTANCE = "ru.yandex.yandexmaps:id/textview_eta_distance";
    private static final String ID_MANEUVER_ARROW = "ru.yandex.yandexmaps:id/image_maneuverballoon_maneuver";

    // Data cache
    private String lastSpeedLimit = null;
    private String lastDistance = null;
    private String lastEta = null;
    private String lastRemainTime = null;
    private String lastManeuverDesc = null;
    private long lastEventTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null)
            return;

        // Throttle - update max every 300ms
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEventTime < 300)
            return;
        lastEventTime = currentTime;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return;

        String pkg = root.getPackageName() != null ? root.getPackageName().toString() : "";
        if (!pkg.contains("yandex")) {
            root.recycle();
            return;
        }

        try {
            // Extract all data
            String speedLimit = extractText(root, ID_SPEED_LIMIT);
            String distance = extractManeuverDistance(root);
            String eta = extractText(root, ID_ETA_ARRIVAL);
            String remainTime = extractText(root, ID_ETA_TIME);
            String maneuverDesc = extractManeuverDescription(root);

            // Check if changed
            boolean changed = !eq(speedLimit, lastSpeedLimit) || !eq(distance, lastDistance) ||
                    !eq(eta, lastEta) || !eq(remainTime, lastRemainTime) ||
                    !eq(maneuverDesc, lastManeuverDesc);

            if (changed) {
                lastSpeedLimit = speedLimit;
                lastDistance = distance;
                lastEta = eta;
                lastRemainTime = remainTime;
                lastManeuverDesc = maneuverDesc;

                Log.d(TAG, String.format("Yandex -> Speed:%s, Dist:%s, ETA:%s, Remain:%s, Arrow:%s",
                        speedLimit, distance, eta, remainTime, maneuverDesc));

                broadcastData(speedLimit, distance, eta, remainTime, maneuverDesc);
            }
        } finally {
            root.recycle();
        }
    }

    private String extractManeuverDistance(AccessibilityNodeInfo root) {
        String dist = extractText(root, ID_MANEUVER_DISTANCE);
        String metric = extractText(root, ID_MANEUVER_METRICS);
        if (dist != null && metric != null) {
            return dist + metric;
        }
        return dist;
    }

    private String extractManeuverDescription(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo node = findById(root, ID_MANEUVER_ARROW);
        if (node != null) {
            CharSequence desc = node.getContentDescription();
            if (desc != null) {
                return parseArrowType(desc.toString());
            }
        }
        return null;
    }

    private String parseArrowType(String desc) {
        desc = desc.toLowerCase();
        if (desc.contains("left") || desc.contains("налево"))
            return "LEFT";
        if (desc.contains("right") || desc.contains("направо"))
            return "RIGHT";
        if (desc.contains("straight") || desc.contains("прямо"))
            return "STRAIGHT";
        if (desc.contains("uturn") || desc.contains("разворот"))
            return "UTURN";
        return "UNKNOWN:" + desc;
    }

    private String extractText(AccessibilityNodeInfo root, String resourceId) {
        AccessibilityNodeInfo node = findById(root, resourceId);
        if (node != null) {
            CharSequence text = node.getText();
            if (text != null) {
                return text.toString().trim();
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findById(AccessibilityNodeInfo root, String id) {
        if (root == null)
            return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
        return (nodes != null && !nodes.isEmpty()) ? nodes.get(0) : null;
    }

    private boolean eq(String a, String b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }

    private void broadcastData(String speedLimit, String distance, String eta, String remainTime, String maneuverDesc) {
        Intent intent = new Intent("sky4s.garminhud.app.YANDEX_NAVI_UPDATE");
        intent.putExtra("SPEED_LIMIT", speedLimit);
        intent.putExtra("DISTANCE", distance);
        intent.putExtra("ETA", eta);
        intent.putExtra("REMAIN_TIME", remainTime);
        intent.putExtra("MANEUVER_DESC", maneuverDesc);
        intent.putExtra("IS_NAVIGATING", distance != null || eta != null);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
}
