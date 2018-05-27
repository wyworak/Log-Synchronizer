package wsdworak;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LogSynchronizerTest {

    private static final String LOG_A_CSV = "log-a.csv";
    private static final String LOG_B_CSV = "log-b.csv";
    private static final String LOG_FULL_CSV = "log-full.csv";

    private LogSynchronizer logSynchronizer;

    @BeforeMethod
    public void setUp() {
        logSynchronizer = new LogSynchronizer();
    }

    @Test
    public void testReadAllItensSuccess() throws IOException {
        final URL resource = getClass().getClassLoader().getResource(LOG_FULL_CSV);
        final List<String[]> csvData = logSynchronizer.readData(Objects.requireNonNull(resource).getFile(), false);
        Assert.assertEquals(csvData.size(), 4001);
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
    ) throws IOException {
        final URL resource = getClass().getClassLoader().getResource(LOG_FULL_CSV);
        final double sample = originalSamplingRate / targetSamplingRate;
        final List<String[]> csvData = logSynchronizer.readData(Objects.requireNonNull(resource).getFile(), false);
        final List<String[]> resampledData = logSynchronizer.logResampling(csvData, sample, tokenStart, tokenEnd, column);
        Assert.assertEquals(resampledData.size(), Math.round((originalSamplingRate / sample)));
    }

    @Test
    public void testRemoveDotsToCreateDecimalValuesSuccess() throws IOException {
        final URL resource = getClass().getClassLoader().getResource(LOG_B_CSV);
        final List<String[]> csvData = logSynchronizer.readData(Objects.requireNonNull(resource).getFile(), true);
        Assert.assertEquals(csvData.get(3)[5], "0,3");
        Assert.assertEquals(csvData.get(3)[6], "1,3");
        Assert.assertEquals(csvData.get(3)[7], "0,123456791");
    }

    @Test
    public void testJoinLogFilesSuccess() throws IOException {
        final URL resourceA = getClass().getClassLoader().getResource(LOG_A_CSV);
        final URL resourceB = getClass().getClassLoader().getResource(LOG_B_CSV);
        final List<String[]> csvDataA = logSynchronizer.readData(resourceA.getFile(), false);
        final List<String[]> csvDataB = logSynchronizer.readData(resourceB.getFile(), false);
        final List<String[]> resultCsv = logSynchronizer.joinLogFiles(csvDataA, csvDataB, 0);
        Assert.assertEquals(resultCsv.get(0)[1], "Item A");
        Assert.assertEquals(resultCsv.get(0)[9], "Item F");
        Assert.assertEquals(resultCsv.get(0)[17], "Value B");
        Assert.assertEquals(resultCsv.get(1)[0], "1");
        Assert.assertEquals(resultCsv.get(1)[1], "a 1");
        Assert.assertEquals(resultCsv.get(1)[9], "f 1");
        Assert.assertEquals(resultCsv.get(1)[17], "60");
    }

    @Test
    public void testWriteCsvFileSuccess() throws IOException {
        List<String[]> csvData = new ArrayList<>();
        csvData.add(new String[]{"Name", "Phone", "Country", "Weight"});
        csvData.add(new String[]{"Test 01", "111 222 333", "Brazil", "80.52"});
        csvData.add(new String[]{"Test 02", "112 223 334", "Portugal", "55.23"});
        csvData.add(new String[]{"Test 03", "113 224 335", "Germany", "71.56"});

        final String path = "target/file.csv";
        logSynchronizer.writeCsvFile(csvData, path);

        final List<String[]> csvDataLoaded = logSynchronizer.readData(path, false);

        Assert.assertEquals(csvDataLoaded.get(0)[0], "Name");
        Assert.assertEquals(csvDataLoaded.get(1)[1], "111 222 333");
        Assert.assertEquals(csvDataLoaded.get(2)[2], "Portugal");
        Assert.assertEquals(csvDataLoaded.get(3)[3], "71.56");
    }

}