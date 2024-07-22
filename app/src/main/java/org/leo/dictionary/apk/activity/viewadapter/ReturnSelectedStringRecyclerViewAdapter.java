package org.leo.dictionary.apk.activity.viewadapter;

import android.app.Activity;
import android.content.Intent;
import androidx.fragment.app.Fragment;

import java.util.List;

public class ReturnSelectedStringRecyclerViewAdapter extends StringRecyclerViewAdapter<String> {

    public static final String DATA_STRING_EXTRA = "selectedValue";

    public ReturnSelectedStringRecyclerViewAdapter(List<String> items, Fragment fragment) {
        super(items, fragment, getOnClickListener(fragment));
    }

    private static OnClickListener<String> getOnClickListener(Fragment fragment) {
        return new OnClickListener<String>() {
            @Override
            public void onClick(StringViewHolder<String> viewHolder) {
                Intent intent = new Intent();
                intent.putExtra(DATA_STRING_EXTRA, viewHolder.valueToString());
                fragment.requireActivity().setResult(Activity.RESULT_OK, intent);
                fragment.requireActivity().finish();
            }
        };
    }

}