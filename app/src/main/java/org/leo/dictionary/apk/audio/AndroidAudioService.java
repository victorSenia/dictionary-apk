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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AndroidAudioService implements AudioService {
    private final static Logger LOGGER = Logger.getLogger(AndroidAudioService.class.getName());

    private TextToSpeech textToSpeech;
    private final Object lock = new Object();
    private boolean isSpeaking;
    private Context context;
    private Speech speech;
    private Map<String, List<Voice>> voicesPerLanguage;

    public void setContext(Context context) {
        this.context = context;
    }

    public void setup() {
        HashMap<Object, Object> properties = new HashMap<>(PreferenceManager.getDefaultSharedPreferences(context).getAll());
        PreferenceConfigurationReader.SharedPreferencesProperties changeListener = new PreferenceConfigurationReader.SharedPreferencesProperties(properties);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(changeListener);
        speech = new Speech();
        speech.setProperties(properties);
        initTTS();
    }

    private void initTTS() {
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
                notifyPlayer();
            }

            @Override
            public void onError(String utteranceId) {
                onError(utteranceId, -1);
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                LOGGER.severe("Text '" + utteranceId + "' finished with error " + errorCode);
            }
        });
    }

    private void notifyPlayer() {
        synchronized (lock) {
            isSpeaking = false;
            lock.notifyAll();
        }
    }

    @Override
    public void play(String language, String text) {
        LOGGER.info(language + " " + text);
        setTTS(true);
        Voice selectedVoice = getSelectedVoice(language);
        if (selectedVoice != null) {
            textToSpeech.setVoice(selectedVoice);
        } else {
            textToSpeech.setLanguage(Locale.forLanguageTag(language));
        }
        isSpeaking = true;
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
        synchronized (lock) {
            while (isSpeaking) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void setTTS(boolean retry) {
        try {
            textToSpeech.setSpeechRate(speech.getSpeed());
            textToSpeech.setPitch(speech.getPitch());
        } catch (Exception e) {
            LOGGER.info("TTS error '" + e + "'");
            if (retry) {
                initTTS();
                setTTS(false);
            }
        }
    }


    public void playAsynchronous(String language, String text) {
        LOGGER.info(language + " " + text);
        setTTS(true);
        Voice selectedVoice = getSelectedVoice(language);
        if (selectedVoice != null) {
            textToSpeech.setVoice(selectedVoice);
        } else {
            textToSpeech.setLanguage(Locale.forLanguageTag(language));
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text);
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
        List<Voice> voices = voicesPerLanguage.get(language);
        if (voices != null) {
            return voices.stream().map(Voice::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void abort() {
        isSpeaking = false;
        if (textToSpeech != null) {
            textToSpeech.stop();
            LOGGER.info("abort");
        }
    }

    @Override
    public void shutdown() {
        isSpeaking = false;
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}
