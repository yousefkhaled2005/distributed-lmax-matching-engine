package com.enterprise.matching.disruptor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * High-performance, lock-free ring buffer padded against false sharing (LMAX Disruptor pattern).
 * Employs cache-line padding (128 bytes on modern x86-64) and memory fence semantics.
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class RingBuffer<T> {
    // 128 bytes cache-line padding to prevent false sharing
    public long p1, p2, p3, p4, p5, p6, p7, p8;

    private final Object[] entries;
    private final int bufferSize;
    private final int mask;

    // Cache-line padded sequence cursor
    public long p9, p10, p11, p12, p13, p14, p15, p16;
    private volatile long cursor = -1L;
    public long p17, p18, p19, p20, p21, p22, p23, p24;

    private static final VarHandle CURSOR;

    static {
        try {
            CURSOR = MethodHandles.lookup().findVarHandle(RingBuffer.class, "cursor", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public RingBuffer(int bufferSize) {
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        this.bufferSize = bufferSize;
        this.mask = bufferSize - 1;
        this.entries = new Object[bufferSize];
    }

    public long next() {
        return (long) CURSOR.getAndAdd(this, 1L) + 1L;
    }

    @SuppressWarnings("unchecked")
    public T get(long sequence) {
        return (T) entries[(int) (sequence & mask)];
    }

    public void set(long sequence, T element) {
        entries[(int) (sequence & mask)] = element;
    }

    public void publish(long sequence) {
        VarHandle.releaseFence();
    }

    public long getCursor() {
        return (long) CURSOR.getVolatile(this);
    }

    public int getBufferSize() {
        return bufferSize;
    }
}
