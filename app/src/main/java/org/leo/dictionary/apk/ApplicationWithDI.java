package org.leo.dictionary.apk;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.HashMap;
import java.util.Map;

// appComponent lives in the Application class to share its lifecycle
public class ApplicationWithDI extends Application {

    // Reference to the application graph that is used across the whole app
    public ApkAppComponent appComponent;
    public Map<String, Object> data = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = DaggerApkAppComponent.builder().apkModule(new ApkModule(this)).build();
        if (appComponent.lastState().getBoolean(ApkModule.LAST_STATE_IS_NIGHT_MODE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    @Override
    public void onTerminate() {
        appComponent.playService().pause();
        super.onTerminate();
    }
}