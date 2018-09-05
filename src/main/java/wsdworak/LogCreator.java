package wsdworak;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import wsdworak.entities.PlayerStartVideo;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogCreator {

    private static final int OFFSET = 240;

    private static final String LOGS_PATH = "logs/";
    private static final String RESOURCES_LOGS = "src/main/resources/logs/";
    private static final String PLAYER_LABEL = "/player_";
    private static final String LOG_LABEL = "_game_log_0";

    private static final String BITALINO_LOG = LOGS_PATH + "%s" + "/" + "%s" + "_BITALINO.csv";
    private static final String GAME_LOG_FILE = LOGS_PATH + "%s" + "/" + "%s" + "_game_log_0" + "%s" + ".csv";
    private static final String NEW_CSV_FILE = RESOURCES_LOGS + "%s" + "/" + "%s" + "_game_0" + "%s" + ".csv";
    private static final String FINAL_CSV_FILE = RESOURCES_LOGS + "%s" + PLAYER_LABEL + "%s" + "_game_log_0" + "%s" + ".csv";

    private static final String GAME_LOG_RESOURCE = LOGS_PATH + "%s" + PLAYER_LABEL + "%s" + LOG_LABEL + "%s.csv";
    private static final String GRAPHIC_GAME_LOG = RESOURCES_LOGS + "%s" + PLAYER_LABEL + "%s" + "_graphic" + LOG_LABEL + "%s.csv";
    private static final String GRAPHIC_GAME_IMG = RESOURCES_LOGS + "%s" + PLAYER_LABEL + "%s" + "_graphic_game_" + "%s";

    private static final int FRAME_RATE_GAME = 60;
    private static final int FRAME_RATE_SENSORS = 1000;
    private static final double NEW_SAMPLE = (double) FRAME_RATE_SENSORS / (double) FRAME_RATE_GAME;
    private static final int SECONDS_BEFORE_AND_AFTER_GAME_STARTS = 4;
    private static final int LINES_BEFORE_AND_AFTER_GAME_STARTS = FRAME_RATE_GAME * SECONDS_BEFORE_AND_AFTER_GAME_STARTS;
    private static final int COLUMN_GAME_START_FINAL_LOG = 6;

    private LogSynchronizer logSynchronizer;
    private List<PlayerStartVideo> players;

    public LogCreator() {
        this.logSynchronizer = new LogSynchronizer();
        try {
            players = loadPlayersStartVideos();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createBaseLogs()
            throws IOException {
        for (PlayerStartVideo player : players) {
            prepareBaseLogs(player);
        }
    }


    public void createCharts()
            throws IOException {
        for (PlayerStartVideo player : players) {
            createBitmapChart(player);
        }
    }

    private void prepareBaseLogs(final PlayerStartVideo player)
            throws IOException {
        final List<Integer> sensorsUsedColumns = new ArrayList<>(Arrays.asList(7, 8, 9, 10));
        final List<Integer> gameUsedColumns = new ArrayList<>(Arrays.asList(16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30));

        final URL fullLog = LogCreator.class.getClassLoader()
                .getResource(String.format(BITALINO_LOG, player.getName(), player.getName()));

        final List<String[]> csvSensorsFullLogData = logSynchronizer.readData(Objects.requireNonNull(fullLog).getFile(), false);

        final List<URL> gameLogList = IntStream.range(1, 4)
                .mapToObj(i -> LogCreator.class.getClassLoader()
                        .getResource(String.format(GAME_LOG_FILE, player.getName(), player.getName(), i)))
                .collect(Collectors.toList());

        String tokenStart[] = new String[]{"13", "14", "15"};
        String tokenEnd[] = new String[]{"14", "15", "1510"};

        for (int i = 0; i < gameLogList.size(); i++) {
            final int gameId = i + 1;
            System.out.println("Operation=prepareBaseLogs, player=" + player.getId() + ", game=" + gameId);
            final URL gameLogUrl = gameLogList.get(i);
            final List<String[]> csvGameDataLog = logSynchronizer.readData(Objects.requireNonNull(gameLogUrl).getFile(), false);
            final List<String[]> csvGameClearLogData = logSynchronizer.removeNonUsedColumns(csvGameDataLog, gameUsedColumns);
            final List<String[]> resampledData = logSynchronizer.logResampling(csvSensorsFullLogData, NEW_SAMPLE, tokenStart[i], tokenEnd[i], 13);
            final List<String[]> resampledClearData = logSynchronizer.removeNonUsedColumns(resampledData, sensorsUsedColumns);

            logSynchronizer.writeCsvFile(resampledClearData, String.format(NEW_CSV_FILE, player.getName(), player.getName(), gameId));

            final int offset = (int) (player.getStartGame()[i] * FRAME_RATE_GAME);

            final List<String[]> finalLog = logSynchronizer.joinLogFilesLineOffsetWithIds(resampledClearData, csvGameClearLogData, offset, String.valueOf(gameId), String.valueOf(player.getId()));

            logSynchronizer.writeCsvFile(
                    logSynchronizer.removeNonUsedLinesAtStartEndLog(finalLog, LINES_BEFORE_AND_AFTER_GAME_STARTS, COLUMN_GAME_START_FINAL_LOG),
                    String.format(FINAL_CSV_FILE, player.getName(), player.getId(), gameId));
        }
    }

    private void createBitmapChart(final PlayerStartVideo player)
            throws IOException {
        final List<List<String[]>> logsList = loadLogs(player);

        for (int i = 0; i < logsList.size(); i++) {
            final int gameId = i + 1;
            System.out.println("Operation=createBitmapChart, player=" + player.getId() + ", game=" + gameId);

            final List<String[]> csvData = logsList.get(i);
            final List<String[]> edaColumn = extractNormalizedEdaColumn(csvData);
            final List<String[]> ghostDiesColumn = extractGhostDiesColumn(csvData);
            final List<String[]> distancesColumn = extractNormalizedMinimumDistanceColumn(csvData);
            final List<String[]> powerfulColumn = extractPowerfulColumn(csvData);
            final List<String[]> gamePercentColumn = extractNormalizedGamePercent(csvData);

            edaColumn.add(0, new String[]{"EDA"});
            ghostDiesColumn.add(0, new String[]{"Any Ghost Dies"});
            distancesColumn.add(0, new String[]{"Distance"});
            powerfulColumn.add(0, new String[]{"Powerful"});
            gamePercentColumn.add(0, new String[]{"Complete"});


            List<String[]> ghostDiesAndDistance = logSynchronizer.joinLogFiles(ghostDiesColumn, distancesColumn);
            List<String[]> tempContent = logSynchronizer.joinLogFiles(ghostDiesAndDistance, powerfulColumn);
            List<String[]> gameContent = logSynchronizer.joinLogFiles(tempContent, gamePercentColumn);
            List<String[]> completeLog = logSynchronizer.joinLogFilesLineOffsetWithIds(edaColumn, gameContent, OFFSET, String.valueOf(gameId), String.valueOf(player.getId()));

            logSynchronizer.writeCsvFile(completeLog, String.format(GRAPHIC_GAME_LOG, player.getName(), String.valueOf(player.getId()), gameId));

            saveGraphic(createChart(completeLog, player.getName(), gameId), String.format(GRAPHIC_GAME_IMG, player.getName(), String.valueOf(player.getId()), gameId));
        }
    }

    private List<String[]> extractNormalizedEdaColumn(final List<String[]> csvData) {
        final List<Double> eda = logSynchronizer.extractColumn(csvData, 4);
        final List<String[]> normalizedEda = logSynchronizer.normalizeColumn(eda);
        normalizedEda.add(0, new String[]{"EDA"});
        final List<Double> edaList = logSynchronizer.extractColumn(normalizedEda, 0);

        return edaList.stream()
                .map(value -> new String[]{String.valueOf(value)})
                .collect(Collectors.toList());

    }

    private List<String[]> extractGhostDiesColumn(final List<String[]> csvData) {
        final List<Double> ghostDies = logSynchronizer.extractColumn(csvData, 6);
        return ghostDies.stream()
                .map(value -> new String[]{String.valueOf(Math.round(value))})
                .collect(Collectors.toList());
    }

    private List<String[]> extractNormalizedMinimumDistanceColumn(final List<String[]> csvData) {
        final List<List<Double>> distanceColumns = new ArrayList<>();
        distanceColumns.add(logSynchronizer.extractColumn(csvData, 7));
        distanceColumns.add(logSynchronizer.extractColumn(csvData, 8));
        distanceColumns.add(logSynchronizer.extractColumn(csvData, 9));
        distanceColumns.add(logSynchronizer.extractColumn(csvData, 10));

        final List<String[]> minimumDistances = logSynchronizer.extractMinimumValueFromColumnsLines(distanceColumns);
        minimumDistances.add(0, new String[]{"MinimumValue"});
        final List<Double> distancesList = logSynchronizer.extractColumn(minimumDistances, 0);
        final List<String[]> normalizedDistances = logSynchronizer.normalizeColumn(distancesList);
        normalizedDistances.add(0, new String[]{"normalizedValue"});
        final List<Double> normalizedDistancesList = logSynchronizer.extractColumn(normalizedDistances, 0);

        return normalizedDistancesList.stream()
                .map(value -> new String[]{String.valueOf(value)})
                .collect(Collectors.toList());
    }

    private List<String[]> extractPowerfulColumn(final List<String[]> csvData) {
        final List<Double> ghostScared = logSynchronizer.extractColumn(csvData, 13);
        return ghostScared.stream()
                .map(value -> new String[]{(value == -1 ? "0" : "1")})
                .collect(Collectors.toList());
    }

    private List<String[]> extractNormalizedGamePercent(final List<String[]> csvData) {
        final List<Double> gamePercent = logSynchronizer.extractColumn(csvData, 18);
        gamePercent.add(0, 100.0);
        final List<String[]> normalizedGamePercent = logSynchronizer.normalizeColumn(gamePercent);
        normalizedGamePercent.remove(0);
        normalizedGamePercent.add(0, new String[]{"Complete"});
        final List<Double> gamePercentList = logSynchronizer.extractColumn(normalizedGamePercent, 0);

        return gamePercentList.stream()
                .map(value -> new String[]{String.valueOf(value)})
                .collect(Collectors.toList());
    }

    private XYChart createChart(final List<String[]> csvData, final String playerName, final int gameId) {
        final double[] eda = listToDoubleArray(logSynchronizer.extractColumn(csvData, 2));
        final double[] time = IntStream.range(0, eda.length)
                .mapToDouble(i -> (i / 60.0))
                .toArray();
        final double[] ghostDies = completeArray(logSynchronizer.extractColumn(csvData, 3), OFFSET);
        final double[] distance = completeArray(logSynchronizer.extractColumn(csvData, 4), OFFSET);
        final double[] powerful = completeArray(logSynchronizer.extractColumn(csvData, 5), OFFSET);
        final double[] complete = completeArray(logSynchronizer.extractColumn(csvData, 6), OFFSET);

        final double totalComplete = complete[complete.length - (OFFSET + 1)];
        final String winString = "Win - 100%";
        final String failString = "Fail - " + String.format("%.2f", (totalComplete * 100)) + "%";
        final String gameResult = totalComplete == 1.0 ? winString : failString;

        final String chartTitle = "Player: " + playerName + " - Game " + gameId;

        final XYChart chart = new XYChartBuilder()
                .width(1900)
                .height(500)
                .title(chartTitle)
                .xAxisTitle("Time (s)")
                //.yAxisTitle("Y")
                .build();

        // Customize Chart
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        // Series
        XYSeries powerful1 = chart.addSeries("Pac-Man Powerful", time, powerful);
        powerful1.setFillColor(Color.GREEN);
        powerful1.setLineColor(Color.GREEN);
        powerful1.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area).setMarker(SeriesMarkers.NONE);

        XYSeries anyGhostDiesSeries = chart.addSeries("Any Ghost Dies", time, ghostDies);
        anyGhostDiesSeries.setFillColor(Color.ORANGE);
        anyGhostDiesSeries.setLineColor(Color.ORANGE);
        anyGhostDiesSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area).setMarker(SeriesMarkers.NONE);

        XYSeries completeSeries = chart.addSeries("Complete", time, complete);
        completeSeries.setLineColor(Color.DARK_GRAY);
        completeSeries.setMarker(SeriesMarkers.NONE);

        XYSeries edaSeries = chart.addSeries("EDA", time, eda);
        edaSeries.setLineColor(Color.BLUE);
        edaSeries.setMarker(SeriesMarkers.NONE);

        XYSeries distanceSeries = chart.addSeries("Distance", time, distance);
        distanceSeries.setLineColor(Color.MAGENTA);
        distanceSeries.setMarker(SeriesMarkers.NONE);

        final List<Double> offsetList = IntStream.range(0, time.length).mapToObj(i -> 0.0).collect(Collectors.toList());
        final int finalGamePosition = offsetList.size() - OFFSET - 1;
        offsetList.remove(finalGamePosition);
        offsetList.add(finalGamePosition, 1.0);

        final double[] yData = offsetList.stream().mapToDouble(aDouble -> aDouble).toArray();

        XYSeries distanceSeries2 = chart.addSeries(gameResult, time, yData);
        distanceSeries2.setLineColor(Color.RED);
        distanceSeries2.setMarker(SeriesMarkers.NONE);

        return chart;
    }

    private void saveGraphic(final XYChart chart, final String name)
            throws IOException {
        BitmapEncoder.saveBitmapWithDPI(chart, name, BitmapEncoder.BitmapFormat.JPG, 300);
    }

    private double[] listToDoubleArray(final List<Double> list) {
        return list.stream()
                .mapToDouble(aDouble -> aDouble)
                .toArray();
    }

    private double[] completeArray(final List<Double> listToComplete,
                                   final int offset) {
        final List<Double> offsetList = IntStream.range(0, offset).mapToObj(i -> 0.0).collect(Collectors.toList());
        final List<Double> finalList = new ArrayList<>(offsetList);
        finalList.addAll(listToComplete);
        finalList.addAll(offsetList);
        return finalList.stream().mapToDouble(aDouble -> aDouble).toArray();
    }

    private List<List<String[]>> loadLogs(final PlayerStartVideo player)
            throws IOException {
        final List<List<String[]>> logsList = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            String resource = String.format(GAME_LOG_RESOURCE, player.getName(), player.getId(), i);
            URL url = LogCreator.class.getClassLoader().getResource(resource);
            logsList.add(logSynchronizer.readData(Objects.requireNonNull(url).getFile(), false));
        }
        return logsList;
    }

    private List<PlayerStartVideo> loadPlayersStartVideos()
            throws IOException {
        final URL playersResources = LogCreator.class.getClassLoader().getResource(LOGS_PATH + "players_start_video.csv");
        final List<String[]> playersData = logSynchronizer.readData(Objects.requireNonNull(playersResources).getFile(), false);
        return playersData.stream().map(playerLine -> PlayerStartVideo.PlayerStartVideoBuilder.playerStartVideoBuilder()
                .id(Integer.parseInt(playerLine[0].replaceAll("[^0-9]+", "")))
                .name(playerLine[1])
                .startGame(new double[]{
                        Double.parseDouble(playerLine[2]),
                        Double.parseDouble(playerLine[3]),
                        Double.parseDouble(playerLine[4])
                })
                .build()).collect(Collectors.toList());
    }
}
