#include <stdio.h>
#include <semaphore.h>
#include <fcntl.h>

#define SEMAPHORE_NAME "/fanotify_semaphore"

int main() {
    sem_t *sem = sem_open(SEMAPHORE_NAME, 0);
    if (sem == SEM_FAILED) {
        perror("sem_open failed");
        return 1;
    }

    sem_post(sem);
    printf("Semaphore freigegeben!\n");

    sem_close(sem);
    return 0;
}
