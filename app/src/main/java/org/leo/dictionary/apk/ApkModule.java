package org.leo.dictionary.apk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import dagger.Module;
import dagger.Provides;
import org.jetbrains.annotations.NotNull;
import org.leo.dictionary.*;
import org.leo.dictionary.apk.activity.MainActivity;
import org.leo.dictionary.apk.audio.AndroidAudioService;
import org.leo.dictionary.apk.config.AssetsConfigurationReader;
import org.leo.dictionary.apk.config.PreferenceConfigurationReader;
import org.leo.dictionary.apk.helper.DBManager;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.apk.word.provider.InputStreamWordProvider;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.config.ConfigParser;
import org.leo.dictionary.config.ConfigurationReader;
import org.leo.dictionary.config.ConfigurationService;
import org.leo.dictionary.config.entity.ParseWords;
import org.leo.dictionary.word.provider.WordProvider;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Module
public class ApkModule {

    public static final String ASSET = "asset";
    public static final String FILE = "file";
    public static final String DB = "db";
    public static final String LAST_STATE_SOURCE = "org.leo.dictionary.apk.config.entity.LastState.source";
    public static final String LAST_STATE_URI = "org.leo.dictionary.apk.config.entity.LastState.uri";
    public static final String LAST_STATE_VOICE = "org.leo.dictionary.apk.config.entity.LastState.voice.";
    public static final String LAST_STATE = "_last_state";
    public static final String LAST_STATE_IS_PORTRAIT = "org.leo.dictionary.apk.config.entity.LastState.isPortrait";
    public static final String LAST_STATE_IS_NIGHT_MODE = "org.leo.dictionary.apk.config.entity.LastState.isNightMode";
    public static final String LAST_STATE_WORD_CRITERIA = "org.leo.dictionary.apk.config.entity.LastState.wordCriteria";
    public static final String LAST_STATE_CURRENT_INDEX = "org.leo.dictionary.apk.config.entity.LastState.currentIndex";
    public static final String LANGUAGES_SEPARATOR = "_";
    private final Application application;

    public ApkModule(Application application) {
        this.application = application;
    }

    public static WordProvider provideAssetsWordProvider(ParseWords configuration, Context context) {
        AssetsWordProvider wordProvider = new AssetsWordProvider();
        wordProvider.setConfiguration(configuration);
        updateLanguagesInConfiguration(configuration);
        wordProvider.setContext(context);
        return wordProvider;
    }

    @Provides
    @Singleton
    @Named("dbWordProvider")
    public static DBWordProvider provideDBWordProvider(Context context) {
        DBWordProvider wordProvider = new DBWordProvider();
        wordProvider.setDbManager(new DBManager(context));
        return wordProvider;
    }

    public static boolean isDBSource(@Named("lastState") SharedPreferences lastState) {
        String source = getLastStateSource(lastState);
        return DB.equals(source);
    }

    public static String getLastStateSource(SharedPreferences lastState) {
        return lastState.getString(LAST_STATE_SOURCE, ASSET);
    }

    protected static void updateLanguagesInConfiguration(ParseWords configuration) {
        try {
            String path = configuration.getPath();
            int start = path.lastIndexOf("-") + 1;
            int end = path.lastIndexOf(".");
            String languagesString = path.substring(start, end);
            String[] languages = languagesString.split(LANGUAGES_SEPARATOR, 2);
            String languageFrom = languages[0];
            languages = languages[1].split(LANGUAGES_SEPARATOR);
            configuration.setLanguageFrom(languageFrom);
            configuration.setLanguagesTo(new ArrayList<>(Arrays.asList(languages)));
        } catch (Exception e) {
            MainActivity.logUnhandledException(e);
            //ignore
        }
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
    @Named("lastState")
    public static SharedPreferences provideLastState(Context context) {
        return context.getSharedPreferences(context.getPackageName() + LAST_STATE, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public static WordProvider createWordProvider(Context context, @Named("lastState") SharedPreferences lastState) {
        String source = getLastStateSource(lastState);
        String uri = lastState.getString(LAST_STATE_URI, null);
        try {
            if (DB.equals(source)) {
                return provideDBWordProvider(context);
            } else if (ASSET.equals(source)) {
                ParseWords configuration = provideParseWordsConfiguration(context);
                if (uri != null && Arrays.asList(context.getAssets().list(AssetsWordProvider.ASSETS_WORDS)).contains(uri)) {
                    configuration.setPath(uri);
                }
                return provideAssetsWordProvider(configuration, context);
            } else {
                return getInputStreamWordProvider(context, Uri.parse(uri));
            }
        } catch (Exception e) {
            MainActivity.logUnhandledException(e);
            return provideAssetsWordProvider(provideParseWordsConfiguration(context), context);
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
    public static PlayServiceImpl providePlayServiceImpl(ConfigurationService configurationService, AudioService audioService, WordProvider wordProvider,
                                                         UiUpdater uiUpdater, WordCriteriaProvider criteriaProvider) {
        PlayServiceImpl playService = new PlayServiceImpl();
        playService.setConfigurationService(configurationService);
        playService.setAudioService(audioService);
        playService.setWordProvider(wordProvider);
        playService.setUiUpdater(uiUpdater);
        playService.findWords(criteriaProvider.getWordCriteria());
        return playService;
    }

    @Provides
    @Singleton
    public static WordCriteriaProvider provideWordCriteriaProvider(@Named("lastState") SharedPreferences lastState) {
        WordCriteriaProvider criteriaProvider = new WordCriteriaProvider();
        criteriaProvider.setLastState(lastState);
        return criteriaProvider;
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
    public static UiUpdater provideUiUpdater(@Named("lastState") SharedPreferences lastState) {
        ApkUiUpdater apkUiUpdater = new ApkUiUpdater();
        apkUiUpdater.setLastState(lastState);
        return apkUiUpdater;
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
