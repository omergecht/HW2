package il.ac.tau.cs.software1.bigram;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


/*
 * A small tester you can run locally (it is NOT graded, and it is NOT what GitHub Actions runs).
 * It prints an error code for every check that fails, and "done!" at the end.
 * Run it from the root of the repository, so that the relative paths below are found.
 * Feel free to add checks of your own.
 */
public class BigramModelTester {
    public static final String ALL_YOU_NEED_FILENAME = "resources/bigram/all_you_need.txt";
    public static final String ALL_YOU_NEED_MODEL_DIR = "resources/bigram/all_you_need_model";

    // Whole books (all of them are in the public domain). Add your own!
    // Moby Dick is the only one with more than MAX_VOCABULARY_SIZE different words;
    // its model needs about 1 GB of memory, so remove it from the list if you get an OutOfMemoryError.
    public static final String[] BOOKS = {
        "resources/bigram/emma.txt",
        "resources/bigram/alice.txt",
        "resources/bigram/sense_and_sensibility.txt",
        "resources/bigram/the_man_who_was_thursday.txt",
        "resources/bigram/moby_dick.txt",
    };
    public static final String[] WORDS_TO_TRY = {"good", "he", "said", "man", "day"};

    // MY own test class

// ניתן להדביק בתוך BigramModelTester ולהריץ מתוך main
public class LocalBigramTests {

    public static void runAllTests() throws IOException {
        Path tempDir = Files.createTempDirectory("bigram_tests");
        try {
            q1_basicAndNormalization(tempDir);
            q1_emptyAndOnlyIllegalWords(tempDir);
            q1_maxVocabularyLimit(tempDir);
            q2_lineBreaksAndIllegalWordsBreakPairs(tempDir);
            q2_wordsExcludedFromVocabBreakPairs(tempDir);
            q3_saveModelFormatAndTrailingNewlines(tempDir);
            q4_loadModelReplacesExistingModelAndCreatesNewArrays(tempDir);
            q5_isLegalSentenceEdgeCases(tempDir);
            System.out.println("ALL Q1-Q5 TESTS PASSED SUCCESSFULLY!");
        } finally {
            // ניקוי הקבצים הזמניים
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> p.toFile().delete());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Test failed: " + message);
        }
    }

    private static void q1_basicAndNormalization(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q1_basic.txt");
        Files.write(input, List.of("Love love LOVE! 1984 13", "man man's --!!- 3.14 -5"));

        BigramModel model = new BigramModel();
        String[] vocab = model.buildVocabularyIndex(input.toString());
        String[] expected = {"love", "love!", "some_num", "man", "man's"};
        check(Arrays.equals(expected, vocab), "q1_basic: got " + Arrays.toString(vocab));
    }

    private static void q1_emptyAndOnlyIllegalWords(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q1_empty.txt");
        Files.write(input, List.of("   ", "!!! 12.34 -99 @#$"));

        BigramModel model = new BigramModel();
        String[] vocab = model.buildVocabularyIndex(input.toString());
        check(vocab.length == 0, "q1_empty: expected length 0");
    }

    private static void q1_maxVocabularyLimit(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q1_max.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15000; i++) {
            sb.append("word").append(i).append(" ");
        }
        Files.writeString(input, sb.toString());

        BigramModel model = new BigramModel();
        String[] vocab = model.buildVocabularyIndex(input.toString());
        check(vocab.length == 14500 && vocab[0].equals("word0") && vocab[14499].equals("word14499"), "q1_max failed");
    }

    private static void q2_lineBreaksAndIllegalWordsBreakPairs(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q2_pairs.txt");
        Files.write(input, List.of("apple banana", "cherry apple 3.14 banana", "banana   cherry"));

        BigramModel model = new BigramModel();
        String[] vocab = model.buildVocabularyIndex(input.toString());
        int[][] counts = model.buildCountsArray(input.toString(), vocab);

        check(counts[0][1] == 1 && counts[2][0] == 1 && counts[1][2] == 1 && counts[0][0] == 0, "q2_pairs failed");
    }

    private static void q2_wordsExcludedFromVocabBreakPairs(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q2_oob.txt");
        Files.write(input, List.of("first excluded second"));

        BigramModel model = new BigramModel();
        String[] partialVocab = {"first", "second"};
        int[][] counts = model.buildCountsArray(input.toString(), partialVocab);
        check(counts[0][1] == 0, "q2_oob failed");
    }

    private static void q3_saveModelFormatAndTrailingNewlines(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q3_in.txt");
        Files.write(input, List.of("a b a c"));

        BigramModel model = new BigramModel();
        model.initModel(input.toString());

        String basePath = tempDir.resolve("my_model").toString();
        model.saveModel(basePath);

        String rawVoc = Files.readString(Path.of(basePath + ".voc"));
        String rawCounts = Files.readString(Path.of(basePath + ".counts"));
        check(rawVoc.endsWith("\n") && !rawVoc.endsWith("\n\n"), "q3 voc trailing newline failed");
        check(rawCounts.endsWith("\n") && !rawCounts.endsWith("\n\n"), "q3 counts trailing newline failed");

        check(Files.readAllLines(Path.of(basePath + ".voc")).equals(List.of("3 words", "0,a", "1,b", "2,c")), "q3 voc content failed");
        check(Files.readAllLines(Path.of(basePath + ".counts")).equals(List.of("0,1:1", "0,2:1", "1,0:1")), "q3 counts content failed");
    }

    private static void q4_loadModelReplacesExistingModelAndCreatesNewArrays(Path tempDir) throws IOException {
        String basePath = tempDir.resolve("manual_model").toString();
        Files.writeString(Path.of(basePath + ".voc"), "2 words\n0,hello\n1,world\n");
        Files.writeString(Path.of(basePath + ".counts"), "0,1:5\n1,0:2\n");

        BigramModel model = new BigramModel();
        Path dummyInput = tempDir.resolve("dummy.txt");
        Files.writeString(dummyInput, "x y z");
        model.initModel(dummyInput.toString());

        String[] oldVocabRef = model.mVocabulary;
        int[][] oldCountsRef = model.mBigramCounts;

        model.loadModel(basePath);

        check(oldVocabRef != model.mVocabulary && oldCountsRef != model.mBigramCounts, "q4 array reference replacement failed");
        check(Arrays.equals(new String[]{"hello", "world"}, model.mVocabulary), "q4 vocab load failed");
        check(model.mBigramCounts[0][1] == 5 && model.mBigramCounts[1][0] == 2 && model.mBigramCounts[0][0] == 0, "q4 counts load failed");
    }

    private static void q5_isLegalSentenceEdgeCases(Path tempDir) throws IOException {
        Path input = tempDir.resolve("q5_in.txt");
        Files.write(input, List.of("the cat sat", "sat on the mat"));

        BigramModel model = new BigramModel();
        model.initModel(input.toString());

        check(model.isLegalSentence(""), "q5 empty failed");
        check(model.isLegalSentence("cat") && !model.isLegalSentence("dog"), "q5 single word failed");
        check(model.isLegalSentence("the cat sat on the mat") && model.isLegalSentence("sat on the cat"), "q5 valid chains failed");
        check(!model.isLegalSentence("cat mat") && !model.isLegalSentence("the sat") && !model.isLegalSentence("the dog sat"), "q5 invalid sentences failed");
    }
}

    public static void main(String[] args) throws IOException {
        // MY own test
        LocalBigramTests.runAllTests();
        // End of my own test

        BigramModel sG = new BigramModel();
        String[] voc = sG.buildVocabularyIndex(ALL_YOU_NEED_FILENAME);
        if (voc.length != 5) {
            System.out.println("Error 1.1");
        }
        if (!Arrays.equals(voc, new String[]{"love", "all", "you", "need", "is"})) {
            System.out.println("Error 1.2");
        }

        int[][] counts = sG.buildCountsArray(ALL_YOU_NEED_FILENAME, voc);
        if (counts[0][0] != 3) { // count of "love love"
            System.out.println("Error 2.1");
        }
        if (counts[1][2] != 3) { // count of "all you"
            System.out.println("Error 2.2");
        }

        sG.initModel(ALL_YOU_NEED_FILENAME);
        sG.saveModel(ALL_YOU_NEED_MODEL_DIR);
        sG.loadModel(ALL_YOU_NEED_MODEL_DIR);

        if (!Arrays.equals(sG.mVocabulary, new String[]{"love", "all", "you", "need", "is"})) {
            System.out.println("Error 4.1");
        }
        if (sG.mBigramCounts[0][0] != 3) { // count of "love love"
            System.out.println("Error 4.2");
        }
        if (sG.mBigramCounts[1][2] != 3) { // count of "all you"
            System.out.println("Error 4.3");
        }

        if (!sG.isLegalSentence("love is all")) {
            System.out.println("Error 5.1");
        }
        if (sG.isLegalSentence("love is is")) {
            System.out.println("Error 5.2");
        }
        if (sG.isLegalSentence("love the beatles")) {
            System.out.println("Error 5.3");
        }

        // The methods below are already implemented, but they rely on your model
        if (sG.getWordIndex("love") != 0) {
            System.out.println("Error 6.1");
        }
        if (sG.getBigramCount("is", "love") != 2) {
            System.out.println("Error 6.2");
        }
        if (sG.getBigramCount("strawberry", "fields") != 0) {
            System.out.println("Error 6.3");
        }
        if (!"love".equals(sG.getMostFrequentProceeding("is"))) {
            System.out.println("Error 6.4");
        }

        // Whole books: this may take a while.
        // For example, in Emma the closest word to "good" should be "great".
        for (String book : BOOKS) {
            sG.initModel(book);
            System.out.println(book + ": " + sG.mVocabulary.length + " words in the vocabulary");
            for (String word : WORDS_TO_TRY) {
                if (sG.getWordIndex(word) != BigramModel.ELEMENT_NOT_FOUND) {
                    System.out.println("    closest word to '" + word + "': " + sG.getClosestWord(word));
                }
            }
        }

        System.out.println("done!");
    }
}
