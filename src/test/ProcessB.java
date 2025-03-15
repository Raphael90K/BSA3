package test;

import java.io.*;
import java.nio.file.*;

public class ProcessB {
    private static final String STATUS_FILE = "status.txt";
    private static final String OUTPUT_FILE = "/zfs/test.txt";
    private static final int SLEEP_MILLIS = 2000;
    private static final int iterations = 10;

    public static void main(String[] args) {
        for (int i = 0; i < iterations; i++) {
            process();
        }
    }

    public static void process() {
        try {
            // 1. Warten, bis A geöffnet hat
            while (!"A_OPEN".equals(getStatus())) {
                Thread.sleep(500);
            }
            // 2. Datei öffnen
            System.out.println("Programm B hat die Datei geöffnet.");
            setStatus("B_OPEN");
            FileWriter writer = new FileWriter(OUTPUT_FILE, true);
            Thread.sleep(SLEEP_MILLIS);

            // 3. Schreiben in die Datei
            writer.write("Programm B schreibt und schließt.\n");
            System.out.println("Programm B hat geschrieben.");
            writer.close();
            Thread.sleep(SLEEP_MILLIS);
            // 4. Status setzen, damit A schreiben kann
            setStatus("B_DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getStatus() throws IOException {
        return Files.readString(Path.of(STATUS_FILE)).trim();
    }

    private static void setStatus(String status) throws IOException {
        Files.writeString(Path.of(STATUS_FILE), status);
    }
}
