package org.leo.dictionary.apk;

import dagger.Component;
import org.leo.dictionary.ExternalVoiceService;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;

import javax.inject.Singleton;

@Singleton
@Component(modules = ApkModule.class)
public interface ApkAppComponent {
    PlayService playService();

    ExternalWordProvider externalWordProvider();

    ExternalVoiceService externalVoiceService();

    UiUpdater uiUpdater();
}
