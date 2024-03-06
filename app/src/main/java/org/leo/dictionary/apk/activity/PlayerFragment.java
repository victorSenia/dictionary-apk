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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.FragmentPlayerBinding;

public class PlayerFragment extends Fragment implements AudioManager.OnAudioFocusChangeListener {
    private FragmentPlayerBinding binding;
    private PlayService playService;
    private AudioManager audioManager;

    @Override
    public void onResume() {
        super.onResume();
        updateButtonUi();
    }

    private void updateButtonUi() {
        if (!playService.isPlaying()) {
            binding.buttonPlay.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_play, 0, 0, 0);
        } else {
            binding.buttonPlay.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_pause, 0, 0, 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playService.pause();
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playService = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.playService();
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
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
                    playService.play();
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
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build();
        int res = audioManager.requestAudioFocus(focusRequest);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == res) {
            return true;
        }
        Toast.makeText(getActivity().getBaseContext(), getString(R.string.audio_not_possible), Toast.LENGTH_SHORT).show();
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
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                playService.pause();
                updateButtonUi();
        }
    }
}