package wsdworak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogSynchronizer {

    private static final Logger LOG = LoggerFactory.getLogger(LogSynchronizer.class);

    protected List<String[]> readData(String file) throws IOException {
        List<String[]> content = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.add(line.split(";"));
            }
        } catch (FileNotFoundException e) {
            //Some error logging
        }
        return content;
    }

    protected List<String[]> logResampling(List<String[]> csvData, int sample, String tokenStart, String tokenEnd, int column) {
        int sampleCounter = 0;
        boolean canWrite = false;
        List<String[]> resampledData = new ArrayList<>();
        for (int i = 0; i < csvData.size(); i++) {
            String[] aCsvData = csvData.get(i);
            if (aCsvData[column].equals(tokenStart)) {
                canWrite = true;
            }
            if (aCsvData[column].equals(tokenEnd)) {
                canWrite = false;
            }
            if (canWrite) {
                if (i != sampleCounter) {
                    continue;
                }
                resampledData.add(aCsvData);
                sampleCounter += sample;
            } else {
                sampleCounter++;
            }
        }
        return resampledData;
    }
}
