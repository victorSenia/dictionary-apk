package org.leo.dictionary.apk.word.provider;

import android.content.Context;
import org.leo.dictionary.word.provider.FileWordProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetsWordProvider extends FileWordProvider {

    public static final String ASSETS_WORDS = "words/";
    private Context context;


    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(ASSETS_WORDS + configuration.getPath()), StandardCharsets.UTF_8));
    }

}
