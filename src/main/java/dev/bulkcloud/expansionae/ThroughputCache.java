package dev.bulkcloud.expansionae;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling sample cache used by the throughput monitor.
 */
final class ThroughputCache {
    private static final int MAX_SIZE = 2400;

    private static final class Entry {
        final long amount;
        final long tick;

        Entry(long amount, long tick) {
            this.amount = amount;
            this.tick = tick;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    int size() {
        return this.entries.size();
    }

    void clear() {
        this.entries.clear();
    }

    void push(long amount, long tick) {
        if (tick <= 0) {
            return;
        }
        Entry first = this.entries.peekFirst();
        if (first != null && first.tick == tick) {
            return;
        }
        this.entries.addFirst(new Entry(amount, tick));
        while (this.entries.size() > MAX_SIZE) {
            this.entries.removeLast();
        }
    }

    double averagePerTick(int timeLimitSeconds, long currentTick) {
        long lowerBound = currentTick - timeLimitSeconds * 20L;
        long lastAmount = Long.MIN_VALUE;
        long lastTick = Long.MIN_VALUE;
        double sum = 0.0;
        int count = 0;

        for (Entry entry : this.entries) {
            if (entry.tick < lowerBound) {
                break;
            }
            if (lastTick != Long.MIN_VALUE) {
                long tickDelta = lastTick - entry.tick;
                if (tickDelta > 0) {
                    sum += (lastAmount - entry.amount) / (double) tickDelta;
                    count++;
                }
            }
            lastAmount = entry.amount;
            lastTick = entry.tick;
        }

        return count == 0 ? 0.0 : sum / count;
    }
}
