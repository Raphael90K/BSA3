package zfsFileMonitor;

import java.io.IOException;

public class SemaphoreControl {

    public static void sem_post() {
        try {
            new ProcessBuilder("./c/sem_post").start().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
