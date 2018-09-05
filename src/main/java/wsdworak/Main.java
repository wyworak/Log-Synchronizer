package wsdworak;

import java.io.IOException;

public class Main {

    public static void main(String[] args)
            throws IOException {

        LogCreator logCreator = new LogCreator();
        //logCreator.createBaseLogs();
        logCreator.createCharts();
    }
}
