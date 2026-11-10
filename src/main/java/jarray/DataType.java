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

    byte code() {
        return (byte) ordinal();
    }

    static DataType fromCode(byte code) {
        return VALUES[code];
    }

    static DataType of(Object value) {

        if (value instanceof Byte) {
            return BYTE;
        }
        if (value instanceof Short) {
            return SHORT;
        }
        if (value instanceof Integer) {
            return INT;
        }
        if (value instanceof Long) {
            return LONG;
        }
        if (value instanceof Float) {
            return FLOAT;
        }
        if (value instanceof Double) {
            return DOUBLE;
        }
        if (value instanceof Character) {
            return CHAR;
        }
        if (value instanceof Boolean) {
            return BOOLEAN;
        }

        return OBJECT;
    }

    public boolean isPrimitive() {
        return this != OBJECT;
    }
}