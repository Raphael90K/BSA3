package zfsFileMonitor;

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

    public int getSize() {
        return size;
    }

    public void enqueue(T item) {
        buffer[tail] = item;
        tail = (tail + 1) % capacity;
        size = size == capacity ? size : size + 1;
    }

    public T peekIndex(int index) {
        if (size <= index) {
            throw new NoSuchElementException("size " + size + " <= index "+ index);
        }
        return buffer[(tail - 1 - index + capacity) % capacity];
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            string.append(this.peekIndex(i));
            string.append(i == size - 1 ? "]" : ",");
        }
        return string.toString();
    }

}
