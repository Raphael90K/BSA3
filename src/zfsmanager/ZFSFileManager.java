package zfsmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZFSFileManager {
    private static final String ZFS_POOL = "zfs";

    public ZFSFileManager() {

    }

    public String createSnapshot() {
        String snapshotName = "";
        try {
            // Hole die aktuelle Zeit in Nanosekunden
            long nanoTime = System.nanoTime();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            snapshotName = ZFS_POOL + "@autosnap_" + timestamp + "_" + nanoTime;
            Process process = new ProcessBuilder("zfs", "snapshot", snapshotName).start();
            process.waitFor();
            System.out.println("📸 Snapshot erstellt: " + snapshotName);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Erstellen des Snapshots: " + e.getMessage());
        }
        return snapshotName;
    }

    public void rollbackSnapshot(String currentSnapshot) {
        try {
            Process process = new ProcessBuilder("zfs", "rollback", currentSnapshot).start();
            process.waitFor();
            System.out.println("🔄 Rollback durchgeführt: " + currentSnapshot);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Rollback: " + e.getMessage());
        }
    }

}
