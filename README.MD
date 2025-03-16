# Betriebssysteme Übung 3 WiSe 24 / 25

## ZFS Datei erstellen und Dateisystem initialisieren, nützliche Befehle

Datei erstellen und als Loopback-Gerät initialisieren
```sh 
dd if=/dev/zero of=zfs_disk.img bs=256K count=4096
losetup /dev/loop9 zfs_disk.img
lsblk
```

Zpool erstellen und prüfen
```sh
sudo zpool create zfs /dev/loop9
zpool status
zfs list
```

Alle Snapshots löschen
```sh 
sudo zfs list -H -o name -t snapshot | xargs -n 1 sudo zfs destroy
```

## Brainstorming Programme verwenden

### Brainstorming mit Konsistenzüberwachung innerhalb des Programms
Erforderliche Programme kompilieren und ausführen
```sh
javac zfsmanager/*.java
sudo java zfsmanager.Brainstorming
```

### Brainstorming mit Konsistenzüberwachung auf Ebene des Ordners
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

### Validierung testen
Test A kompilieren und starten
```sh 
javac testA/*.java
sudo java testA.AutomaticTest
```

Um Test B auszuführen zu können ist eine Datei in /dev/shm/ notwendig
```sh 
sudo touch /dev/shm/status
```

Anschließend Test B kompilieren und beide Programme ausführen
```sh
javac testB/*.java
sudo java testB.ProcessA
sudo java testB.ProcessB
```