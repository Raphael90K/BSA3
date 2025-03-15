package testA;

import java.io.*;

public class ProcessA {
    private static final String OUTPUT_FILE = "/zfs/5000.txt";
    private static final int SLEEP_MILLIS = 5000;
    private static final int iterations = 20;

    public static void main(String[] args) throws Exception {
        SimpleWrite.write_mem('Z');
        for (int i = 0; i < iterations; i++) {
            System.out.printf("iteration: %d\n", i);
            process();
            Thread.sleep(5000);
        }
    }

    public static void process() {
        try {
            // 1. Datei öffnen
            System.out.println("Programm A hat die Datei geöffnet.");
            FileWriter writer = new FileWriter(OUTPUT_FILE, true);
            Thread.sleep(SLEEP_MILLIS);
            setStatus('B');

            // 2. Warten, bis B fertig geschrieben hat
            while (!(getStatus() == 'A')) {
                Thread.sleep(SLEEP_MILLIS);
            }
            // 3. Schreiben in die Datei
            writer.write("A\n");
            writer.close();
            System.out.println("Programm A hat geschrieben.");
            Thread.sleep(SLEEP_MILLIS);
            // 4. Abschlussstatus setzen

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static char getStatus() throws Exception {
        return SimpleWrite.read_mem();
    }

    private static void setStatus(char status) throws Exception {
        SimpleWrite.write_mem(status);
    }

}
