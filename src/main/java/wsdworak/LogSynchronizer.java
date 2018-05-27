package wsdworak;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LogSynchronizer {

    private static final Logger LOG = LoggerFactory.getLogger(LogSynchronizer.class);

    protected List<String[]> readData(
            final String file,
            final boolean hasDecimalContent
    ) throws IOException {
        List<String[]> content = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (hasDecimalContent) {
                    line = stringToDecimalValue(line);
                }
                content.add(line.split(";"));
            }
        }
        return content;
    }

    protected List<String[]> logResampling(
            final List<String[]> csvData,
            final double sample,
            final String tokenStart,
            final String tokenEnd,
            final int column
    ) {
        double sampleCounter = 0;
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
                if (i != Math.round(sampleCounter)) {
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

    protected List<String[]> joinLogFiles(final List<String[]> logA,
                                          final List<String[]> logB,
                                          final int newColumnIndex) {
        final int linesCount = Math.max(logA.size(), logB.size());
        List<String[]> resultLog = new ArrayList<>();
        for (int i = 0; i < linesCount; i++) {
            final String[] result = Stream.of(logA.get(i), logB.get(i))
                    .flatMap(Stream::of)
                    .toArray(String[]::new);
            resultLog.add(result);
        }
        return resultLog;
    }

    private String stringToDecimalValue(final String value) {
        return value.replace(".", ",");
    }

    protected void writeCsvFile(final List<String[]> data,
                              final String path)
            throws IOException {
        try (
                Writer writer = Files.newBufferedWriter(Paths.get(path));

                CSVWriter csvWriter = new CSVWriter(writer,
                        ';',
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END)
        ) {
            for (int i = 0; i < data.size(); i++) {
                csvWriter.writeNext(data.get(i));
            }
        }
    }
}
