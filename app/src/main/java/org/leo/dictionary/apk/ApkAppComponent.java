package org.leo.dictionary.apk;

import android.content.SharedPreferences;
import dagger.Component;
import org.leo.dictionary.ExternalVoiceService;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Component(modules = ApkModule.class)
public interface ApkAppComponent {
    PlayService playService();

    ExternalWordProvider externalWordProvider();

    ExternalVoiceService externalVoiceService();

    UiUpdater uiUpdater();

    WordCriteriaProvider wordCriteriaProvider();

    @Named("lastState")
    SharedPreferences lastState();
}
