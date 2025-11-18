package org.leo.dictionary.apk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import dagger.Module;
import dagger.Provides;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.PlayServiceImpl;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.activity.ActivityUtils;
import org.leo.dictionary.apk.activity.MainActivity;
import org.leo.dictionary.apk.audio.AndroidAudioService;
import org.leo.dictionary.apk.config.AssetsConfigurationReader;
import org.leo.dictionary.apk.config.PreferenceConfigurationReader;
import org.leo.dictionary.apk.grammar.provider.AssetsGrammarProvider;
import org.leo.dictionary.apk.grammar.provider.AssetsSentenceProvider;
import org.leo.dictionary.apk.helper.*;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;
import org.leo.dictionary.apk.word.provider.InputStreamWordProvider;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.config.ConfigParser;
import org.leo.dictionary.config.ConfigurationReader;
import org.leo.dictionary.config.ConfigurationService;
import org.leo.dictionary.config.entity.ParseGrammar;
import org.leo.dictionary.config.entity.ParseSentences;
import org.leo.dictionary.config.entity.ParseWords;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.grammar.provider.GrammarProvider;
import org.leo.dictionary.grammar.provider.SentenceProvider;
import org.leo.dictionary.word.provider.DBWordProvider;
import org.leo.dictionary.word.provider.WordProvider;
import org.leo.dictionary.word.provider.WordProviderDelegate;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Module
public class ApkModule {

    public static final String ASSET = "asset";
    public static final String FILE = "file";
    public static final String DB = "db";
    public static final String LAST_STATE_SOURCE = "org.leo.dictionary.apk.config.entity.LastState.source";
    public static final String LAST_STATE_URI = "org.leo.dictionary.apk.config.entity.LastState.uri";
    public static final String LAST_STATE_GRAMMAR_SOURCE = "org.leo.dictionary.apk.config.entity.LastState.grammarSource";
    public static final String LAST_STATE_GRAMMAR_URI = "org.leo.dictionary.apk.config.entity.LastState.grammarUri";
    public static final String LAST_STATE_SENTENCE_SOURCE = "org.leo.dictionary.apk.config.entity.LastState.sentenceSource";
    public static final String LAST_STATE_SENTENCE_URI = "org.leo.dictionary.apk.config.entity.LastState.sentenceUri";
    public static final String LAST_STATE_VOICE = "org.leo.dictionary.apk.config.entity.LastState.voice.";
    public static final String LAST_STATE = "_last_state";
    public static final String LAST_STATE_IS_PORTRAIT = "org.leo.dictionary.apk.config.entity.LastState.isPortrait";
    public static final String LAST_STATE_IS_NIGHT_MODE = "org.leo.dictionary.apk.config.entity.LastState.isNightMode";
    public static final String LAST_STATE_WORD_CRITERIA = "org.leo.dictionary.apk.config.entity.LastState.wordCriteria";
    public static final String LAST_STATE_GRAMMAR_CRITERIA = "org.leo.dictionary.apk.config.entity.LastState.sentenceCriteria";
    public static final String LAST_STATE_CURRENT_INDEX = "org.leo.dictionary.apk.config.entity.LastState.currentIndex";
    private final Application application;

    public ApkModule(Application application) {
        this.application = application;
    }

    public static void playAsynchronousIfPossible(AudioService audioService, String language, String string) {
        if (audioService instanceof AndroidAudioService) {
            ((AndroidAudioService) audioService).playAsynchronous(language, string);
        } else {
            audioService.play(language, string);
        }
    }

    public static WordProvider createAssetsWordProvider(ParseWords configuration, Context context) {
        AssetsWordProvider wordProvider = new AssetsWordProvider();
        wordProvider.setConfiguration(configuration);
        wordProvider.setContext(context);
        wordProvider.parseAndUpdateConfiguration();
        return wordProvider;
    }

    public static GrammarProvider createAssetsGrammarProvider(ParseGrammar configuration, Context context) {
        AssetsGrammarProvider grammarProvider = new AssetsGrammarProvider();
        grammarProvider.setConfiguration(configuration);
        grammarProvider.setContext(context);
        grammarProvider.parseAndUpdateConfiguration();
        return grammarProvider;
    }

    public static GrammarProvider createAssetsGrammarProvider(String name, Context context) {
        ParseGrammar configuration = new ParseGrammar();
        configuration.setProperties(new HashMap<>());
        configuration.setPath(name);
        return createAssetsGrammarProvider(configuration, context);
    }

    public static SentenceProvider createAssetsSentenceProvider(String name, Context context) {
        ParseSentences configuration = new ParseSentences();
        configuration.setProperties(new HashMap<>());
        configuration.setPath(name);
        return createAssetsSentenceProvider(configuration, context);
    }

    public static SentenceProvider createAssetsSentenceProvider(ParseSentences configuration, Context context) {
        AssetsSentenceProvider grammarProvider = new AssetsSentenceProvider();
        grammarProvider.setConfiguration(configuration);
        grammarProvider.setContext(context);
        grammarProvider.parseAndUpdateConfiguration();
        return grammarProvider;
    }

    @Provides
    @Singleton
    public static SentenceProvider getOrCreateSentenceProvider(Context context, @Named("lastState") SharedPreferences lastState) {
//        criteriaProvider.setObject(null);
        SentenceProvider provider = createAssetsSentenceProvider(lastState.getString(LAST_STATE_SENTENCE_URI, "Sentences.txt"), context);
        SentenceProviderHolder providerHolder = new SentenceProviderHolder();
        providerHolder.setSentenceProvider(provider);
        return providerHolder;
    }

    @Provides
    @Singleton
    @Named("dbWordProvider")
    public static DBWordProvider provideDBWordProvider(DatabaseManager databaseManager) {
        DBWordProvider wordProvider = new DBWordProvider();
        wordProvider.setDbManager(databaseManager);
        return wordProvider;
    }

    @Provides
    @Singleton
    public static DatabaseHelper provideDatabaseHelper(Context context) {
        return new DatabaseHelper(context);
    }

    @Provides
    @Singleton
    public static DatabaseManager provideDatabaseManager(DatabaseHelper databaseHelper) {
        return new DatabaseManager(databaseHelper);
    }

    public static boolean isDBSource(@Named("lastState") SharedPreferences lastState) {
        String source = getLastStateSource(lastState);
        return DB.equals(source);
    }

    public static String getLastStateSource(SharedPreferences lastState) {
        return lastState.getString(LAST_STATE_SOURCE, ASSET);
    }

    public static int getLastStateCurrentIndex(SharedPreferences lastState) {
        int defaultIndex = 0;
        if (lastState.contains(LAST_STATE_CURRENT_INDEX)) {
            return lastState.getInt(LAST_STATE_CURRENT_INDEX, defaultIndex);
        }
        return defaultIndex;
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

    public static WordProvider createWordProvider(Context context, @Named("lastState") SharedPreferences lastState, WordCriteriaProvider criteriaProvider) {
        String source = getLastStateSource(lastState);
        String uri = lastState.getString(LAST_STATE_URI, null);
        try {
            if (FILE.equals(source)) {
                return createInputStreamWordProvider(context, Uri.parse(uri));
            } else if (ASSET.equals(source)) {
                ParseWords configuration = provideParseWordsConfiguration(context);
                if (uri != null && Arrays.asList(context.getAssets().list(AssetsWordProvider.ASSETS_WORDS)).contains(uri)) {
                    configuration.setPath(uri);
                }
                return createAssetsWordProvider(configuration, context);
            }
        } catch (Exception e) {
            ActivityUtils.logUnhandledException(e);
        }
        Toast.makeText(context, "File cannot be accessed.", Toast.LENGTH_SHORT).show();
        criteriaProvider.setObject(null);
        return createAssetsWordProvider(provideParseWordsConfiguration(context), context);
    }

    @Provides
    @Singleton
    public static GrammarProvider getOrCreateGrammarProvider(Context context, @Named("lastState") SharedPreferences lastState, GrammarCriteriaProvider criteriaProvider) {
//        criteriaProvider.setObject(null);
//        GrammarProvider provider = createAssetsGrammarProvider(lastState.getString(LAST_STATE_GRAMMAR_URI, "Conjugation of Modal Verbs.txt"), context);
        GrammarProvider provider = createAssetsGrammarProvider(lastState.getString(LAST_STATE_GRAMMAR_URI, "konnen.txt"), context);
        GrammarProviderHolder providerHolder = new GrammarProviderHolder();
        providerHolder.setGrammarProvider(provider);
        return providerHolder;
    }

    @Provides
    @Singleton
    public static WordProvider getOrCreateWordProvider(Context context, @Named("lastState") SharedPreferences lastState, @Named("dbWordProvider") DBWordProvider dbWordProvider, WordCriteriaProvider criteriaProvider) {
        WordProviderDelegate wordProvider = new WordProviderDelegate();
        if (isDBSource(lastState)) {
            wordProvider.setWordProvider(dbWordProvider);
        } else {
            wordProvider.setWordProvider(createWordProvider(context, lastState, criteriaProvider));
        }
        return wordProvider;
    }

    public static InputStreamWordProvider createInputStreamWordProvider(Context context, Uri data) throws FileNotFoundException {
        InputStreamWordProvider wordProvider = new InputStreamWordProvider();
        ParseWords parseWords = new ParseWords();
        parseWords.setProperties(new HashMap<>(PreferenceManager.getDefaultSharedPreferences(context).getAll()));
        wordProvider.setConfiguration(parseWords);
        wordProvider.setInputStream(context.getContentResolver().openInputStream(data));
        return wordProvider;
    }

    public static List<Word> getWords(Context context) {
        ApkAppComponent appComponent = ((ApplicationWithDI) context.getApplicationContext()).appComponent;
        return appComponent.externalWordProvider().findWords(appComponent.wordCriteriaProvider().getObject());
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
        WordCriteria criteria = criteriaProvider.getObject();
        playService.setPlayTranslationFor(criteria.getPlayTranslationFor());
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
    @Singleton
    public static GrammarCriteriaProvider provideGrammarCriteriaProvider(@Named("lastState") SharedPreferences lastState) {
        GrammarCriteriaProvider criteriaProvider = new GrammarCriteriaProvider();
        criteriaProvider.setLastState(lastState);
        return criteriaProvider;
    }


    @Provides
    @Singleton
    public static PlayServiceAdapter providePlayServiceWrapper(Context context, PlayServiceImpl playService) {
        return new PlayServiceAdapter(context, playService);
    }

    @Provides
    @Singleton
    public static PlayService providePlayService(PlayServiceAdapter playService) {
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
