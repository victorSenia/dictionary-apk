package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;

import java.util.List;

public class TopicsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.topics_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        findViewById(R.id.all_topics).setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    public static class TopicsFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            PlayService playService = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.playService();
            return playService.findTopics();
        }

        @Override
        protected ReturnSelectedStringRecyclerViewAdapter getRecyclerViewAdapter() {
            return new ReturnSelectedStringRecyclerViewAdapter(getStrings(), this);
        }
    }
}