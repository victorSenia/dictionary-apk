package org.leo.dictionary.apk.audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import androidx.preference.PreferenceManager;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.config.PreferenceConfigurationReader;
import org.leo.dictionary.apk.config.entity.Speech;
import org.leo.dictionary.audio.AudioService;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AndroidAudioService implements AudioService {
    private final static Logger LOGGER = Logger.getLogger(AndroidAudioService.class.getName());

    private TextToSpeech textToSpeech = null;
    private ConcurrentSkipListSet<String> speaking;
    private Context context;
    private Speech speech;
    private PreferenceConfigurationReader.SharedPreferencesProperties changeListener;
    private Map<String, List<Voice>> voicesPerLanguage;

    public void setContext(Context context) {
        this.context = context;
    }

    public void setup() {
        HashMap<Object, Object> properties = new HashMap<>(PreferenceManager.getDefaultSharedPreferences(context).getAll());
        changeListener = new PreferenceConfigurationReader.SharedPreferencesProperties(properties);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(changeListener);
        speech = new Speech();
        speech.setProperties(properties);

        speaking = new ConcurrentSkipListSet<>();
        textToSpeech = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                Set<Voice> voices = textToSpeech.getVoices();
                voicesPerLanguage = voices.stream().collect(Collectors.groupingBy(v -> v.getLocale().getLanguage()));
            }
        });
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                speaking.remove(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                speaking.remove(utteranceId);
            }
        });
    }

    @Override
    public void play(String language, String text) {
        LOGGER.info(language + " " + text);
        textToSpeech.setSpeechRate(speech.getSpeed());
        textToSpeech.setPitch(speech.getPitch());
        Voice selectedVoice = getSelectedVoice(language);
        if (selectedVoice != null) {
            textToSpeech.setVoice(selectedVoice);
        } else {
            textToSpeech.setLanguage(Locale.forLanguageTag(language));
        }
        speaking.add(text);
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
        do {
        } while (speaking.contains(text));
    }

    private Voice getSelectedVoice(String language) {
        String voiceName = ApkModule.provideLastState(context).getString(ApkModule.LAST_STATE_VOICE + language, null);
        if (voiceName != null && voicesPerLanguage.get(language) != null) {
            return voicesPerLanguage.get(language).stream().filter(v -> voiceName.equals(v.getName())).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public List<String> getVoicesNames(String language) {
        if (voicesPerLanguage.get(language) != null) {
            return voicesPerLanguage.get(language).stream().map(Voice::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void abort() {
        speaking.clear();
        if (textToSpeech != null) {
            textToSpeech.stop();
            LOGGER.info("abort");
        }
    }

    @Override
    public void shutdown() {
        speaking.clear();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}
