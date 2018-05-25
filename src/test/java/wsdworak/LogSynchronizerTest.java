package wsdworak;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.*;

public class LogSynchronizerTest {

    private static final String CSV_FILE = "template-log.csv";

    final URL resource = getClass().getClassLoader().getResource(CSV_FILE);
    final LogSynchronizer logSynchronizer = new LogSynchronizer();

    @Test
    public void testReadAllItensSuccess() throws IOException {
        final List<String[]> csvData = logSynchronizer.readData(resource.getFile());
        Assert.assertEquals(csvData.size(), 3001);
    }

    @Test
    public void testName() throws IOException {
        final List<String[]> csvData = logSynchronizer.readData(resource.getFile());
        final List<String[]> resampledData = logSynchronizer.logResampling(csvData, 16, "13", "14", 6);
        for (int i = 0; i < resampledData.size(); i++) {
            System.out.println(resampledData.get(i)[0]);
        }
    }
}