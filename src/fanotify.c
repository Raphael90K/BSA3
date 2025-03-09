#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/fanotify.h>
#include <errno.h>
#include <string.h>

#define BUF_SIZE 4096
#define MAX_FILENAME_LEN 256
#define PIPE_NAME "/tmp/fanotify_pipe"

// Zähler für Ereignisse
int open_count = 0;
int modify_count = 0;

// Funktion zum Abrufen des Dateinamens aus dem Dateideskriptor
void get_filename_from_fd(int fd, char *filename, size_t len) {
    snprintf(filename, len, "/proc/self/fd/%d", fd);
    ssize_t res = readlink(filename, filename, len);
    if (res == -1) {
        perror("Fehler beim Abrufen des Dateinamens");
        return;
    }
    filename[res] = '\0';  // Null-terminiere den Dateinamen
}

// Funktion, um den fanotify-Event zu verarbeiten und den Dateinamen abzurufen
void process_fanotify_event(struct fanotify_event_metadata *metadata, char *buffer, int pipe_fd) {
    char filename[MAX_FILENAME_LEN];
    int fd;

    fd = metadata->fd;
    get_filename_from_fd(fd, filename, sizeof(filename));


    // Ereignis zählen
    if (metadata->mask & FAN_OPEN) {
        modify_count = modify_count + 1;
        dprintf(pipe_fd, "%d,OPEN,%s,%d\n",modify_count, filename, metadata->pid);
    }
    if (metadata->mask & FAN_MODIFY) {
        modify_count = modify_count + 1;
        dprintf(pipe_fd, "%d,MODIFY,%s,%d\n",modify_count, filename, metadata->pid);
    }
}

int main() {
    int fanotify_fd;
    int pipe_fd;
    struct fanotify_event_metadata *metadata;
    char buffer[BUF_SIZE];
    ssize_t len;

    // Pipe erstellen, falls sie noch nicht existiert
    if (mkfifo(PIPE_NAME, 0666) == -1 && errno != EEXIST) {
        perror("mkfifo failed");
        exit(EXIT_FAILURE);
    }

    // Öffne die Pipe für das Schreiben von Daten
    pipe_fd = open(PIPE_NAME, O_WRONLY);
    if (pipe_fd == -1) {
        perror("open pipe failed");
        exit(EXIT_FAILURE);
    }

    // fanotify initialisieren
    fanotify_fd = fanotify_init(FAN_NONBLOCK, O_RDONLY);
    if (fanotify_fd == -1) {
        perror("fanotify_init failed");
        exit(EXIT_FAILURE);
    }

    // Überwache das Verzeichnis
    if (fanotify_mark(fanotify_fd, FAN_MARK_ADD | FAN_MARK_FILESYSTEM, FAN_OPEN | FAN_MODIFY, AT_FDCWD, "/zfs") == -1) {
        perror("fanotify_mark failed");
        close(fanotify_fd);
        exit(EXIT_FAILURE);
    }

    printf("Überwache Lese-, Änderungszugriffe in /zfs\n");

    // Endlosschleife zum Abfragen und Verarbeiten von Events
    while (1) {
        len = read(fanotify_fd, buffer, sizeof(buffer));
        if (len == -1) {
            if (errno == EAGAIN) {
                sleep(1);  // Keine neuen Ereignisse
            } else {
                perror("read failed");
                close(fanotify_fd);
                close(pipe_fd);
                exit(EXIT_FAILURE);
            }
        }

        metadata = (struct fanotify_event_metadata *) buffer;
        while (FAN_EVENT_OK(metadata, len)) {
            process_fanotify_event(metadata, buffer, pipe_fd);
            metadata = FAN_EVENT_NEXT(metadata, len);
        }
    }

    close(fanotify_fd);
    close(pipe_fd);
    return 0;
}
