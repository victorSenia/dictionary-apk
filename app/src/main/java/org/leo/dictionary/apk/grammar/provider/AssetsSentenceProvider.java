package org.leo.dictionary.apk.grammar.provider;

import android.content.Context;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;
import org.leo.dictionary.grammar.provider.FileSentenceProvider;
import org.leo.dictionary.word.provider.WordExporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.leo.dictionary.word.provider.FileWordProvider.decode;
import static org.leo.dictionary.word.provider.FileWordProvider.encode;


public class AssetsSentenceProvider extends FileSentenceProvider {
    public static final String ASSETS_SENTENCES = "sentences/";
    public static final String PARSE_SENTENCES_CONFIGURATION = "org.leo.dictionary.config.entity.ParseSentences";
    private Context context;

    private static boolean isConfigurationLine(String line) {
        return line.startsWith(PARSE_SENTENCES_CONFIGURATION);
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(ASSETS_SENTENCES + configuration.getPath()), StandardCharsets.UTF_8));
    }

    @Override
    protected boolean isIgnoredLine(String line) {
        return super.isIgnoredLine(line) || isConfigurationLine(line);
    }

    public void parseAndUpdateConfiguration() {
        try (BufferedReader fileReader = getBufferedReader()) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                if (isConfigurationLine(line)) {
                    String[] configParts = line.split(WordExporter.MAIN_DIVIDER, -1);
                    if (configParts.length < 6) {
                        throw new IllegalArgumentException("config incorrect " + line);
                    }
                    configuration.setLanguage(decode(configParts[1]));
                    configuration.setLanguagesTo(AssetsWordProvider.parseListProperty(configParts[2]));
                    configuration.setDelimiter(decode(configParts[3]));
                    configuration.setTranslationDelimiter(decode(configParts[4]));
                    configuration.setTopicFlag(decode(configParts[5]));
                    configuration.setRootTopic(decode(configParts[6]));
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String configurationToLine() {
        return String.join(WordExporter.MAIN_DIVIDER, new String[]{
                PARSE_SENTENCES_CONFIGURATION,
                encode(configuration.getLanguage()),
                AssetsWordProvider.listPropertyToString(configuration.getLanguagesTo()),
                encode(configuration.getDelimiter()),
                encode(configuration.getTranslationDelimiter()),
                encode(configuration.getTopicFlag()),
                encode(configuration.getRootTopic())
        });
    }
}
