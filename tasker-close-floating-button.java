final String OVERLAY_ID = "bubble-1";          // must match Snippet 1
final String PREF_NAME  = "bsh_overlay";
final String KEY_CLOSE  = "close:" + OVERLAY_ID;

android.content.SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);

// Signal close; the bubble will remove itself on next poll tick
prefs.edit().putBoolean(KEY_CLOSE, true).apply();
