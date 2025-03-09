package ZFSFileMonitor;

/**
 * Metadaten zu einer geöffneten Datei. Sie enthält informationen über den Prozess der die Datei geöffnet hat und
 * den Hashwert der Datei bei Öffnen.
 *
 */
class Metadata {
    private long pid;   // Prozess-ID
    private String hash;


    Metadata(long pid, String hash) {
        this.pid = pid;
        this.hash = hash;
    }

    public long getPid() {
        return pid;
    }

    public String getHash() {
        return hash;
    }
}
