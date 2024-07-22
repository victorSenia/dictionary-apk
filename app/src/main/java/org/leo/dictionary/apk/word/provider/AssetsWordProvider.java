package org.leo.dictionary.apk.word.provider;

import android.content.Context;
import org.leo.dictionary.word.provider.FileWordProvider;
import org.leo.dictionary.word.provider.WordExporter;
import org.leo.dictionary.word.provider.WordImporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AssetsWordProvider extends FileWordProvider {

    public static final String ASSETS_WORDS = "words/";
    public static final String PARSE_WORDS_CONFIGURATION = "org.leo.dictionary.config.entity.ParseWords";
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(context.getAssets().open(ASSETS_WORDS + configuration.getPath()), StandardCharsets.UTF_8));
    }

    @Override
    protected boolean isIgnoredLine(String line) {
        return super.isIgnoredLine(line) || isConfigurationLine(line);
    }

    private static boolean isConfigurationLine(String line) {
        return line.startsWith(PARSE_WORDS_CONFIGURATION);
    }

    public void parseAndUpdateConfiguration() {
        try (BufferedReader fileReader = getBufferedReader()) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                if (isConfigurationLine(line)) {
                    String[] configParts = line.split(WordExporter.MAIN_DIVIDER, -1);
                    if (configParts.length < 9) {
                        throw new IllegalArgumentException("config incorrect " + line);
                    }
                    configuration.setLanguageFrom(decode(configParts[1]));
                    configuration.setLanguagesTo(parseListProperty(configParts[2]));
                    configuration.setArticles(parseListProperty(configParts[3]));
                    configuration.setDelimiter(decode(configParts[4]));
                    configuration.setAdditionalInformationDelimiter(decode(configParts[5]));
                    configuration.setTranslationDelimiter(decode(configParts[6]));
                    configuration.setTopicFlag(decode(configParts[7]));
                    configuration.setTopicDelimiter(decode(configParts[8]));
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String decode(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        return WordImporter.decode(string);
    }

    private static List<String> parseListProperty(String configPart) {
        return Arrays.stream(configPart.split(WordExporter.PARTS_DIVIDER)).map(AssetsWordProvider::decode).collect(Collectors.toList());
    }

    public String configurationToLine() {
        return String.join(WordExporter.MAIN_DIVIDER, new String[]{
                PARSE_WORDS_CONFIGURATION,
                encode(configuration.getLanguageFrom()),
                listPropertyToString(configuration.getLanguagesTo()),
                listPropertyToString(configuration.getArticles()),
                encode(configuration.getDelimiter()),
                encode(configuration.getAdditionalInformationDelimiter()),
                encode(configuration.getTranslationDelimiter()),
                encode(configuration.getTopicFlag()),
                encode(configuration.getTopicDelimiter())
        });
    }


    private static String encode(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        return WordExporter.encode(string);
    }

    private static String listPropertyToString(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        return strings.stream().map(AssetsWordProvider::encode).collect(Collectors.joining(WordExporter.PARTS_DIVIDER));
    }
}
