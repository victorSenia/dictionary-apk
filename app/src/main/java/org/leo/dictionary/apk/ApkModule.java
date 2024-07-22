package org.leo.dictionary.apk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import dagger.Module;
import dagger.Provides;
import org.leo.dictionary.*;
import org.leo.dictionary.apk.audio.AndroidAudioService;
import org.leo.dictionary.apk.config.AssetsConfigurationReader;
import org.leo.dictionary.apk.config.PreferenceConfigurationReader;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;
import org.leo.dictionary.apk.word.provider.InputStreamWordProvider;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.config.ConfigParser;
import org.leo.dictionary.config.ConfigurationReader;
import org.leo.dictionary.config.ConfigurationService;
import org.leo.dictionary.config.entity.ParseWords;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordProvider;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Module
public class ApkModule {

    public static final String ASSET = "asset";
    public static final String FILE = "file";
    public static final String LAST_STATE_SOURCE = "org.leo.dictionary.apk.config.entity.LastState.source";
    public static final String LAST_STATE_URI = "org.leo.dictionary.apk.config.entity.LastState.uri";
    public static final String LAST_STATE_TOPIC = "org.leo.dictionary.apk.config.entity.LastState.topic";
    public static final String LAST_STATE_VOICE = "org.leo.dictionary.apk.config.entity.LastState.voice.";
    public static final String LAST_STATE = "_last_state";
    public static final String LAST_STATE_IS_PORTRAIT = "org.leo.dictionary.apk.config.entity.LastState.isPortrait";
    public static final String LAST_STATE_IS_NIGHT_MODE = "org.leo.dictionary.apk.config.entity.LastState.isNightMode";
    private final Application application;

    public ApkModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public static WordProvider provideAssetsWordProvider(ParseWords configuration, Context context) {
        AssetsWordProvider wordProvider = new AssetsWordProvider();
        wordProvider.setConfiguration(configuration);
        wordProvider.setContext(context);
        return wordProvider;
    }

    @Provides
    @Singleton
    public static ParseWords provideParseWordsConfiguration(Context context) {
        ParseWords parseWords = new ParseWords();
        parseWords.setProperties(new HashMap<>());
        AssetsConfigurationReader configurationReader = new AssetsConfigurationReader();
        configurationReader.setContext(context);
        Map<?, ?> properties = configurationReader.readConfig("parseWordsConfig.properties");
        return ConfigParser.createConfig(properties, parseWords);
    }

    @Provides
    @Singleton
    @Named("last_state")
    public static SharedPreferences provideLastState(Context context) {
        return context.getSharedPreferences(context.getPackageName() + LAST_STATE, Context.MODE_PRIVATE);
    }

    public static WordProvider createWordProvider(Context context, SharedPreferences last_state) {
        String source = last_state.getString(LAST_STATE_SOURCE, ASSET);
        String uri = last_state.getString(LAST_STATE_URI, null);
        if (ASSET.equals(source)) {
            ParseWords configuration = provideParseWordsConfiguration(context);
            if (uri != null) {
                configuration.setPath(uri);
            }
            return provideAssetsWordProvider(configuration, context);
        } else {
            try {
                return getInputStreamWordProvider(context, Uri.parse(uri));
            } catch (Exception e) {
                last_state.edit().remove(LAST_STATE_TOPIC).apply();
                return provideAssetsWordProvider(provideParseWordsConfiguration(context), context);
            }
        }
    }

    public static InputStreamWordProvider getInputStreamWordProvider(Context context, Uri data) throws FileNotFoundException {
        InputStreamWordProvider wordProvider = new InputStreamWordProvider();
        ParseWords parseWords = new ParseWords();
        parseWords.setProperties(new HashMap<>(PreferenceManager.getDefaultSharedPreferences(context).getAll()));
        wordProvider.setConfiguration(parseWords);
        wordProvider.setInputStream(context.getContentResolver().openInputStream(data));
        return wordProvider;
    }

    @Provides
    @Singleton
    public static AudioService provideAudioService(Context context) {
        AndroidAudioService audioService = new AndroidAudioService();
        audioService.setContext(context);
        audioService.setup();
        return audioService;
    }

    @Provides
    @Singleton
    public static ConfigurationService provideConfigurationService(ConfigurationReader configurationReader) {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigurationReader(configurationReader);
        return configurationService;
    }

    @Provides
    @Singleton
    public static ConfigurationReader provideConfigurationReader(Context context) {
        PreferenceConfigurationReader configurationReader = new PreferenceConfigurationReader();
        configurationReader.setContext(context);
        return configurationReader;
    }

    @Provides
    @Singleton
    public static PlayServiceImpl providePlayServiceImpl(Context context, ConfigurationService configurationService, AudioService audioService, WordProvider wordProvider,
                                                         UiUpdater uiUpdater, @Named("last_state") SharedPreferences last_state) {
        PlayServiceImpl playService = new PlayServiceImpl();
        playService.setConfigurationService(configurationService);
        playService.setAudioService(audioService);
        playService.setWordProvider(createWordProvider(context, last_state));
        playService.setUiUpdater(uiUpdater);

        WordCriteria wordCriteria = new WordCriteria();
        String topic = last_state.getString(LAST_STATE_TOPIC, null);
        if (topic != null && !topic.isEmpty()) {
            wordCriteria.setTopicsOr(Collections.singleton(topic));
        }
        playService.findWords(wordCriteria);

        return playService;
    }

    @Provides
    public static PlayService providePlayService(PlayServiceImpl playService) {
        return playService;
    }

    @Provides
    public static ExternalWordProvider provideExternalWordProvider(PlayServiceImpl playService) {
        return playService;
    }

    @Provides
    public static ExternalVoiceService provideExternalVoiceService(PlayServiceImpl playService) {
        return playService;
    }

    @Provides
    @Singleton
    public static UiUpdater provideUiUpdater() {
        return new ApkUiUpdater();
    }

    @Provides
    @Singleton
    public Context context() {
        return application;
    }

    @Provides
    @Singleton
    public Application provideApplication() {
        return application;
    }
}
