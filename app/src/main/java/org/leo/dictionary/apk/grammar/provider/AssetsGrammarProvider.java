package org.leo.dictionary.apk.grammar.provider;

import android.content.Context;
import org.leo.dictionary.grammar.provider.FileGrammarProvider;
import org.leo.dictionary.word.provider.WordExporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.leo.dictionary.word.provider.FileWordProvider.decode;
import static org.leo.dictionary.word.provider.FileWordProvider.encode;

public class AssetsGrammarProvider extends FileGrammarProvider {

    public static final String ASSETS_GRAMMAR = "grammar/";
    public static final String PARSE_GRAMMAR_CONFIGURATION = "org.leo.dictionary.config.entity.ParseGrammar";
    private Context context;

    private static boolean isConfigurationLine(String line) {
        return line.startsWith(PARSE_GRAMMAR_CONFIGURATION);
    }


    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(ASSETS_GRAMMAR + configuration.getPath()), StandardCharsets.UTF_8));
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
                    if (configParts.length < 8) {
                        throw new IllegalArgumentException("config incorrect " + line);
                    }
                    configuration.setLanguage(decode(configParts[1]));
                    configuration.setDelimiter(decode(configParts[2]));
                    configuration.setPlaceholder(decode(configParts[3]));
                    configuration.setHintAtSameLine(Boolean.parseBoolean(configParts[4]));
                    configuration.setTopicFlag(decode(configParts[5]));
                    configuration.setHintFlag(decode(configParts[6]));
                    configuration.setRootTopic(decode(configParts[7]));
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String configurationToLine() {
        return String.join(WordExporter.MAIN_DIVIDER, new String[]{
                PARSE_GRAMMAR_CONFIGURATION,
                encode(configuration.getLanguage()),
                encode(configuration.getDelimiter()),
                encode(configuration.getPlaceholder()),
                Boolean.toString(configuration.getHintAtSameLine()),
                encode(configuration.getTopicFlag()),
                encode(configuration.getHintFlag()),
                encode(configuration.getRootTopic())
        });
    }
}
