package org.leo.dictionary.apk;

import android.app.Application;

// appComponent lives in the Application class to share its lifecycle
public class ApplicationWithDI extends Application {

    // Reference to the application graph that is used across the whole app
    public ApkAppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = DaggerApkAppComponent.builder().apkModule(new ApkModule(this)).build();
    }

    @Override
    public void onTerminate() {
        appComponent.playService().pause();
        super.onTerminate();
    }
}