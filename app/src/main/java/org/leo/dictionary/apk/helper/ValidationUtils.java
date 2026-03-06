package org.leo.dictionary.apk.helper;

import android.widget.EditText;
import org.leo.dictionary.apk.R;

public abstract class ValidationUtils {
    public static boolean isEmptySetEmptyErrorIfNot(EditText text) {
        if (text.getText().toString().trim().isEmpty()) {
            text.setError(text.getContext().getString(R.string.cannot_be_empty));
            return false;
        }
        text.setError(null);
        return true;
    }
}
