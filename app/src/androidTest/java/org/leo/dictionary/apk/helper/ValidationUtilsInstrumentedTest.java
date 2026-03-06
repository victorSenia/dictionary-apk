package org.leo.dictionary.apk.helper;

import android.app.Instrumentation;
import android.content.Context;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.leo.dictionary.apk.R;

@RunWith(AndroidJUnit4.class)
public class ValidationUtilsInstrumentedTest {

    @Test
    public void isEmptySetEmptyErrorIfNot_returnsFalseAndSetsErrorForBlankText() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        AtomicBoolean result = new AtomicBoolean(true);
        AtomicReference<CharSequence> error = new AtomicReference<>();

        instrumentation.runOnMainSync(() -> {
            EditText editText = new EditText(context);
            editText.setText("   ");
            result.set(ValidationUtils.isEmptySetEmptyErrorIfNot(editText));
            error.set(editText.getError());
        });

        assertFalse(result.get());
        assertEquals(context.getString(R.string.cannot_be_empty), String.valueOf(error.get()));
    }

    @Test
    public void isEmptySetEmptyErrorIfNot_returnsTrueAndClearsErrorForNonBlankText() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        AtomicBoolean result = new AtomicBoolean(false);
        AtomicReference<CharSequence> error = new AtomicReference<>();

        instrumentation.runOnMainSync(() -> {
            EditText editText = new EditText(context);
            editText.setError("old error");
            editText.setText("word");
            result.set(ValidationUtils.isEmptySetEmptyErrorIfNot(editText));
            error.set(editText.getError());
        });

        assertTrue(result.get());
        assertNull(error.get());
    }
}
