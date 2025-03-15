package zfsmanager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZFSFileManager {
    private static final String ZFS_POOL = "zfs";
    private String currentSnapshot;

    public ZFSFileManager() {

    }

    public void createSnapshot() {
        try {
            // Hole die aktuelle Zeit in Nanosekunden
            long nanoTime = System.nanoTime();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String snapshotName = ZFS_POOL + "@autosnap_" + timestamp + "_" + nanoTime;
            Process process = new ProcessBuilder("zfs", "snapshot", snapshotName).start();
            process.waitFor();
            currentSnapshot = snapshotName;
            System.out.println("📸 Snapshot erstellt: " + snapshotName);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Erstellen des Snapshots: " + e.getMessage());
        }
    }

    public void rollbackSnapshot() {
        try {
            Process process = new ProcessBuilder("zfs", "rollback", currentSnapshot).start();
            process.waitFor();
            System.out.println("🔄 Rollback durchgeführt: " + currentSnapshot);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Rollback: " + e.getMessage());
        }
    }

    public void setCurrentSnapshot(String snapshot) {
        this.currentSnapshot = snapshot;
    }

}
