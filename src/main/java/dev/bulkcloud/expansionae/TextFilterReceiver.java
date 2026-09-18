package dev.bulkcloud.expansionae;

/** Server-side target for text-filter updates from an open ExpansionAE container. */
public interface TextFilterReceiver {
    void applyTextFilter(int key, String value);
}
