package zfsmanager;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class TransactionManager {
    private int rollbacks;
    private int commits;
    private static String latestSnapshot;
    private final ZFSFileManager zfsManager;
    private String directory;
    private Path filePath;
    private String fileHash;
    private BufferedWriter writer;
    private int commitFails;
    private boolean txStarted;

    public TransactionManager(String pathToDir) {
        this.rollbacks = 0;
        this.commits = 0;
        this.commitFails = 0;
        this.zfsManager = new ZFSFileManager();
        this.directory = pathToDir;
        this.txStarted = false;
    }

    public void start(Path filePath) {
        latestSnapshot = this.zfsManager.createSnapshot();
        this.filePath = filePath;
        try {
            this.fileHash = getFileHash(this.filePath);
            this.txStarted = true;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("File %s: %s\n", filePath.getFileName(), fileHash);
    }

    public boolean commit(String content, boolean append, boolean deleteFile) {
        boolean success = false;
        if (!this.txStarted) {
            throw new RuntimeException("Transaction not started yet with start(filePath).");
        }
        if (deleteFile) {
            return true;
        }
        success = commitIsSuccess(content, append, success);
        this.txStarted = false;
        return success;
    }

    private boolean commitIsSuccess(String content, boolean append, boolean success) {
        String latestSnapFileHash;
        String currentFileHash;
        // Sperre Datei, schreibe und prüfe, ob Rollback notwendig ist.
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile(), append)) {
            FileChannel fileChannel = fos.getChannel();
            FileLock lock;
            while ((lock = fileChannel.tryLock()) == null){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // Exklusive Sperre für den ganzen Prozess
            System.out.println("Datei gesperrt!");
            currentFileHash = getFileHash(filePath);

            write(content, fos);

            // Neuen Datei-Hash berechnen
            latestSnapFileHash = getLatestSnapshotHash(this.filePath);
            // Prüfe, ob Datei unverändert ist
            if (latestSnapFileHash.equals(this.fileHash) && currentFileHash.equals(this.fileHash)) {
                latestSnapshot = zfsManager.createSnapshot();
                this.commits++;
                lock.release();
                System.out.println("Datei freigegeben.");
                System.out.println("Commit erfolgreich");
                success = true;
            } else {
                zfsManager.rollbackSnapshot(latestSnapshot);
                lock.release();
                this.rollbacks++;
            }

        } catch (IOException | NoSuchAlgorithmException | OverlappingFileLockException e) {
            System.out.println("Commit Error: " + e.getLocalizedMessage());
            this.commitFails++;
        }
        return success;
    }

    private void write(String content, FileOutputStream fos) throws IOException {
        // Datei schreiben
        this.writer = new BufferedWriter(new OutputStreamWriter(fos));
        this.writer.write(content);
        this.writer.flush();
    }

    public int getRollbacks() {
        return rollbacks;
    }

    public int getCommits() {
        return commits;
    }

    public int getCommitFails() {
        return commitFails;
    }

    private String getFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        if (Files.exists(filePath)) {
            return calculateHash(filePath);
        } else {
            System.err.println("⚠ Datei nicht gefunden: " + filePath);
            return "";
        }
    }

    private String calculateHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getLatestSnapshotHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        Path filePathInSnapshot = getLatestSnapshotPath(filePath);
        if (Files.exists(filePathInSnapshot)) {
            return calculateHash(filePathInSnapshot);
        } else {
            System.err.println("⚠ Kein Snapshot gefunden: " + filePathInSnapshot);
            return "";
        }
    }

    private Path getLatestSnapshotPath(Path filePath) throws IOException, NoSuchAlgorithmException {
        latestSnapshot = getLatestSnapshot();
        return Paths.get(filePath.getParent() + "/.zfs/snapshot/" + latestSnapshot.split("@")[1] + "/" + filePath.getFileName());
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
