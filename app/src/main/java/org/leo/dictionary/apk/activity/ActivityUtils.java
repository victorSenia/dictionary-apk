package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.leo.dictionary.entity.Topic;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class ActivityUtils {
    public final static Logger LOGGER = Logger.getLogger(ActivityUtils.class.getName());

    public static void runAtBackground(Runnable runnable) {
        new Thread(runnable).start();
    }

    public static void logUnhandledException(Exception e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    public static Set<Long> getTopicIds(Collection<Topic> topics) {
        return topics != null ? topics.stream().map(Topic::getId).collect(Collectors.toSet()) : Collections.emptySet();
    }

    public static void setFullScreen(Activity activity, View root) {
        activity.getWindow().setDecorFitsSystemWindows(false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            // get Insets object
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            // apply padding using Insets
            v.setPadding(
                    Math.max(systemBarsInsets.left, imeInsets.left),
                    Math.max(systemBarsInsets.top, imeInsets.top),
                    Math.max(systemBarsInsets.right, imeInsets.right),
                    Math.max(systemBarsInsets.bottom, imeInsets.bottom)
            );
            return insets; // must return insets
        });
        ViewCompat.requestApplyInsets(root);
    }
}
