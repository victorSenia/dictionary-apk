package org.leo.dictionary.apk.word.provider;

import org.leo.dictionary.word.provider.FileWordProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class InputStreamWordProvider extends FileWordProvider {
    private InputStream inputStream;

    @Override
    protected BufferedReader getBufferedReader() {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        words = null;
    }
}