package org.leo.dictionary.apk.config;

import android.content.Context;
import org.leo.dictionary.config.FileConfigurationReader;
import org.leo.dictionary.config.entity.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetsConfigurationReader extends FileConfigurationReader {

    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void updateConfiguration(Configuration configuration) {
    }

    @Override
    public boolean isExists(String path) {
        return true;
    }

    @Override
    protected BufferedReader getBufferedReader(String path) throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(path), StandardCharsets.UTF_8));
    }

}
