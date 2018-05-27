package wsdworak;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Main {

    /*
     * A3 EDA
     * 13 14 15 marcas onde começa o PacMan
     */
    private static final String CSV_FILE = "T01_1_BITALINO.csv";


    public static void main(String[] args) throws IOException {
        final URL resource = Main.class.getClassLoader().getResource(CSV_FILE);
        final LogSynchronizer logSynchronizer = new LogSynchronizer();
        final List<String[]> csvData = logSynchronizer.readData(resource.getFile(), false);
        final List<String[]> resampledData = logSynchronizer.logResampling(csvData, 16, "13", "14", 13);

        for (int i = 0; i < resampledData.size(); i++) {
            final String x = resampledData.get(i)[9];
            System.out.println(x);
        }
        System.out.println(resampledData.size());
    }
}
