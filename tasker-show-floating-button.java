import android.view.WindowManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.content.Intent;
import android.widget.ImageView;

// ====== config ======
final String OVERLAY_ID      = "bubble-1";                   // string id used to close
final String TAP_BROADCAST   = "com.example.MY_BUBBLE";      // short tap intent
final String LONG_BROADCAST  = "com.example.MY_BUBBLE_LONG"; // long press intent
final int    DIAMETER_PX     = 108;                          // round size (~1/3 smaller)
final String ICON_PATH       = null;                         // e.g. "/sdcard/Download/icon.png" or null
final String PREF_NAME       = "bsh_overlay";
final String KEY_CLOSE       = "close:" + OVERLAY_ID;
final int    POLL_MS         = 300;                          // close flag polling interval
final boolean EXIT_ON_CLOSE  = false;                        // set true (not recommended) if you must exit
// =====================

final android.content.Context appctx = context.getApplicationContext();
final WindowManager wm = (WindowManager) appctx.getSystemService("window");
final android.content.SharedPreferences prefs =
    appctx.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);

// Run on the main (UI) thread
new Handler(appctx.getMainLooper()).post(new Runnable() {
  public void run() {
    try {
      final ImageButton btn = new ImageButton(appctx);

      // round background
      GradientDrawable bg = new GradientDrawable();
      bg.setShape(GradientDrawable.OVAL);
      bg.setColor(0xFF448AFF);
      btn.setBackground(bg);

      // optional PNG icon
      if (ICON_PATH != null) {
        try {
          android.graphics.Bitmap bmp = BitmapFactory.decodeFile(ICON_PATH);
          if (bmp != null) {
            btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            btn.setImageDrawable(new BitmapDrawable(appctx.getResources(), bmp));
            int pad = Math.max(8, DIAMETER_PX / 8);
            btn.setPadding(pad, pad, pad, pad);
          }
        } catch (Throwable ignored) {}
      } else {
        btn.setImageDrawable(null);
        btn.setPadding(0,0,0,0);
      }

      final int type = (Build.VERSION.SDK_INT >= 26)
        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        : WindowManager.LayoutParams.TYPE_PHONE;

      final int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                      | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

      final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
          DIAMETER_PX, DIAMETER_PX, type, flags, PixelFormat.TRANSLUCENT);
      lp.gravity = Gravity.TOP | Gravity.START;
      lp.x = 48;  lp.y = 200;

      // --- Drag + tap + long-press without GestureDetector ---
      final int touchSlop = android.view.ViewConfiguration.get(appctx).getScaledTouchSlop();
      final float[] down = new float[2];
      final int[] start = new int[2];
      final Handler h = new Handler();
      final boolean[] longFired = new boolean[]{false};
      final Runnable[] longTask = new Runnable[1];

      btn.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent e) {
          switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
              down[0] = e.getRawX(); down[1] = e.getRawY();
              start[0] = lp.x;       start[1] = lp.y;
              longFired[0] = false;
              longTask[0] = new Runnable() { public void run() {
                longFired[0] = true;
                // Long-press → send LONG_BROADCAST (do NOT close here)
                try { appctx.sendBroadcast(new Intent(LONG_BROADCAST)); } catch (Throwable ignored) {}
              }};
              h.postDelayed(longTask[0], 600); // 600ms long-press
              return true;

            case MotionEvent.ACTION_MOVE:
              int dx = Math.round(e.getRawX() - down[0]);
              int dy = Math.round(e.getRawY() - down[1]);
              // cancel long-press if dragging
              if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                h.removeCallbacks(longTask[0]);
                lp.x = start[0] + dx;
                lp.y = start[1] + dy;
                wm.updateViewLayout(btn, lp);
              }
              return true;

            case MotionEvent.ACTION_UP:
              // if long-press already fired, consume
              if (longFired[0]) return true;
              // cancel pending long-press
              h.removeCallbacks(longTask[0]);

              int dxUp = Math.abs(Math.round(e.getRawX() - down[0]));
              int dyUp = Math.abs(Math.round(e.getRawY() - down[1]));
              if (dxUp < touchSlop && dyUp < touchSlop) {
                // Short tap → send TAP_BROADCAST
                try { appctx.sendBroadcast(new Intent(TAP_BROADCAST)); 

// Button press effect
bg.setColor(0xFFFF0000);
btn.setBackground(bg);
new Handler().postDelayed(new Runnable(){ public void run(){ 
bg.setColor(0xFF448AFF);
btn.setBackground(bg);
 }}, 500);

} catch (Throwable ignored) {}
                return true;
              }
              return false;
          }
          return false;
        }
      });

      // show it
      wm.addView(btn, lp);

      // --- Polling loop for close-by-ID flag (no receivers) ---
      final Handler pollHandler = new Handler();
      final Runnable poller = new Runnable() {
        public void run() {
          try {
            if (prefs.getBoolean(KEY_CLOSE, false)) {
              // reset the flag first
              prefs.edit().putBoolean(KEY_CLOSE, false).apply();
              try { wm.removeView(btn); } catch (Throwable ignored) {}

              if (EXIT_ON_CLOSE) {
                // optional (can crash on some hosts): delay a bit then exit
                new Handler().postDelayed(new Runnable(){ public void run(){ System.exit(0); }}, 120);
              }
              return; // stop polling
            }
          } catch (Throwable ignored) {}
          // schedule next check
          pollHandler.postDelayed(this, POLL_MS);
        }
      };
      pollHandler.postDelayed(poller, POLL_MS);

    } catch (Throwable ignored) {}
  }
});
