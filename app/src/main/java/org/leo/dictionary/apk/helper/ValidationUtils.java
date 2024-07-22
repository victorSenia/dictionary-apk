package org.leo.dictionary.apk.helper;

import android.widget.EditText;

public abstract class ValidationUtils {
    public static boolean isEmptySetEmptyErrorIfNot(EditText text) {
        if (text.getText().toString().isEmpty()) {
            text.setError("Cannot be empty");
            return false;
        }
        return true;
    }
}
