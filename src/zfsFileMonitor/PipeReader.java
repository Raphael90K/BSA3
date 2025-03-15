package zfsFileMonitor;

import java.io.*;

public class PipeReader {
    private BufferedReader br;

    PipeReader(String pipeName) {
        try {
            this.br = new BufferedReader(new FileReader(pipeName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String readLine() {
        try {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            this.br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
