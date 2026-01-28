package org.leo.dictionary.apk.helper;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.widget.Toast;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.PlayServiceImpl;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.word.provider.WordProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayServiceAdapter implements PlayService, AudioManager.OnAudioFocusChangeListener {

    private final Context context;
    private final PlayServiceImpl playService;

    private final AtomicBoolean resumeOnFocusGain = new AtomicBoolean(false);
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    public PlayServiceAdapter(Context context, PlayServiceImpl playService) {
        this.context = context.getApplicationContext();
        this.playService = playService;
    }

    private void ensureServiceRunning() {
        if (audioManager == null) {
            audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        }
        if (isAudioFocusGranted()) {
            Intent serviceIntent = new Intent(context, PlayServiceService.class);
            context.startForegroundService(serviceIntent);
        }
    }

    private boolean isAudioFocusGranted() {
        if (audioFocusRequest == null) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
        int res = audioManager.requestAudioFocus(audioFocusRequest);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == res) {
            return true;
        } else if (AudioManager.AUDIOFOCUS_REQUEST_DELAYED == res) {
            resumeOnFocusGain.set(true);
            Toast.makeText(context, context.getString(R.string.audio_delayed), Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(context, context.getString(R.string.audio_not_possible), Toast.LENGTH_SHORT).show();
        return false;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        if (playService != null) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (playService.isPlaying()) {
                        playService.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (playService.isPlaying()) {
                        resumeOnFocusGain.set(true);
                        playService.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (resumeOnFocusGain.get()) {
                        resumeOnFocusGain.set(false);
                        playService.play();
                    }
            }
        }
    }

    @Override
    public void play() {
        ensureServiceRunning();
        playService.play();
    }

    @Override
    public void pause() {
        pausePlayService();

        Intent stopIntent = new Intent(context, PlayServiceService.class);
        stopIntent.setAction(PlayServiceService.ACTION_STOP_SERVICE);
        context.startService(stopIntent);
    }

    private void abandonAudioFocusRequest() {
        if (audioFocusRequest != null && audioManager != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        }
        audioManager = null;
        audioFocusRequest = null;
    }

    public void pausePlayService() {
        abandonAudioFocusRequest();
        playService.pause();
    }

    @Override
    public void setPlayTranslationFor(Set<String> playTranslationFor) {
        playService.setPlayTranslationFor(playTranslationFor);
    }

    @Override
    public void next() {
        ensureServiceRunning();
        playService.next();
    }

    @Override
    public void playFrom(int index) {
        ensureServiceRunning();
        playService.playFrom(index);
    }

    @Override
    public boolean isPlaying() {
        return playService.isPlaying();
    }

    @Override
    public boolean isReady() {
        return playService.isReady();
    }

    @Override
    public void previous() {
        ensureServiceRunning();
        playService.previous();
    }

    @Override
    public void setUiUpdater(UiUpdater uiUpdater) {
        playService.setUiUpdater(uiUpdater);
    }

    @Override
    public void setWords(List<Word> words) {
        playService.setWords(words);
    }

    public void setWordProvider(WordProvider wordProvider) {
        playService.setWordProvider(wordProvider);
    }
}
