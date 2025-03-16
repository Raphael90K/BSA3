# Betriebssysteme Übung 3 WiSe 24 / 25

## ZFS Datei erstellen und Dateisystem initialisieren, nützliche Befehle

Datei erstellen und als Loopback-Gerät initialisieren
```sh 
dd if=/dev/zero of=deine_datei.img bs=256K count=4096
losetup /dev/loop0 deine_datei.img
```

Zpool erstellen und prüfen
```sh
zpool create mein_zfs_pool /dev/loop0
zpool status
zfs list
```

Alle Snapshots löschen
```sh 
sudo zfs list -H -o name -t snapshot | xargs -n 1 sudo zfs destroy
```

## Brainstorming Programme verwenden

### Konsistenzüberwachung innerhalb des Programms


### Konsistenzüberwachung auf Ebene des Ordners
Erforderliche c Programme zur Überwachung des Dateisystems kompilieren und ausführen
```sh
gcc -o fanotify fanotify.c
gcc -o sem_post sem_post.c
sudo ./fanotify
```

Erforderliche java Programme zur Behandlung der Events kompilieren und ausführen
```sh 
javac zfsFileMonitor/*.java
sudo java zfsFileMonitor.ZFSFileMonitorPID
```

BrainstormingApp mit Lantera kompilieren und ausführen
````sh
javac -cp :laterna-3.1.3.jar brainstorming/*.java
sudo java -cp .:laterna-3.1.3.jar brainstorming.BrainstormingApp
````