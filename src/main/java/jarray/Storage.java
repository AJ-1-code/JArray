package jarray;

import java.util.Arrays;
import java.util.List;

final class Storage {

    static final int BYTE = 0;
    static final int SHORT = 1;
    static final int INT = 2;
    static final int LONG = 3;
    static final int FLOAT = 4;
    static final int DOUBLE = 5;
    static final int CHAR = 6;
    static final int BOOLEAN = 7;
    static final int OBJECT = 8;

    private static final int NONE = -1;
    private static final int TYPE_SHIFT = 28;
    private static final int PHYSICAL_MASK = 0x0FFFFFFF;
    private static final int INITIAL_CAPACITY = 10;
    private static final int MINIMUM_ORPHAN_LIMIT = 16;

    private int[] metadata;
    private int uniformCode = NONE;

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

    private int size;
    private int orphanCount;
    private int version;
    private int reserved;

    static int codeOf(Object value) {

        if (value instanceof Integer) {
            return INT;
        }
        if (value instanceof Double) {
            return DOUBLE;
        }
        if (value instanceof Long) {
            return LONG;
        }
        if (value instanceof Number) {
            if (value instanceof Float) {
                return FLOAT;
            }
            if (value instanceof Short) {
                return SHORT;
            }
            if (value instanceof Byte) {
                return BYTE;
            }
            return OBJECT;
        }
        if (value instanceof Boolean) {
            return BOOLEAN;
        }
        if (value instanceof Character) {
            return CHAR;
        }

        return OBJECT;
    }

    private static int pack(int code, int physicalIndex) {
        return (code << TYPE_SHIFT) | physicalIndex;
    }

    private static int entryType(int entry) {
        return entry >>> TYPE_SHIFT;
    }

    private static int entryPhysical(int entry) {
        return entry & PHYSICAL_MASK;
    }

    private static void checkPhysicalLimit(int count) {
        if (count > PHYSICAL_MASK) {
            throw new IllegalStateException(
                "Mixed-type arrays support at most " + (PHYSICAL_MASK + 1)
                + " elements per type."
            );
        }
    }

    private static int grown(int length) {
        return Math.max(INITIAL_CAPACITY, length + (length >> 1));
    }

    int size() {
        return size;
    }

    int version() {
        return version;
    }

    int capacity() {

        if (metadata != null) {
            return metadata.length;
        }

        switch (uniformCode) {
            case BYTE:
                return bytes == null ? reserved : bytes.length;
            case SHORT:
                return shorts == null ? reserved : shorts.length;
            case INT:
                return ints == null ? reserved : ints.length;
            case LONG:
                return longs == null ? reserved : longs.length;
            case FLOAT:
                return floats == null ? reserved : floats.length;
            case DOUBLE:
                return doubles == null ? reserved : doubles.length;
            case CHAR:
                return chars == null ? reserved : chars.length;
            case BOOLEAN:
                return booleans == null ? reserved : booleans.length;
            case OBJECT:
                return objects == null ? reserved : objects.length;
            default:
                return reserved;
        }
    }

    void ensureCapacity(int minimum) {

        if (metadata != null) {
            ensureMetadataCapacity(minimum);
        } else if (uniformCode == NONE) {
            reserved = Math.max(reserved, minimum);
        } else {
            ensureUniformCapacity(minimum);
        }
    }

    private void ensureMetadataCapacity(int minimum) {

        int current = metadata.length;

        if (minimum > current) {
            metadata = Arrays.copyOf(metadata, Math.max(grown(current), minimum));
        }
    }

    private int initialLength(int minimum) {
        return Math.max(Math.max(INITIAL_CAPACITY, reserved), minimum);
    }

    private byte[] ensure(byte[] array, int minimum) {
        if (array == null) {
            return new byte[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private short[] ensure(short[] array, int minimum) {
        if (array == null) {
            return new short[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private int[] ensure(int[] array, int minimum) {
        if (array == null) {
            return new int[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private long[] ensure(long[] array, int minimum) {
        if (array == null) {
            return new long[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private float[] ensure(float[] array, int minimum) {
        if (array == null) {
            return new float[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private double[] ensure(double[] array, int minimum) {
        if (array == null) {
            return new double[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private char[] ensure(char[] array, int minimum) {
        if (array == null) {
            return new char[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private boolean[] ensure(boolean[] array, int minimum) {
        if (array == null) {
            return new boolean[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private Object[] ensure(Object[] array, int minimum) {
        if (array == null) {
            return new Object[initialLength(minimum)];
        }
        return minimum <= array.length
            ? array
            : Arrays.copyOf(array, Math.max(grown(array.length), minimum));
    }

    private void ensureUniformCapacity(int minimum) {

        switch (uniformCode) {
            case BYTE:
                bytes = ensure(bytes, minimum);
                break;
            case SHORT:
                shorts = ensure(shorts, minimum);
                break;
            case INT:
                ints = ensure(ints, minimum);
                break;
            case LONG:
                longs = ensure(longs, minimum);
                break;
            case FLOAT:
                floats = ensure(floats, minimum);
                break;
            case DOUBLE:
                doubles = ensure(doubles, minimum);
                break;
            case CHAR:
                chars = ensure(chars, minimum);
                break;
            case BOOLEAN:
                booleans = ensure(booleans, minimum);
                break;
            default:
                objects = ensure(objects, minimum);
        }
    }

    private Object uniformArray() {

        switch (uniformCode) {
            case BYTE:
                return bytes;
            case SHORT:
                return shorts;
            case INT:
                return ints;
            case LONG:
                return longs;
            case FLOAT:
                return floats;
            case DOUBLE:
                return doubles;
            case CHAR:
                return chars;
            case BOOLEAN:
                return booleans;
            default:
                return objects;
        }
    }

    private boolean stayUniform(int code) {

        if (metadata != null) {
            return false;
        }

        if (uniformCode == code) {
            return true;
        }

        if (size == 0) {
            resetPhysical();
            uniformCode = code;
            return true;
        }

        convertToMixed();
        return false;
    }

    private void convertToMixed() {

        checkPhysicalLimit(size - 1);

        int[] converted = new int[grown(size)];
        int base = uniformCode << TYPE_SHIFT;

        for (int i = 0; i < size; i++) {
            converted[i] = base | i;
        }

        metadata = converted;
        setCount(uniformCode, size);
        uniformCode = NONE;
        orphanCount = 0;
    }

    private void setCount(int code, int count) {

        switch (code) {
            case BYTE:
                byteCount = count;
                break;
            case SHORT:
                shortCount = count;
                break;
            case INT:
                intCount = count;
                break;
            case LONG:
                longCount = count;
                break;
            case FLOAT:
                floatCount = count;
                break;
            case DOUBLE:
                doubleCount = count;
                break;
            case CHAR:
                charCount = count;
                break;
            case BOOLEAN:
                booleanCount = count;
                break;
            case OBJECT:
                objectCount = count;
                break;
            default:
                break;
        }
    }

    private void resetPhysical() {
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
    }

    void addByte(byte value) {
        if (stayUniform(BYTE)) {
            bytes = ensure(bytes, size + 1);
            bytes[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(BYTE, pushByte(value));
    }

    void addShort(short value) {
        if (stayUniform(SHORT)) {
            shorts = ensure(shorts, size + 1);
            shorts[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(SHORT, pushShort(value));
    }

    void addInt(int value) {
        if (stayUniform(INT)) {
            ints = ensure(ints, size + 1);
            ints[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(INT, pushInt(value));
    }

    void addLong(long value) {
        if (stayUniform(LONG)) {
            longs = ensure(longs, size + 1);
            longs[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(LONG, pushLong(value));
    }

    void addFloat(float value) {
        if (stayUniform(FLOAT)) {
            floats = ensure(floats, size + 1);
            floats[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(FLOAT, pushFloat(value));
    }

    void addDouble(double value) {
        if (stayUniform(DOUBLE)) {
            doubles = ensure(doubles, size + 1);
            doubles[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(DOUBLE, pushDouble(value));
    }

    void addChar(char value) {
        if (stayUniform(CHAR)) {
            chars = ensure(chars, size + 1);
            chars[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(CHAR, pushChar(value));
    }

    void addBoolean(boolean value) {
        if (stayUniform(BOOLEAN)) {
            booleans = ensure(booleans, size + 1);
            booleans[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(BOOLEAN, pushBoolean(value));
    }

    private void addObject(Object value) {
        if (stayUniform(OBJECT)) {
            objects = ensure(objects, size + 1);
            objects[size++] = value;
            version++;
            return;
        }
        ensureMetadataCapacity(size + 1);
        commit(OBJECT, pushObject(value));
    }

    void add(Object value) {

        int code = codeOf(value);

        if (code == OBJECT) {
            addObject(value);
            return;
        }

        if (stayUniform(code)) {
            ensureUniformCapacity(size + 1);
            store(code, size, value);
            size++;
            version++;
            return;
        }

        ensureMetadataCapacity(size + 1);
        commit(code, push(code, value));
    }

    void appendFrom(Storage source, int logicalIndex) {

        int code;
        int physical;

        if (source.metadata == null) {
            code = source.uniformCode;
            physical = logicalIndex;
        } else {
            int entry = source.metadata[logicalIndex];
            code = entryType(entry);
            physical = entryPhysical(entry);
        }

        switch (code) {
            case BYTE:
                addByte(source.bytes[physical]);
                break;
            case SHORT:
                addShort(source.shorts[physical]);
                break;
            case INT:
                addInt(source.ints[physical]);
                break;
            case LONG:
                addLong(source.longs[physical]);
                break;
            case FLOAT:
                addFloat(source.floats[physical]);
                break;
            case DOUBLE:
                addDouble(source.doubles[physical]);
                break;
            case CHAR:
                addChar(source.chars[physical]);
                break;
            case BOOLEAN:
                addBoolean(source.booleans[physical]);
                break;
            default:
                addObject(source.objects[physical]);
        }
    }

    void appendRange(Storage source, int from, int to) {

        int count = to - from;

        if (count <= 0) {
            return;
        }

        if (source.metadata == null && stayUniform(source.uniformCode)) {
            ensureUniformCapacity(size + count);
            System.arraycopy(source.uniformArray(), from, uniformArray(), size, count);
            size += count;
            version++;
            return;
        }

        for (int i = from; i < to; i++) {
            appendFrom(source, i);
        }
    }

    void insert(int logicalIndex, Object value) {

        int code = codeOf(value);

        if (stayUniform(code)) {
            ensureUniformCapacity(size + 1);
            Object array = uniformArray();
            System.arraycopy(array, logicalIndex, array, logicalIndex + 1, size - logicalIndex);
            store(code, logicalIndex, value);
            size++;
            version++;
            return;
        }

        ensureMetadataCapacity(size + 1);
        System.arraycopy(metadata, logicalIndex, metadata, logicalIndex + 1, size - logicalIndex);
        metadata[logicalIndex] = pack(code, push(code, value));
        size++;
        version++;
    }

    void insertAll(int logicalIndex, List<?> values) {

        int count = values.size();

        if (count == 0) {
            return;
        }

        int code = codeOf(values.get(0));
        boolean sameType = true;

        for (int i = 1; i < count && sameType; i++) {
            sameType = codeOf(values.get(i)) == code;
        }

        if (sameType && stayUniform(code)) {
            ensureUniformCapacity(size + count);
            Object array = uniformArray();
            System.arraycopy(array, logicalIndex, array, logicalIndex + count, size - logicalIndex);

            for (int i = 0; i < count; i++) {
                store(code, logicalIndex + i, values.get(i));
            }

            size += count;
            version++;
            return;
        }

        if (metadata == null) {
            convertToMixed();
        }

        ensureMetadataCapacity(size + count);
        System.arraycopy(metadata, logicalIndex, metadata, logicalIndex + count, size - logicalIndex);

        for (int i = 0; i < count; i++) {
            Object value = values.get(i);
            int valueCode = codeOf(value);
            metadata[logicalIndex + i] = pack(valueCode, push(valueCode, value));
        }

        size += count;
        version++;
    }

    Object get(int logicalIndex) {

        if (metadata == null) {
            return getPhysical(uniformCode, logicalIndex);
        }

        int entry = metadata[logicalIndex];
        return getPhysical(entryType(entry), entryPhysical(entry));
    }

    private Object getPhysical(int code, int physical) {

        switch (code) {
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
        return DataType.fromCode(
            metadata == null ? uniformCode : entryType(metadata[logicalIndex])
        );
    }

    private int matchingPhysical(int logicalIndex, int code) {

        if (metadata == null) {
            return uniformCode == code ? logicalIndex : -1;
        }

        int entry = metadata[logicalIndex];
        return entryType(entry) == code ? entryPhysical(entry) : -1;
    }

    private int physicalFor(int logicalIndex, int code) {

        int physical = matchingPhysical(logicalIndex, code);

        if (physical < 0) {
            throw new IllegalStateException(
                "Element at index " + logicalIndex + " is " + typeAt(logicalIndex)
                + ", not " + DataType.fromCode(code) + "."
            );
        }

        return physical;
    }

    byte getByte(int logicalIndex) {
        return bytes[physicalFor(logicalIndex, BYTE)];
    }

    short getShort(int logicalIndex) {
        return shorts[physicalFor(logicalIndex, SHORT)];
    }

    int getInt(int logicalIndex) {
        return ints[physicalFor(logicalIndex, INT)];
    }

    long getLong(int logicalIndex) {
        return longs[physicalFor(logicalIndex, LONG)];
    }

    float getFloat(int logicalIndex) {
        return floats[physicalFor(logicalIndex, FLOAT)];
    }

    double getDouble(int logicalIndex) {
        return doubles[physicalFor(logicalIndex, DOUBLE)];
    }

    char getChar(int logicalIndex) {
        return chars[physicalFor(logicalIndex, CHAR)];
    }

    boolean getBoolean(int logicalIndex) {
        return booleans[physicalFor(logicalIndex, BOOLEAN)];
    }

    void setByte(int logicalIndex, byte value) {
        int physical = matchingPhysical(logicalIndex, BYTE);
        if (physical >= 0) {
            bytes[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setShort(int logicalIndex, short value) {
        int physical = matchingPhysical(logicalIndex, SHORT);
        if (physical >= 0) {
            shorts[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setInt(int logicalIndex, int value) {
        int physical = matchingPhysical(logicalIndex, INT);
        if (physical >= 0) {
            ints[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setLong(int logicalIndex, long value) {
        int physical = matchingPhysical(logicalIndex, LONG);
        if (physical >= 0) {
            longs[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setFloat(int logicalIndex, float value) {
        int physical = matchingPhysical(logicalIndex, FLOAT);
        if (physical >= 0) {
            floats[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setDouble(int logicalIndex, double value) {
        int physical = matchingPhysical(logicalIndex, DOUBLE);
        if (physical >= 0) {
            doubles[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setChar(int logicalIndex, char value) {
        int physical = matchingPhysical(logicalIndex, CHAR);
        if (physical >= 0) {
            chars[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void setBoolean(int logicalIndex, boolean value) {
        int physical = matchingPhysical(logicalIndex, BOOLEAN);
        if (physical >= 0) {
            booleans[physical] = value;
        } else {
            set(logicalIndex, value);
        }
    }

    void set(int logicalIndex, Object value) {

        int code = codeOf(value);
        int physical = matchingPhysical(logicalIndex, code);

        if (physical >= 0) {
            store(code, physical, value);
            return;
        }

        if (metadata == null) {
            convertToMixed();
        }

        int entry = metadata[logicalIndex];
        release(entryType(entry), entryPhysical(entry));
        metadata[logicalIndex] = pack(code, push(code, value));
        compactIfNeeded();
    }

    void fill(int from, int to, Object value) {

        int code = codeOf(value);

        if (metadata != null || uniformCode != code) {

            if (from != 0 || to != size || size == 0) {
                for (int i = from; i < to; i++) {
                    set(i, value);
                }
                return;
            }

            resetPhysical();
            metadata = null;
            uniformCode = code;
            orphanCount = 0;
            ensureUniformCapacity(size);
        }

        for (int i = from; i < to; i++) {
            store(code, i, value);
        }
    }

    void removeAt(int logicalIndex) {

        if (metadata == null) {
            Object array = uniformArray();
            System.arraycopy(array, logicalIndex + 1, array, logicalIndex, size - logicalIndex - 1);
            size--;

            if (uniformCode == OBJECT) {
                objects[size] = null;
            }

            version++;
            return;
        }

        int entry = metadata[logicalIndex];
        release(entryType(entry), entryPhysical(entry));
        System.arraycopy(metadata, logicalIndex + 1, metadata, logicalIndex, size - logicalIndex - 1);

        size--;
        version++;

        compactIfNeeded();
    }

    int removeEqual(Object value) {

        int code = codeOf(value);

        if (metadata == null) {
            return removeEqualUniform(code, value);
        }

        int write = 0;

        for (int read = 0; read < size; read++) {

            int entry = metadata[read];

            if (entryType(entry) == code && equalsPhysical(code, entryPhysical(entry), value)) {
                release(code, entryPhysical(entry));
            } else {
                metadata[write++] = entry;
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

    private int removeEqualUniform(int code, Object value) {

        if (code != uniformCode) {
            return 0;
        }

        Object array = uniformArray();
        int write = 0;

        for (int read = 0; read < size; read++) {

            if (!equalsPhysical(code, read, value)) {

                if (write != read) {
                    System.arraycopy(array, read, array, write, 1);
                }

                write++;
            }
        }

        int removed = size - write;

        if (uniformCode == OBJECT) {
            Arrays.fill(objects, write, size, null);
        }

        size = write;

        if (removed > 0) {
            version++;
        }

        return removed;
    }

    int indexOf(Object value, int from, int to) {

        int code = codeOf(value);

        if (metadata == null) {
            return code == uniformCode ? indexOfUniform(code, value, from, to) : -1;
        }

        for (int i = from; i < to; i++) {
            if (matches(i, code, value)) {
                return i;
            }
        }

        return -1;
    }

    private int indexOfUniform(int code, Object value, int from, int to) {

        switch (code) {
            case BYTE: {
                byte target = (Byte) value;
                for (int i = from; i < to; i++) {
                    if (bytes[i] == target) {
                        return i;
                    }
                }
                return -1;
            }
            case SHORT: {
                short target = (Short) value;
                for (int i = from; i < to; i++) {
                    if (shorts[i] == target) {
                        return i;
                    }
                }
                return -1;
            }
            case INT: {
                int target = (Integer) value;
                for (int i = from; i < to; i++) {
                    if (ints[i] == target) {
                        return i;
                    }
                }
                return -1;
            }
            case LONG: {
                long target = (Long) value;
                for (int i = from; i < to; i++) {
                    if (longs[i] == target) {
                        return i;
                    }
                }
                return -1;
            }
            case FLOAT: {
                int target = Float.floatToIntBits((Float) value);
                for (int i = from; i < to; i++) {
                    if (Float.floatToIntBits(floats[i]) == target) {
                        return i;
                    }
                }
                return -1;
            }
            case DOUBLE: {
                long target = Double.doubleToLongBits((Double) value);
                for (int i = from; i < to; i++) {
                    if (Double.doubleToLongBits(doubles[i]) == target) {
                        return i;
                    }
                }
                return -1;
            }
            case CHAR: {
                char target = (Character) value;
                for (int i = from; i < to; i++) {
                    if (chars[i] == target) {
                        return i;
                    }
                }
                return -1;
            }
            case BOOLEAN: {
                boolean target = (Boolean) value;
                for (int i = from; i < to; i++) {
                    if (booleans[i] == target) {
                        return i;
                    }
                }
                return -1;
            }
            default: {
                for (int i = from; i < to; i++) {
                    if (value == null ? objects[i] == null : value.equals(objects[i])) {
                        return i;
                    }
                }
                return -1;
            }
        }
    }

    int lastIndexOf(Object value, int from, int to) {

        int code = codeOf(value);

        for (int i = to - 1; i >= from; i--) {
            if (matches(i, code, value)) {
                return i;
            }
        }

        return -1;
    }

    int count(Object value, int from, int to) {

        int code = codeOf(value);
        int count = 0;

        for (int i = from; i < to; i++) {
            if (matches(i, code, value)) {
                count++;
            }
        }

        return count;
    }

    boolean equalsAt(int logicalIndex, Object value) {
        return matches(logicalIndex, codeOf(value), value);
    }

    private boolean matches(int logicalIndex, int code, Object value) {
        int physical = matchingPhysical(logicalIndex, code);
        return physical >= 0 && equalsPhysical(code, physical, value);
    }

    private boolean equalsPhysical(int code, int physical, Object value) {

        switch (code) {
            case BYTE:
                return bytes[physical] == (Byte) value;
            case SHORT:
                return shorts[physical] == (Short) value;
            case INT:
                return ints[physical] == (Integer) value;
            case LONG:
                return longs[physical] == (Long) value;
            case FLOAT:
                return Float.floatToIntBits(floats[physical])
                    == Float.floatToIntBits((Float) value);
            case DOUBLE:
                return Double.doubleToLongBits(doubles[physical])
                    == Double.doubleToLongBits((Double) value);
            case CHAR:
                return chars[physical] == (Character) value;
            case BOOLEAN:
                return booleans[physical] == (Boolean) value;
            default:
                return value == null
                    ? objects[physical] == null
                    : value.equals(objects[physical]);
        }
    }

    Storage copyRange(int from, int to) {

        Storage copy = new Storage();
        int count = to - from;

        if (count <= 0) {
            return copy;
        }

        if (metadata == null) {

            copy.uniformCode = uniformCode;
            copy.size = count;

            switch (uniformCode) {
                case BYTE:
                    copy.bytes = Arrays.copyOfRange(bytes, from, to);
                    break;
                case SHORT:
                    copy.shorts = Arrays.copyOfRange(shorts, from, to);
                    break;
                case INT:
                    copy.ints = Arrays.copyOfRange(ints, from, to);
                    break;
                case LONG:
                    copy.longs = Arrays.copyOfRange(longs, from, to);
                    break;
                case FLOAT:
                    copy.floats = Arrays.copyOfRange(floats, from, to);
                    break;
                case DOUBLE:
                    copy.doubles = Arrays.copyOfRange(doubles, from, to);
                    break;
                case CHAR:
                    copy.chars = Arrays.copyOfRange(chars, from, to);
                    break;
                case BOOLEAN:
                    copy.booleans = Arrays.copyOfRange(booleans, from, to);
                    break;
                default:
                    copy.objects = Arrays.copyOfRange(objects, from, to);
            }

            return copy;
        }

        copy.ensureCapacity(count);

        for (int i = from; i < to; i++) {
            copy.appendFrom(this, i);
        }

        return copy;
    }

    void swap(int first, int second) {

        if (metadata != null) {
            int entry = metadata[first];
            metadata[first] = metadata[second];
            metadata[second] = entry;
            return;
        }

        switch (uniformCode) {
            case BYTE: {
                byte temp = bytes[first];
                bytes[first] = bytes[second];
                bytes[second] = temp;
                break;
            }
            case SHORT: {
                short temp = shorts[first];
                shorts[first] = shorts[second];
                shorts[second] = temp;
                break;
            }
            case INT: {
                int temp = ints[first];
                ints[first] = ints[second];
                ints[second] = temp;
                break;
            }
            case LONG: {
                long temp = longs[first];
                longs[first] = longs[second];
                longs[second] = temp;
                break;
            }
            case FLOAT: {
                float temp = floats[first];
                floats[first] = floats[second];
                floats[second] = temp;
                break;
            }
            case DOUBLE: {
                double temp = doubles[first];
                doubles[first] = doubles[second];
                doubles[second] = temp;
                break;
            }
            case CHAR: {
                char temp = chars[first];
                chars[first] = chars[second];
                chars[second] = temp;
                break;
            }
            case BOOLEAN: {
                boolean temp = booleans[first];
                booleans[first] = booleans[second];
                booleans[second] = temp;
                break;
            }
            default: {
                Object temp = objects[first];
                objects[first] = objects[second];
                objects[second] = temp;
            }
        }
    }

    void clear() {
        resetPhysical();
        metadata = null;
        uniformCode = NONE;
        size = 0;
        orphanCount = 0;
        reserved = 0;
        version++;
    }

    private void commit(int code, int physical) {
        metadata[size] = pack(code, physical);
        size++;
        version++;
    }

    private int push(int code, Object value) {

        switch (code) {
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

    private void store(int code, int index, Object value) {

        switch (code) {
            case BYTE:
                bytes[index] = (Byte) value;
                break;
            case SHORT:
                shorts[index] = (Short) value;
                break;
            case INT:
                ints[index] = (Integer) value;
                break;
            case LONG:
                longs[index] = (Long) value;
                break;
            case FLOAT:
                floats[index] = (Float) value;
                break;
            case DOUBLE:
                doubles[index] = (Double) value;
                break;
            case CHAR:
                chars[index] = (Character) value;
                break;
            case BOOLEAN:
                booleans[index] = (Boolean) value;
                break;
            default:
                objects[index] = value;
        }
    }

    private void release(int code, int physical) {

        if (code == OBJECT) {
            objects[physical] = null;
        }

        orphanCount++;
    }

    private void compactIfNeeded() {

        if (metadata != null && orphanCount > Math.max(MINIMUM_ORPHAN_LIMIT, size)) {
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

        metadata = compacted.metadata;
        uniformCode = compacted.uniformCode;
        orphanCount = 0;
    }

    private int pushByte(byte value) {
        checkPhysicalLimit(byteCount);
        bytes = ensure(bytes, byteCount + 1);
        bytes[byteCount] = value;
        return byteCount++;
    }

    private int pushShort(short value) {
        checkPhysicalLimit(shortCount);
        shorts = ensure(shorts, shortCount + 1);
        shorts[shortCount] = value;
        return shortCount++;
    }

    private int pushInt(int value) {
        checkPhysicalLimit(intCount);
        ints = ensure(ints, intCount + 1);
        ints[intCount] = value;
        return intCount++;
    }

    private int pushLong(long value) {
        checkPhysicalLimit(longCount);
        longs = ensure(longs, longCount + 1);
        longs[longCount] = value;
        return longCount++;
    }

    private int pushFloat(float value) {
        checkPhysicalLimit(floatCount);
        floats = ensure(floats, floatCount + 1);
        floats[floatCount] = value;
        return floatCount++;
    }

    private int pushDouble(double value) {
        checkPhysicalLimit(doubleCount);
        doubles = ensure(doubles, doubleCount + 1);
        doubles[doubleCount] = value;
        return doubleCount++;
    }

    private int pushChar(char value) {
        checkPhysicalLimit(charCount);
        chars = ensure(chars, charCount + 1);
        chars[charCount] = value;
        return charCount++;
    }

    private int pushBoolean(boolean value) {
        checkPhysicalLimit(booleanCount);
        booleans = ensure(booleans, booleanCount + 1);
        booleans[booleanCount] = value;
        return booleanCount++;
    }

    private int pushObject(Object value) {
        checkPhysicalLimit(objectCount);
        objects = ensure(objects, objectCount + 1);
        objects[objectCount] = value;
        return objectCount++;
    }
}