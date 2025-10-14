package org.leo.dictionary.apk.activity;

import android.R.drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.viewmodel.DetailsViewModel;
import org.leo.dictionary.apk.audio.AndroidSpeechRecognitionService;
import org.leo.dictionary.apk.databinding.ActivitySpeechRecognitionBinding;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SpeechRecognitionActivity extends AppCompatActivity {
    private final AndroidSpeechRecognitionService speechRecognitionService = new AndroidSpeechRecognitionService();
    private final AtomicBoolean recoding = new AtomicBoolean(false);
    private ActivitySpeechRecognitionBinding binding;
    private DetailsViewModel detailsViewModel;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpeechRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        detailsViewModel = new ViewModelProvider(this).get(DetailsViewModel.class);
        detailsViewModel.getData().observe(this, w -> binding.buttonSpeak.setEnabled(w != null));
        binding.buttonSpeak.setOnClickListener(buttonView -> {
            if (recoding.get()) {
                binding.buttonSpeak.setImageDrawable(AppCompatResources.getDrawable(this, drawable.ic_btn_speak_now));
                speechRecognitionService.stopListening();
            } else {
                binding.buttonSpeak.setImageDrawable(AppCompatResources.getDrawable(this, drawable.sym_action_chat));
                binding.textResult.setVisibility(View.GONE);
                speechRecognitionService.createAndStart(this, getLanguage(), resultConsumer(), errorConsumer());
            }
            recoding.set(!recoding.get());
        });
        binding.actionNext.setOnClickListener(v -> nextWord());
        binding.playWord.setOnClickListener(v -> playWord());
        if (detailsViewModel.getValue() == null) {
            nextWord();
        }
    }

    private String getLanguage() {
        return binding.language.getText().toString();
    }

    private void nextWord() {
        List<Word> words = ApkModule.getWords(this);
        if (!words.isEmpty()) {
            recordingStopped();
            Random random = new Random();
            Word word = words.get(random.nextInt(words.size()));
            detailsViewModel.updateWord(word, 0);
            binding.textResult.setVisibility(View.GONE);
        }
    }

    private void playWord() {
        AudioService audioService = ((ApplicationWithDI) getApplicationContext()).appComponent.audioService();
        ApkModule.playAsynchronousIfPossible(audioService, detailsViewModel.getValue().getLanguage(), detailsViewModel.getValue().getFullWord());
    }

    private Consumer<ArrayList<String>> resultConsumer() {
        return result -> {
            recordingStopped();
            if (result.size() != 1 || !result.get(0).equalsIgnoreCase(detailsViewModel.getValue().getFullWord())) {
                binding.textResult.setVisibility(View.VISIBLE);
                binding.textResult.setText(result.stream().collect(Collectors.joining(System.lineSeparator())));
            } else {
                Toast.makeText(this, R.string.correct, Toast.LENGTH_SHORT).show();
                nextWord();
            }
        };
    }

    private void recordingStopped() {
        binding.buttonSpeak.setImageDrawable(AppCompatResources.getDrawable(this, drawable.ic_btn_speak_now));
        recoding.set(false);
    }

    private Consumer<String> errorConsumer() {
        return result -> {
            recordingStopped();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        speechRecognitionService.destroy();
    }
}
