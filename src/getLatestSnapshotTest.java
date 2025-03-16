import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class getLatestSnapshotTest {

    public static void main(String[] args) {
        //System.out.printf(getLatestSnapshot());

        Path p = Paths.get("/zfs/snapshot/12.txt");
        System.out.println(Files.exists(p));
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
