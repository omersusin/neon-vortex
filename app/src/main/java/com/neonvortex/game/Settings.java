package com.neonvortex.game;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    public int difficulty = 2;
    public boolean vibrationEnabled = true;
    public boolean darkMode = true;
    public boolean trailEnabled = true;

    public float speedMult = 1.0f;
    public float spawnMult = 1.0f;

    private SharedPreferences prefs;

    public Settings(Context context) {
        prefs = context.getSharedPreferences("nv_settings", Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        difficulty = prefs.getInt("diff", 2);
        vibrationEnabled = prefs.getBoolean("vib", true);
        darkMode = prefs.getBoolean("dark", true);
        trailEnabled = prefs.getBoolean("trail", true);
        applyDiff();
    }

    public void save() {
        prefs.edit().putInt("diff", difficulty)
            .putBoolean("vib", vibrationEnabled)
            .putBoolean("dark", darkMode)
            .putBoolean("trail", trailEnabled).apply();
        applyDiff();
    }

    private void applyDiff() {
        switch (difficulty) {
            case 0: speedMult=0.6f; spawnMult=0.5f; break;
            case 1: speedMult=0.8f; spawnMult=0.75f; break;
            case 2: speedMult=1.0f; spawnMult=1.0f; break;
            case 3: speedMult=1.3f; spawnMult=1.3f; break;
            case 4: speedMult=1.7f; spawnMult=1.7f; break;
        }
    }
}
