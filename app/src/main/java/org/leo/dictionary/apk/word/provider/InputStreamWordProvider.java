package org.leo.dictionary.apk.word.provider;

import android.content.Context;
import android.net.Uri;
import org.leo.dictionary.word.provider.FileWordProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class InputStreamWordProvider extends FileWordProvider {
    private Context context;
    private Uri uri;

    @Override
    protected BufferedReader getBufferedReader() throws java.io.IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) throw new java.io.FileNotFoundException("Cannot open: " + uri);
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    public void setSource(Context context, Uri uri) {
        this.context = context.getApplicationContext();
        this.uri = uri;
        this.words = null;
        this.topics = null;
    }
}