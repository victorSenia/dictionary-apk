package org.leo.dictionary.apk.activity.viewmodel;

public class LanguageViewModel extends ObjectViewModel<String> {
    public void setSelected(CharSequence s) {
        postValue(s.toString());
    }
}
