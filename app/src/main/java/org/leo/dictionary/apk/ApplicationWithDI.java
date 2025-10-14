package org.leo.dictionary.apk;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

// appComponent lives in the Application class to share its lifecycle
public class ApplicationWithDI extends Application {

    public final Map<String, Object> data = new HashMap<>();
    // Reference to the application graph that is used across the whole app
    public ApkAppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        setupErrorLogging();
        appComponent = DaggerApkAppComponent.builder().apkModule(new ApkModule(this)).build();
        if (appComponent.lastState().getBoolean(ApkModule.LAST_STATE_IS_NIGHT_MODE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
//        setupLogging(this);
    }

    private static void setupErrorLogging() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Logger logger = Logger.getLogger(ApplicationWithDI.class.getName());
            logger.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), throwable);

            // Let Android handle the crash
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, throwable);
        });
    }

    @Override
    public void onTerminate() {
        appComponent.playService().pause();
        super.onTerminate();
    }

    private void setupLogging(Context context) {
        try {
            File logDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MyAppLogs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String absolutePath = logDir.getAbsolutePath();

            try (InputStream in = context.getAssets().open("logging.properties");
                 Scanner scanner = new Scanner(in).useDelimiter("\\A")) {
                String configText = scanner.hasNext() ? scanner.next() : "";
                configText = configText.replace("ABSOLUTE_LOG_PATH", absolutePath);

                try (InputStream configStream = new ByteArrayInputStream(configText.getBytes(StandardCharsets.UTF_8))) {
                    LogManager.getLogManager().readConfiguration(configStream);
                }
            }

            Logger logger = Logger.getLogger(ApplicationWithDI.class.getName());
            logger.info("Logging initialized successfully");
            logger.severe("Logging severe initialized successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}