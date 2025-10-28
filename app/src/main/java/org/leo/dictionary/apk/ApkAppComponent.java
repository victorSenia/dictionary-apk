package org.leo.dictionary.apk;

import android.content.SharedPreferences;
import dagger.Component;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.helper.GrammarCriteriaProvider;
import org.leo.dictionary.apk.helper.PlayServiceService;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.grammar.provider.GrammarProvider;
import org.leo.dictionary.grammar.provider.SentenceProvider;
import org.leo.dictionary.word.provider.DBWordProvider;
import org.leo.dictionary.word.provider.WordProvider;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Component(modules = ApkModule.class)
public interface ApkAppComponent {
    PlayService playService();

    AudioService audioService();

    WordProvider externalWordProvider();

    AudioService externalVoiceService();

    UiUpdater uiUpdater();

    WordCriteriaProvider wordCriteriaProvider();

    GrammarCriteriaProvider grammarCriteriaProvider();

    GrammarProvider externalGrammarProvider();

    SentenceProvider externalSentenceProvider();

    @Named("lastState")
    SharedPreferences lastState();

    @Named("dbWordProvider")
    DBWordProvider dbWordProvider();

    void inject(PlayServiceService service);
}
