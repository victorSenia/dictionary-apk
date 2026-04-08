package org.leo.dictionary.apk.grammar.provider;

import android.content.Context;
import android.net.Uri;
import org.leo.dictionary.grammar.provider.FileSentenceProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class InputStreamSentenceProvider extends FileSentenceProvider {
    private Context context;
    private Uri uri;

    @Override
    protected BufferedReader getBufferedReader() throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) {
            throw new FileNotFoundException("Cannot open: " + uri);
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    public void setSource(Context context, Uri uri) {
        this.context = context.getApplicationContext();
        this.uri = uri;
        this.sentences = null;
        this.topics = null;
    }
}
