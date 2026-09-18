package dev.bulkcloud.expansionae;

import java.util.LinkedList;

final class ThroughputCache {
    private static final int MAX_SIZE = 600;
    private final LinkedList<Entry> samples = new LinkedList<>();

    void push(long amount, long tick) {
        if (tick <= 0) return;
        if (!samples.isEmpty() && samples.getFirst().tick == tick) return;
        samples.addFirst(new Entry(amount, tick));
        while (samples.size() > MAX_SIZE) samples.removeLast();
    }

    void clear() { samples.clear(); }
    int size() { return samples.size(); }

    double averagePerTick(long currentTick, int seconds) {
        long cutoff = currentTick - seconds * 20L;
        Entry newest = null;
        Entry oldest = null;
        for (Entry e : samples) {
            if (newest == null) newest = e;
            oldest = e;
            if (e.tick <= cutoff) break;
        }
        if (newest == null || oldest == null || newest.tick == oldest.tick) return 0;
        return (newest.amount - oldest.amount) / (double) (newest.tick - oldest.tick);
    }

    private static final class Entry {
        final long amount;
        final long tick;
        Entry(long amount, long tick) { this.amount = amount; this.tick = tick; }
    }
}
