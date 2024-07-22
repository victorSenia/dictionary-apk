package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.fragment.app.Fragment;

import java.util.List;

public class ReturnSelectedStringRecyclerViewAdapter extends StringRecyclerViewAdapter {

    public static final String DATA_STRING_EXTRA = "selectedValue";

    public ReturnSelectedStringRecyclerViewAdapter(List<String> items, Fragment fragment) {
        super(items, fragment);
    }

    @Override
    public View.OnClickListener getOnClickListener(ViewHolder viewHolder) {
        return v -> {
            Intent intent = new Intent();
            intent.putExtra(DATA_STRING_EXTRA, viewHolder.mItem);
            fragment.getActivity().setResult(Activity.RESULT_OK, intent);
            fragment.getActivity().finish();
        };
    }
}