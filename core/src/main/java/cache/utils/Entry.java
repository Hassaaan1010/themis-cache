package cache.utils;

import com.google.protobuf.ByteString;

public final class Entry {

    private final String key;
    private ByteString value;

    // index inside the random-sampling array/list
    public int index;

    private int size;

    public Entry(String key, ByteString value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public ByteString value() {
        return value;
    }

    public void setValue(ByteString value) {
        this.value = value;
        this.size = value.size();
    }

    public int size() {
        return this.size;
    }
}