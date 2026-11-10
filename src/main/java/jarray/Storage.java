package jarray;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class Storage {

    private static final int INITIAL_CAPACITY = 10;
    private static final int MINIMUM_ORPHAN_LIMIT = 16;

    private byte[] bytes;
    private int byteCount;

    private short[] shorts;
    private int shortCount;

    private int[] ints;
    private int intCount;

    private long[] longs;
    private int longCount;

    private float[] floats;
    private int floatCount;

    private double[] doubles;
    private int doubleCount;

    private char[] chars;
    private int charCount;

    private boolean[] booleans;
    private int booleanCount;

    private Object[] objects;
    private int objectCount;

    private byte[] types;
    private int[] indices;

    private int size;
    private int orphanCount;
    private int version;

    int size() {
        return size;
    }

    int version() {
        return version;
    }

    int metadataCapacity() {
        return types == null ? 0 : types.length;
    }

    void ensureCapacity(int minimum) {

        int current = types == null ? 0 : types.length;

        if (minimum <= current) {
            return;
        }

        int capacity = Math.max(Math.max(INITIAL_CAPACITY, current * 2), minimum);

        types = types == null ? new byte[capacity] : Arrays.copyOf(types, capacity);
        indices = indices == null ? new int[capacity] : Arrays.copyOf(indices, capacity);
    }

    void addByte(byte value) {
        ensureCapacity(size + 1);
        commit(DataType.BYTE, pushByte(value));
    }

    void addShort(short value) {
        ensureCapacity(size + 1);
        commit(DataType.SHORT, pushShort(value));
    }

    void addInt(int value) {
        ensureCapacity(size + 1);
        commit(DataType.INT, pushInt(value));
    }

    void addLong(long value) {
        ensureCapacity(size + 1);
        commit(DataType.LONG, pushLong(value));
    }

    void addFloat(float value) {
        ensureCapacity(size + 1);
        commit(DataType.FLOAT, pushFloat(value));
    }

    void addDouble(double value) {
        ensureCapacity(size + 1);
        commit(DataType.DOUBLE, pushDouble(value));
    }

    void addChar(char value) {
        ensureCapacity(size + 1);
        commit(DataType.CHAR, pushChar(value));
    }

    void addBoolean(boolean value) {
        ensureCapacity(size + 1);
        commit(DataType.BOOLEAN, pushBoolean(value));
    }

    void add(Object value) {
        ensureCapacity(size + 1);
        place(size, value);
        size++;
        version++;
    }

    void appendFrom(Storage source, int logicalIndex) {

        ensureCapacity(size + 1);

        DataType type = DataType.fromCode(source.types[logicalIndex]);
        int physical = source.indices[logicalIndex];
        int newPhysical;

        switch (type) {
            case BYTE:
                newPhysical = pushByte(source.bytes[physical]);
                break;
            case SHORT:
                newPhysical = pushShort(source.shorts[physical]);
                break;
            case INT:
                newPhysical = pushInt(source.ints[physical]);
                break;
            case LONG:
                newPhysical = pushLong(source.longs[physical]);
                break;
            case FLOAT:
                newPhysical = pushFloat(source.floats[physical]);
                break;
            case DOUBLE:
                newPhysical = pushDouble(source.doubles[physical]);
                break;
            case CHAR:
                newPhysical = pushChar(source.chars[physical]);
                break;
            case BOOLEAN:
                newPhysical = pushBoolean(source.booleans[physical]);
                break;
            default:
                newPhysical = pushObject(source.objects[physical]);
        }

        commit(type, newPhysical);
    }

    void insert(int logicalIndex, Object value) {

        ensureCapacity(size + 1);
        shiftRight(logicalIndex, 1);
        place(logicalIndex, value);

        size++;
        version++;
    }

    void insertAll(int logicalIndex, List<?> values) {

        int count = values.size();

        if (count == 0) {
            return;
        }

        ensureCapacity(size + count);
        shiftRight(logicalIndex, count);

        for (int i = 0; i < count; i++) {
            place(logicalIndex + i, values.get(i));
        }

        size += count;
        version++;
    }

    Object get(int logicalIndex) {

        int physical = indices[logicalIndex];

        switch (DataType.fromCode(types[logicalIndex])) {
            case BYTE:
                return bytes[physical];
            case SHORT:
                return shorts[physical];
            case INT:
                return ints[physical];
            case LONG:
                return longs[physical];
            case FLOAT:
                return floats[physical];
            case DOUBLE:
                return doubles[physical];
            case CHAR:
                return chars[physical];
            case BOOLEAN:
                return booleans[physical];
            default:
                return objects[physical];
        }
    }

    DataType typeAt(int logicalIndex) {
        return DataType.fromCode(types[logicalIndex]);
    }

    void set(int logicalIndex, Object value) {

        DataType newType = DataType.of(value);
        DataType oldType = DataType.fromCode(types[logicalIndex]);
        int physical = indices[logicalIndex];

        if (newType == oldType) {
            overwrite(newType, physical, value);
            return;
        }

        release(oldType, physical);
        place(logicalIndex, value);
        compactIfNeeded();
    }

    void removeAt(int logicalIndex) {

        release(DataType.fromCode(types[logicalIndex]), indices[logicalIndex]);

        int moved = size - logicalIndex - 1;

        System.arraycopy(types, logicalIndex + 1, types, logicalIndex, moved);
        System.arraycopy(indices, logicalIndex + 1, indices, logicalIndex, moved);

        size--;
        version++;

        compactIfNeeded();
    }

    int removeEqual(Object value) {

        int write = 0;

        for (int read = 0; read < size; read++) {

            if (Objects.equals(get(read), value)) {
                release(DataType.fromCode(types[read]), indices[read]);
            } else {
                types[write] = types[read];
                indices[write] = indices[read];
                write++;
            }
        }

        int removed = size - write;
        size = write;

        if (removed > 0) {
            version++;
            compactIfNeeded();
        }

        return removed;
    }

    void swap(int first, int second) {

        byte type = types[first];
        types[first] = types[second];
        types[second] = type;

        int index = indices[first];
        indices[first] = indices[second];
        indices[second] = index;
    }

    void clear() {

        bytes = null;
        byteCount = 0;
        shorts = null;
        shortCount = 0;
        ints = null;
        intCount = 0;
        longs = null;
        longCount = 0;
        floats = null;
        floatCount = 0;
        doubles = null;
        doubleCount = 0;
        chars = null;
        charCount = 0;
        booleans = null;
        booleanCount = 0;
        objects = null;
        objectCount = 0;

        types = null;
        indices = null;
        size = 0;
        orphanCount = 0;
        version++;
    }

    private void commit(DataType type, int physical) {
        types[size] = type.code();
        indices[size] = physical;
        size++;
        version++;
    }

    private void shiftRight(int from, int amount) {
        System.arraycopy(types, from, types, from + amount, size - from);
        System.arraycopy(indices, from, indices, from + amount, size - from);
    }

    private void place(int logicalIndex, Object value) {
        DataType type = DataType.of(value);
        indices[logicalIndex] = push(type, value);
        types[logicalIndex] = type.code();
    }

    private int push(DataType type, Object value) {

        switch (type) {
            case BYTE:
                return pushByte((Byte) value);
            case SHORT:
                return pushShort((Short) value);
            case INT:
                return pushInt((Integer) value);
            case LONG:
                return pushLong((Long) value);
            case FLOAT:
                return pushFloat((Float) value);
            case DOUBLE:
                return pushDouble((Double) value);
            case CHAR:
                return pushChar((Character) value);
            case BOOLEAN:
                return pushBoolean((Boolean) value);
            default:
                return pushObject(value);
        }
    }

    private void overwrite(DataType type, int physical, Object value) {

        switch (type) {
            case BYTE:
                bytes[physical] = (Byte) value;
                break;
            case SHORT:
                shorts[physical] = (Short) value;
                break;
            case INT:
                ints[physical] = (Integer) value;
                break;
            case LONG:
                longs[physical] = (Long) value;
                break;
            case FLOAT:
                floats[physical] = (Float) value;
                break;
            case DOUBLE:
                doubles[physical] = (Double) value;
                break;
            case CHAR:
                chars[physical] = (Character) value;
                break;
            case BOOLEAN:
                booleans[physical] = (Boolean) value;
                break;
            default:
                objects[physical] = value;
        }
    }

    private void release(DataType type, int physical) {

        if (type == DataType.OBJECT) {
            objects[physical] = null;
        }

        orphanCount++;
    }

    private void compactIfNeeded() {

        if (orphanCount > Math.max(MINIMUM_ORPHAN_LIMIT, size)) {
            compact();
        }
    }

    private void compact() {

        Storage compacted = new Storage();
        compacted.ensureCapacity(size);

        for (int i = 0; i < size; i++) {
            compacted.appendFrom(this, i);
        }

        bytes = compacted.bytes;
        byteCount = compacted.byteCount;
        shorts = compacted.shorts;
        shortCount = compacted.shortCount;
        ints = compacted.ints;
        intCount = compacted.intCount;
        longs = compacted.longs;
        longCount = compacted.longCount;
        floats = compacted.floats;
        floatCount = compacted.floatCount;
        doubles = compacted.doubles;
        doubleCount = compacted.doubleCount;
        chars = compacted.chars;
        charCount = compacted.charCount;
        booleans = compacted.booleans;
        booleanCount = compacted.booleanCount;
        objects = compacted.objects;
        objectCount = compacted.objectCount;

        types = compacted.types;
        indices = compacted.indices;
        orphanCount = 0;
    }

    private int pushByte(byte value) {
        if (bytes == null) {
            bytes = new byte[INITIAL_CAPACITY];
        } else if (byteCount == bytes.length) {
            bytes = Arrays.copyOf(bytes, bytes.length * 2);
        }
        bytes[byteCount] = value;
        return byteCount++;
    }

    private int pushShort(short value) {
        if (shorts == null) {
            shorts = new short[INITIAL_CAPACITY];
        } else if (shortCount == shorts.length) {
            shorts = Arrays.copyOf(shorts, shorts.length * 2);
        }
        shorts[shortCount] = value;
        return shortCount++;
    }

    private int pushInt(int value) {
        if (ints == null) {
            ints = new int[INITIAL_CAPACITY];
        } else if (intCount == ints.length) {
            ints = Arrays.copyOf(ints, ints.length * 2);
        }
        ints[intCount] = value;
        return intCount++;
    }

    private int pushLong(long value) {
        if (longs == null) {
            longs = new long[INITIAL_CAPACITY];
        } else if (longCount == longs.length) {
            longs = Arrays.copyOf(longs, longs.length * 2);
        }
        longs[longCount] = value;
        return longCount++;
    }

    private int pushFloat(float value) {
        if (floats == null) {
            floats = new float[INITIAL_CAPACITY];
        } else if (floatCount == floats.length) {
            floats = Arrays.copyOf(floats, floats.length * 2);
        }
        floats[floatCount] = value;
        return floatCount++;
    }

    private int pushDouble(double value) {
        if (doubles == null) {
            doubles = new double[INITIAL_CAPACITY];
        } else if (doubleCount == doubles.length) {
            doubles = Arrays.copyOf(doubles, doubles.length * 2);
        }
        doubles[doubleCount] = value;
        return doubleCount++;
    }

    private int pushChar(char value) {
        if (chars == null) {
            chars = new char[INITIAL_CAPACITY];
        } else if (charCount == chars.length) {
            chars = Arrays.copyOf(chars, chars.length * 2);
        }
        chars[charCount] = value;
        return charCount++;
    }

    private int pushBoolean(boolean value) {
        if (booleans == null) {
            booleans = new boolean[INITIAL_CAPACITY];
        } else if (booleanCount == booleans.length) {
            booleans = Arrays.copyOf(booleans, booleans.length * 2);
        }
        booleans[booleanCount] = value;
        return booleanCount++;
    }

    private int pushObject(Object value) {
        if (objects == null) {
            objects = new Object[INITIAL_CAPACITY];
        } else if (objectCount == objects.length) {
            objects = Arrays.copyOf(objects, objects.length * 2);
        }
        objects[objectCount] = value;
        return objectCount++;
    }
}