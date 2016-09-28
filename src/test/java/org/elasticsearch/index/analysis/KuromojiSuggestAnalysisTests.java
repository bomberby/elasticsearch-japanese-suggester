package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.plugin.JapaneseSuggesterPlugin;
import org.elasticsearch.test.ESTestCase;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

public class KuromojiSuggestAnalysisTests extends ESTestCase {
    private static AnalysisService analysisService;

    private static final String INPUT = "シュークリーム";
    // Original + key stroke variations.
    private static final List<String> KEY_STROKES = Arrays.asList(
            "シュークリーム", "syu-kuri-mu", "shu-kuri-mu", "sixyu-kuri-mu", "shixyu-kuri-mu");

    @BeforeClass
    public static void createAnalysisService() throws IOException {
        Settings settings = Settings.builder()
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();
        Path home = createTempDir();

        Settings nodeSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), home).build();
        analysisService = createAnalysisService(new Index("test", "_na_"), nodeSettings, settings, new JapaneseSuggesterPlugin());
    }

    public void testSimpleSearchAnalyzer() throws IOException {
        testTokenization(getSearchAnalyzer(), INPUT, KEY_STROKES.subList(0, 2));
    }

    public void testDedupInput() throws IOException {
        testTokenization(getIndexAnalyzer(), "aa", Collections.singletonList("aa"));
    }

    public void testKanjiAndAlphaNumeric() throws IOException {
        testTokenization(getIndexAnalyzer(), "2015年", Arrays.asList("2015年", "2015nen"));
        testTokenization(getIndexAnalyzer(), "第138回", Arrays.asList("第138回", "dai138kai"));
        testTokenization(getIndexAnalyzer(), "A型", Arrays.asList("a型", "agata"));
    }

    public void testExpansionOrder() throws IOException {
        testTokenization(getIndexAnalyzer(),
                "ジョジョ",
                Arrays.asList("jojo", "jozyo", "zyojo", "jixyojo", "jojixyo", "zyozyo", "jixyozyo", "zyojixyo", "jozixyo", "zixyojo",
                        "zyozixyo", "jixyojixyo", "zixyozyo", "zixyojixyo", "jixyozixyo", "zixyozixyo", "ジョジョ"),
                true);

        testTokenization(getSearchAnalyzer(),
                "ジョジョ",
                Arrays.asList("jojo", "ジョジョ"),
                true);

        testTokenization(getSearchAnalyzer(),
                "あいう",
                Arrays.asList("aiu", "あいう"),
                true);

    }

    private void testTokenization(Analyzer analyzer, String input, List<String> expected) throws IOException {
        testTokenization(analyzer, input, expected, false);
    }

    private void testTokenization(Analyzer analyzer, String input, List<String> expected, boolean ordered) throws IOException {
        TokenStream stream = analyzer.tokenStream("dummy", input);
        List<String> result = readStream(stream);
        stream.close();
        if (ordered) {
            assertThat(result, equalTo(expected));
        } else {
            assertThat(new HashSet<>(result), equalTo(new HashSet<>(expected)));
        }
    }

    private List<String> readStream(TokenStream stream) throws IOException {
        stream.reset();

        List<String> result = new ArrayList<>();
        while (stream.incrementToken()) {
            result.add(stream.getAttribute(CharTermAttribute.class).toString());
        }

        return result;
    }

    private Analyzer getIndexAnalyzer() {
        return analysisService.analyzer(KuromojiSuggestAnalyzerProvider.INDEX_ANALYZER);
    }

    private Analyzer getSearchAnalyzer() {
        return analysisService.analyzer(KuromojiSuggestAnalyzerProvider.SEARCH_ANALYZER);
    }
}
