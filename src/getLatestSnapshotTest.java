import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class getLatestSnapshotTest {

    public static void main(String[] args) throws IOException {
        System.out.printf(getLatestSnapshot());
    }

    public static String getLatestSnapshot() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "sh", "-c", "zfs list -t snapshot -o name -s creation | grep '^" + "zfs" + "@' | tail -n 1"
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String lastSnapshot = reader.readLine();
        process.destroy();
        return (!lastSnapshot.equals("no datasets available")) ? lastSnapshot.trim() : "";
    }
}
