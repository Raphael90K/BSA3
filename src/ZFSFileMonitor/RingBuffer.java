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
        size = size == capacity ? size : size + 1;
    }

    public T peekIndex(int index) {
        if (size <= index) {
            throw new NoSuchElementException("size <= index");
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

    public static void main(String[] args) {
        RingBuffer<String> rb = new RingBuffer<>(3);
        rb.enqueue("a");
        rb.enqueue("b");
        rb.enqueue("c");
        rb.enqueue("d");
        rb.enqueue("e");

        System.out.println(rb);
    }
}
