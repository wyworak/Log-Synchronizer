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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    /**
     * Resampling a log file creating a new file based on specific samples.
     *
     * @param csvData      the original CSV log.
     * @param sample       the number of samples that will be retrieved from log.
     * @param tokenStart   the columnMarker of game starts.
     * @param tokenEnd     the columnMarker of game ends.
     * @param columnMarker the column with marker.
     * @return a new CSV log.
     */
    protected List<String[]> logResampling(
            final List<String[]> csvData,
            final double sample,
            final String tokenStart,
            final String tokenEnd,
            final int columnMarker
    ) {
        final List<String[]> resampledData = new ArrayList<>();
        final String[] headers = csvData.get(0);
        double sampleCounter = 0;
        boolean canWrite = false;
        boolean isHeader = true;

        int i = 0;
        do {
            final String[] aCsvData = csvData.get(i);
            if (aCsvData.length > columnMarker) {
                if (aCsvData[columnMarker].equals(tokenStart)) {
                    canWrite = true;
                }
                if (aCsvData[columnMarker].equals(tokenEnd)) {
                    canWrite = false;
                }
                if (canWrite) {
                    if (i != Math.round(sampleCounter)) {
                        i++;
                        continue;
                    }
                    if (isHeader) {
                        resampledData.add(headers);
                        isHeader = false;
                    }
                    resampledData.add(aCsvData);
                    sampleCounter += sample;
                } else {
                    sampleCounter++;
                }
            }
            i++;
        } while (i < csvData.size());

        return resampledData;
    }

    protected List<String[]> joinLogFiles(final List<String[]> logA,
                                          final List<String[]> logB) {
        final int linesCount = Math.max(logA.size(), logB.size());
        List<String[]> logResult = new ArrayList<>();
        for (int i = 0; i < linesCount; i++) {
            final String[] result = Stream.of(logA.get(i), logB.get(i))
                    .flatMap(Stream::of)
                    .toArray(String[]::new);
            logResult.add(result);
        }
        return logResult;
    }

    protected List<String[]> joinLogFilesLineOffset(final List<String[]> logA,
                                                    final List<String[]> logB,
                                                    final int offset) {
        final int linesCount = Math.max(logA.size(), logB.size());
        final List<String[]> logResult = new ArrayList<>();

        for (int i = 0; i < linesCount; i++) {
            final String[] emptyLog = new String[logB.get(0).length];
            final String[] result =
                    Stream.of(logA.get(i), i == 0 ? logB.get(i) : ((i - offset) >= logB.size() ? emptyLog : (i > offset ? logB.get(i - offset) : emptyLog)))
                            .flatMap(Arrays::stream)
                            .toArray(String[]::new);
            logResult.add(result);
        }
        return logResult;
    }

    protected List<String[]> joinLogFilesLineOffsetWithIds(final List<String[]> logA,
                                                           final List<String[]> logB,
                                                           final int offset,
                                                           final String logId,
                                                           final String playerId) {
        final List<String[]> tempLog = new ArrayList<>();
        final String[] header = new String[]{"log_id", "player_id"};
        final String[] idsLogAndPlayer = new String[]{logId, playerId};
        final int logLines = logA.size();

        for (int i = 0; i < logLines; i++) {
            final String[] result = Stream.of(i == 0 ? header : idsLogAndPlayer, logA.get(i))
                    .flatMap(Arrays::stream)
                    .toArray(String[]::new);
            tempLog.add(result);
        }

        return joinLogFilesLineOffset(tempLog, logB, offset);
    }

    protected List<String[]> joinLogFilesLineOffsetAndLabels(final List<String[]> logA,
                                                             final List<String[]> logB,
                                                             final String[] labels,
                                                             final int offset) {
        logB.add(0, labels);
        return joinLogFilesLineOffset(logA, logB, offset);
    }

    private String stringToDecimalValue(final String value) {
        return value.replace(".", ",");
    }

    protected boolean writeCsvFile(final List<String[]> data,
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
            return true;
        }
    }

    protected List<String[]> removeNonUsedColumns(
            final List<String[]> csvData,
            final List<Integer> usedColumns) {
        final List<String[]> logResult = new ArrayList<>();

        for (int i = 0; i < csvData.size(); i++) {
            String[] originalColumns = csvData.get(i);
            String[] columns = new String[usedColumns.size()];
            boolean needAdd = false;
            int index = 0;
            for (int j = 0; j < originalColumns.length; j++) {
                if (usedColumns.contains(j)) {
                    columns[index] = originalColumns[j];
                    index++;
                    needAdd = true;
                }
            }
            if (needAdd) {
                logResult.add(columns);
            }
        }
        return logResult;
    }

    protected List<String[]> removeNonUsedLinesAtStartEndLog(
            final List<String[]> csvData,
            final int lines,
            final int markerColumn) {
        final List<String[]> logResult = new ArrayList<>(csvData);
        boolean isFirstBlock = true;
        int startLines = 0;
        int endLines = 0;

        for (int i = 1; i < csvData.size(); i++) {
            final String[] line = csvData.get(i);
            if (line.length > markerColumn) {
                if (line[markerColumn] == null) {
                    if (isFirstBlock) {
                        startLines++;
                    } else {
                        endLines++;
                    }
                } else {
                    isFirstBlock = false;
                }
            } else {
                isFirstBlock = false;
            }
        }
        int i = 1;
        while (i <= (startLines - lines)) {
            logResult.remove(1);
            i++;
        }

        i = 1;
        while (i <= (endLines - lines)) {
            logResult.remove(logResult.size() - 1);
            i++;
        }

        return logResult;
    }

    protected List<Double> extractColumn(final List<String[]> csvData, final int columnIndex) {
        List<Double> list = new ArrayList<>();
        int bound = csvData.size();
        for (int i = 1; i < bound; i++) {
            if (csvData.get(i).length > columnIndex && Objects.nonNull(csvData.get(i)[columnIndex])) {
                Double aDouble = new Double(csvData.get(i)[columnIndex]);
                list.add(aDouble);
            }
        }
        return list;
    }

    protected List<String[]> normalizeColumn(final List<Double> column) {
        final double max = Collections.max(column);
        final double min = Collections.min(column);
        //(lineValue-min) / (max-min)
        return column.stream()
                .map(value -> new String[]{String.valueOf((float) (value - min) / (float) (max - min))})
                .collect(Collectors.toList());
    }

    protected List<String[]> extractMinimumValueFromColumnsLines(final List<List<Double>> columns) {
        final List<String[]> result = new ArrayList<>();
        final List<Double> temp = new ArrayList<>();
        int totalColumnItems = 0;
        for (int i = 0; i < columns.size(); i++) {
            if (i == 0) {
                totalColumnItems = columns.get(i).size();
            } else {
                if (totalColumnItems != columns.get(i).size())
                    throw new RuntimeException("The columns doesn't have the same size.");
            }
        }

        for (int i = 0; i < totalColumnItems; i++) {
            for (int j = 0; j < columns.size(); j++) {
                temp.add(columns.get(j).get(i));
            }
            result.add(new String[]{String.valueOf(Collections.min(temp))});
            temp.clear();
        }
        return result;
    }
}
