package wsdworak;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogSynchronizerTest {

    private static final String LOG_A_CSV = "log-a.csv";
    private static final String LOG_B_CSV = "log-b.csv";
    private static final String LOG_C_CSV = "log-c.csv";
    private static final String LOG_D_CSV = "log-d.csv";
    private static final String LOG_FULL_CSV = "log-full.csv";
    private static final String LOG_COMPLETE_CSV = "log-complete.csv";
    private static final String LOG_TO_NORMALIZE_CSV = "log-to-normalize.csv";
    private static final String LOG_TO_EXTRACT_MINIMAL_CSV = "log-to-extract-minimal.csv";
    private static final String LOG_WITHOUT_LABEL_CSV = "log-without-label.csv";

    private LogSynchronizer victim;

    @BeforeMethod
    public void setUp() {
        victim = new LogSynchronizer();
    }

    @Test
    public void testReadAllItensSuccess() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_FULL_CSV);
        final List<String[]> result = victim.readData(Objects.requireNonNull(resource).getFile(), false);

        Assert.assertEquals(result.size(), 4001);
    }

    @Test
    public void testWriteCsvFileSuccess() throws Exception {
        final List<String[]> csvData = new ArrayList<>();
        csvData.add(new String[]{"Name", "Phone", "Country", "Weight"});
        csvData.add(new String[]{"Test 01", "111 222 333", "Brazil", "80.52"});
        csvData.add(new String[]{"Test 02", "112 223 334", "Portugal", "55.23"});
        csvData.add(new String[]{"Test 03", "113 224 335", "Germany", "71.56"});

        Assert.assertTrue(victim.writeCsvFile(csvData, "target/file.csv"));
    }

    @DataProvider
    public Object[][] dataProvider() {
        return new Object[][]{
                {"13", "14", 6, 1000, 60},
                {"14", "15", 6, 1000, 60},
                {"15", "16", 6, 1000, 60}
        };
    }

    @Test(dataProvider = "dataProvider")
    public void testReadAllResempledItensSuccess(
            final String tokenStart,
            final String tokenEnd,
            final int column,
            final double originalSamplingRate,
            final double targetSamplingRate
    ) throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_FULL_CSV);
        final double sample = originalSamplingRate / targetSamplingRate;
        final List<String[]> csvData = victim.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<String[]> result = victim.logResampling(csvData, sample, tokenStart, tokenEnd, column);

        Assert.assertEquals(result.size(), Math.round((originalSamplingRate / sample) + 1));
    }

    @Test
    public void testRemoveDotsToCreateDecimalValuesSuccess() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_B_CSV);
        final List<String[]> result = victim.readData(Objects.requireNonNull(resource).getFile(), true);

        Assert.assertEquals(result.get(3)[5], "0,3");
        Assert.assertEquals(result.get(3)[6], "1,3");
        Assert.assertEquals(result.get(3)[7], "0,123456791");
    }

    @Test
    public void testJoinLogFilesSuccess() throws Exception {
        final URL resourceA = getClass().getClassLoader().getResource(LOG_A_CSV);
        final URL resourceB = getClass().getClassLoader().getResource(LOG_B_CSV);
        final List<String[]> csvDataA = victim.readData(Objects.requireNonNull(resourceA).getFile(), false);
        final List<String[]> csvDataB = victim.readData(Objects.requireNonNull(resourceB).getFile(), false);
        final List<String[]> result = victim.joinLogFiles(csvDataA, csvDataB);

        Assert.assertEquals(result.get(0)[1], "Item A");
        Assert.assertEquals(result.get(0)[9], "Item F");
        Assert.assertEquals(result.get(0)[17], "Value B");
        Assert.assertEquals(result.get(1)[0], "1");
        Assert.assertEquals(result.get(1)[1], "a 1");
        Assert.assertEquals(result.get(1)[9], "f 1");
        Assert.assertEquals(result.get(1)[17], "60");
    }

    @Test
    public void testJoinLogFilesLineOffsetSuccess() throws Exception {
        final URL resourceA = getClass().getClassLoader().getResource(LOG_D_CSV);
        final URL resourceB = getClass().getClassLoader().getResource(LOG_C_CSV);
        final List<String[]> csvDataA = victim.readData(Objects.requireNonNull(resourceA).getFile(), false);
        final List<String[]> csvDataB = victim.readData(Objects.requireNonNull(resourceB).getFile(), false);
        final List<String[]> result = victim.joinLogFilesLineOffset(csvDataA, csvDataB, 10);

        Assert.assertEquals(result.get(0)[0], "Item A");
        Assert.assertEquals(result.get(0)[9], "Item L");
        Assert.assertEquals(result.get(33)[6], "i 23");
        Assert.assertEquals(result.get(50)[8], "1.40");
        Assert.assertEquals(result.get(60)[1], "b 60");
    }

    @Test
    public void testJoinLogFilesLineOffsetWithIdsSuccess() throws Exception {
        final URL resourceA = getClass().getClassLoader().getResource(LOG_D_CSV);
        final URL resourceB = getClass().getClassLoader().getResource(LOG_C_CSV);
        final List<String[]> csvDataA = victim.readData(Objects.requireNonNull(resourceA).getFile(), false);
        final List<String[]> csvDataB = victim.readData(Objects.requireNonNull(resourceB).getFile(), false);
        final List<String[]> result = victim.joinLogFilesLineOffsetWithIds(csvDataA, csvDataB, 10, "2", "3");

        Assert.assertEquals(result.get(0)[0], "log_id");
        Assert.assertEquals(result.get(0)[9], "Item J");
        Assert.assertEquals(result.get(33)[6], "g 23");
        Assert.assertEquals(result.get(50)[8], "i 40");
        Assert.assertEquals(result.get(60)[1], "3");
    }

    @Test
    public void testJoinLogFilesLineOffsetAndLabels() throws Exception {
        final URL resourceA = getClass().getClassLoader().getResource(LOG_COMPLETE_CSV);
        final URL resourceB = getClass().getClassLoader().getResource(LOG_WITHOUT_LABEL_CSV);
        final List<String[]> csvDataA = victim.readData(Objects.requireNonNull(resourceA).getFile(), false);
        final List<String[]> csvDataB = victim.readData(Objects.requireNonNull(resourceB).getFile(), false);
        final List<String[]> result = victim.joinLogFilesLineOffsetAndLabels(csvDataA, csvDataB, new String[]{"TEST"}, 10);

        Assert.assertEquals(result.get(0)[12], "TEST");
        Assert.assertEquals(result.get(12)[12], "0.1675");
        Assert.assertEquals(result.get(50)[12], "0.91675");
    }

    @Test
    public void testRemoveNonUsedColumns() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_A_CSV);
        final List<String[]> csvData = victim.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<String[]> result = victim.removeNonUsedColumns(csvData, Arrays.asList(1, 2));

        Assert.assertEquals(result.get(0)[0], "Item A");
        Assert.assertEquals(result.get(1)[1], "b 1");
        Assert.assertEquals(result.get(3)[1], "b 3");
        Assert.assertEquals(result.get(5)[0], "a 5");
    }

    @Test
    public void testRemoveNonUsedLinesAtStartEndLog() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_COMPLETE_CSV);
        final List<String[]> csvData = victim.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<String[]> result = victim.removeNonUsedLinesAtStartEndLog(csvData, 5, 4);

        Assert.assertEquals(result.get(0)[0], "log_id");
        Assert.assertEquals(result.get(0)[9], "Item J");
        Assert.assertEquals(result.get(33)[6], "g 23");
        Assert.assertEquals(result.get(50)[8], "i 40");
        Assert.assertEquals(result.get(60)[1], "3");
    }

    @Test
    public void testExtractColumn() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_TO_NORMALIZE_CSV);
        final List<String[]> csvData = victim.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<Double> result = victim.extractColumn(csvData, 1);

        Assert.assertEquals(result.get(0), 519.0);
        Assert.assertEquals(result.get(6), 521.0);
        Assert.assertEquals(result.get(59), 519.0);
    }

    @Test
    public void testNormalizeColumn() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_TO_NORMALIZE_CSV);
        final List<String[]> csvData = victim.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<Double> columnToNormalize = victim.extractColumn(csvData, 1);
        final List<String[]> result = victim.normalizeColumn(columnToNormalize);

        Assert.assertEquals(result.get(0)[0], "0.22970903");
        Assert.assertEquals(result.get(6)[0], "0.23277183");
        Assert.assertEquals(result.get(59)[0], "0.22970903");
    }

    @Test
    public void testExtractMinimumValueFromColumnsLines() throws Exception {
        final URL resource = getClass().getClassLoader().getResource(LOG_TO_EXTRACT_MINIMAL_CSV);
        final List<String[]> csvData = victim.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<List<Double>> columns = IntStream.range(0, 4).mapToObj(columnIndex -> victim.extractColumn(csvData, columnIndex)).collect(Collectors.toList());
        final List<String[]> result = victim.extractMinimumValueFromColumnsLines(columns);

        Assert.assertEquals(result.get(0)[0], "0.016752");
        Assert.assertEquals(result.get(6)[0], "0.004");
        Assert.assertEquals(result.get(59)[0], "0.116752");
    }
}