package org.leo.dictionary.apk.audio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class AndroidSpeechRecognitionService {
    public static final Integer RecordAudioRequestCode = 1;
    private final static Logger LOGGER = Logger.getLogger(AndroidSpeechRecognitionService.class.getName());
    private SpeechRecognizer speechRecognizer;
    private Consumer<ArrayList<String>> resultConsumer;
    private Consumer<String> onErrorConsumer;

    public void createAndStart(Activity activity, String language, Consumer<ArrayList<String>> resultConsumer, Consumer<String> onErrorConsumer) {
        this.resultConsumer = resultConsumer;
        this.onErrorConsumer = onErrorConsumer;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(activity);
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        speechRecognizer.setRecognitionListener(createRecognitionListener());
        startListening(createRecognizerIntent(language));
    }

    protected Intent createRecognizerIntent(String language) {
        Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        return speechRecognizerIntent;
    }

    protected void startListening(Intent speechRecognizerIntent) {
        speechRecognizer.startListening(speechRecognizerIntent);
    }

    protected RecognitionListener createRecognitionListener() {
        return new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                LOGGER.fine("onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {
                LOGGER.fine("onBeginningOfSpeech");
            }

            @Override
            public void onRmsChanged(float v) {
                LOGGER.fine("onRmsChanged");
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                LOGGER.fine("onBufferReceived");
            }

            @Override
            public void onEndOfSpeech() {
                LOGGER.fine("onEndOfSpeech");
            }

            @Override
            public void onError(int i) {
                LOGGER.severe("onError");
                onErrorConsumer.accept(errorMapping(i));
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                LOGGER.fine("onResults " + data);
                resultConsumer.accept(data);
                destroy();
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                LOGGER.fine("onPartialResults");
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                LOGGER.fine("onEvent");
            }
        };
    }

    private String errorMapping(int error) {
        switch (error) {
            case 1:
                return "Network operation timed out.";
            case 2:
                return "Other network related errors.";
            case 3:
                return "Audio recording error.";
            case 4:
                return "Server sends error status.";
            case 5:
                return "Other client side errors.";
            case 6:
                return "No speech input";
            case 7:
                return "No recognition result matched.";
            case 8:
                return "RecognitionService busy.";
            case 9:
                return "Insufficient permissions";
            case 10:
                return "Too many requests from the same client.";
            case 11:
                return "Server has been disconnected, e.g. because the app has crashed.";
            case 12:
                return "Requested language is not available to be used with the current recognizer.";
            case 13:
                return "Requested language is supported, but not available currently (e.g. not downloaded yet).";
            case 14:
                return "The service does not allow to check for support.";
            case 15:
                return "The service does not support listening to model downloads events.";
            default:
                return "Unknown error";
        }
    }

    public void stopListening() {
        speechRecognizer.stopListening();
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        resultConsumer = null;
        onErrorConsumer = null;
    }

    protected void checkPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
    }
}
