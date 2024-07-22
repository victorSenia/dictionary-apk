package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.FragmentPlayerBinding;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerFragment extends Fragment implements AudioManager.OnAudioFocusChangeListener {
    private final AtomicBoolean resumeOnFocusGain = new AtomicBoolean(false);
    private FragmentPlayerBinding binding;
    private PlayService playService;
    private AudioManager audioManager;

    @Override
    public void onResume() {
        super.onResume();
        updateButtonUi();
    }

    public void updateButtonUi() {
        if (!playService.isPlaying()) {
            binding.buttonPlay.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
        } else {
            binding.buttonPlay.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause, 0, 0, 0);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        playService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.playService();
        audioManager = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonNext.setOnClickListener(v -> {
            if (isAudioFocusGranted()) {
                playService.next();
                updateButtonUi();
            }
        });
        binding.buttonPlay.setOnClickListener(v -> {
            if (!playService.isPlaying()) {
                if (isAudioFocusGranted()) {
                    int index = ApkModule.getLastStateCurrentIndex(((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.lastState());
                    if (index != -1) {
                        playService.playFrom(index);
                    } else {
                        playService.play();
                    }
                }
            } else {
                playService.pause();
            }
            updateButtonUi();
        });
        binding.buttonPrevious.setOnClickListener(v -> {
            if (isAudioFocusGranted()) {
                playService.previous();
                updateButtonUi();
            }
        });
    }

    private boolean isAudioFocusGranted() {
        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build();
        int res = audioManager.requestAudioFocus(focusRequest);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == res) {
            return true;
        } else if (AudioManager.AUDIOFOCUS_REQUEST_DELAYED == res) {
            resumeOnFocusGain.set(true);
            Toast.makeText(requireActivity().getBaseContext(), getString(R.string.audio_delayed), Toast.LENGTH_SHORT).show();
            return false;
        }
        Toast.makeText(requireActivity().getBaseContext(), getString(R.string.audio_not_possible), Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        playService = null;
        audioManager = null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (playService != null) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    playService.pause();
                    updateButtonUi();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (playService.isPlaying()) {
                        resumeOnFocusGain.set(true);
                        playService.pause();
                        updateButtonUi();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (resumeOnFocusGain.get()) {
                        playService.play();
                    }
            }
        }
    }
}