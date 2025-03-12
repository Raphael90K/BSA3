package ZFSFileMonitor;

import java.util.NoSuchElementException;

public class RingBuffer<T> {
    private final T[] buffer;
    private int tail = 0;
    private int size = 0;
    private final int capacity;

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = (T[]) new Object[capacity];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void enqueue(T item) {
        buffer[tail] = item;
        tail = (tail + 1) % capacity;
        size++;
    }

    public T peekFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("RingBuffer is empty");
        }
        return buffer[tail - 1];
    }

    public T peekSecond() {
        if (size < 2) {
            throw new NoSuchElementException("Size < 2");
        }
        return buffer[tail - 2 % capacity];
    }
}
