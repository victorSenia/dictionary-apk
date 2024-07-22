package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import androidx.fragment.app.Fragment;

import java.util.List;

public class ReturnSelectedStringRecyclerViewAdapter extends StringRecyclerViewAdapter {

    public static final String DATA_STRING_EXTRA = "selectedValue";

    public ReturnSelectedStringRecyclerViewAdapter(List<String> items, Fragment fragment) {
        super(items, fragment, getOnClickListener(fragment));
    }

    private static OnClickListener getOnClickListener(Fragment fragment) {
        return new OnClickListener() {
            @Override
            public void onClick(StringViewHolder viewHolder) {
                Intent intent = new Intent();
                intent.putExtra(DATA_STRING_EXTRA, viewHolder.mItem);
                fragment.requireActivity().setResult(Activity.RESULT_OK, intent);
                fragment.requireActivity().finish();
            }
        };
    }

}