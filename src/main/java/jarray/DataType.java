package jarray;

public enum DataType {

    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    CHAR,
    BOOLEAN,
    OBJECT;

    private static final DataType[] VALUES = values();

    static {
        if (BYTE.ordinal() != Storage.BYTE
            || SHORT.ordinal() != Storage.SHORT
            || INT.ordinal() != Storage.INT
            || LONG.ordinal() != Storage.LONG
            || FLOAT.ordinal() != Storage.FLOAT
            || DOUBLE.ordinal() != Storage.DOUBLE
            || CHAR.ordinal() != Storage.CHAR
            || BOOLEAN.ordinal() != Storage.BOOLEAN
            || OBJECT.ordinal() != Storage.OBJECT) {
            throw new IllegalStateException(
                "DataType order must match the type codes in Storage."
            );
        }
    }

    static DataType fromCode(int code) {
        return VALUES[code];
    }

    static DataType of(Object value) {
        return VALUES[Storage.codeOf(value)];
    }

    public boolean isPrimitive() {
        return this != OBJECT;
    }
}