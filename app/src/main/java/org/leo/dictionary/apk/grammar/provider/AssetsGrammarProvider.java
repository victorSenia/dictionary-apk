package org.leo.dictionary.apk.grammar.provider;

import android.content.Context;
import org.leo.dictionary.grammar.provider.FileGrammarProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetsGrammarProvider extends FileGrammarProvider {

    public static final String ASSETS_GRAMMAR = "grammar/";
    private Context context;


    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(ASSETS_GRAMMAR + configuration.getPath()), StandardCharsets.UTF_8));
    }
}
