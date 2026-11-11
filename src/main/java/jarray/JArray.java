package jarray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class JArray implements Iterable<Object> {

    private final Storage storage;
    private final boolean view;
    private final boolean contiguous;
    private final int viewSize;
    private final int expectedVersion;
    private final int offset;

    private int[] dimensions;
    private int[] strides;
    private boolean tracksSize;

    public JArray() {
        this(new int[0]);
    }

    public JArray(int... shape) {

        this.storage = new Storage();
        this.view = false;
        this.contiguous = true;
        this.viewSize = 0;
        this.expectedVersion = 0;
        this.offset = 0;

        if (shape == null || shape.length == 0) {
            this.dimensions = new int[]{0};
            this.strides = new int[]{1};
            this.tracksSize = true;
        } else {
            validateShape(shape);

            if (shape.length == 1) {
                this.dimensions = new int[]{0};
                this.strides = new int[]{1};
                this.tracksSize = true;
                this.storage.ensureCapacity(shape[0]);
            } else {
                this.dimensions = Arrays.copyOf(shape, shape.length);
                this.strides = computeStrides(this.dimensions);
                this.tracksSize = false;
            }
        }
    }

    private JArray(Storage storage, int[] shape) {

        this.storage = storage;
        this.view = false;
        this.contiguous = true;
        this.viewSize = 0;
        this.expectedVersion = 0;
        this.offset = 0;

        if (shape.length == 1) {
            this.dimensions = new int[]{storage.size()};
            this.strides = new int[]{1};
            this.tracksSize = true;
        } else {
            this.dimensions = Arrays.copyOf(shape, shape.length);
            this.strides = computeStrides(this.dimensions);
            this.tracksSize = false;
        }
    }

    private JArray(Storage storage, int[] shape, int[] strides, int offset) {
        this.storage = storage;
        this.view = true;
        this.dimensions = shape;
        this.strides = strides;
        this.offset = offset;
        this.tracksSize = false;
        this.viewSize = computeSize(shape);
        this.contiguous = isRowMajor(shape, strides);
        this.expectedVersion = storage.version();
    }

    public static JArray of(Object... values) {
        JArray result = new JArray();
        result.addAll(Arrays.asList(values));
        return result;
    }

    public static JArray filled(Object value, int... shape) {

        JArray result = new JArray(shape);
        int count = shape == null || shape.length == 0 ? 0 : computeSize(shape);

        for (int i = 0; i < count; i++) {
            result.add(value);
        }

        return result;
    }

    private static void validateShape(int[] shape) {

        for (int dimension : shape) {
            if (dimension < 0) {
                throw new IllegalArgumentException(
                    "Shape dimensions cannot be negative."
                );
            }
        }
    }

    private static int[] computeStrides(int[] shape) {

        int[] strides = new int[shape.length];
        int stride = 1;

        for (int i = shape.length - 1; i >= 0; i--) {
            strides[i] = stride;
            stride = Math.multiplyExact(stride, shape[i]);
        }

        return strides;
    }

    private static int computeSize(int[] shape) {

        int size = 1;

        for (int dimension : shape) {
            size = Math.multiplyExact(size, dimension);
        }

        return size;
    }

    private static boolean isRowMajor(int[] shape, int[] strides) {

        int expected = 1;

        for (int axis = shape.length - 1; axis >= 0; axis--) {

            if (shape[axis] != 1 && strides[axis] != expected) {
                return false;
            }

            expected *= shape[axis];
        }

        return true;
    }

    private static int[] resolveShape(int[] requested, int total) {

        if (requested == null || requested.length == 0) {
            throw new IllegalArgumentException("Shape cannot be empty.");
        }

        int[] resolved = Arrays.copyOf(requested, requested.length);
        int inferredAxis = -1;
        int known = 1;

        for (int i = 0; i < resolved.length; i++) {

            if (resolved[i] == -1) {
                if (inferredAxis != -1) {
                    throw new IllegalArgumentException(
                        "Only one dimension can be inferred."
                    );
                }
                inferredAxis = i;
            } else if (resolved[i] < 0) {
                throw new IllegalArgumentException(
                    "Shape dimensions cannot be negative."
                );
            } else {
                known = Math.multiplyExact(known, resolved[i]);
            }
        }

        if (inferredAxis >= 0) {
            if (known == 0 || total % known != 0) {
                throw new IllegalArgumentException(
                    "Cannot reshape " + total + " elements into "
                    + Arrays.toString(requested)
                );
            }
            resolved[inferredAxis] = total / known;
        } else if (known != total) {
            throw new IllegalArgumentException(
                "Cannot reshape " + total + " elements into "
                + Arrays.toString(requested)
            );
        }

        return resolved;
    }

    private static List<Object> snapshot(Iterable<?> values) {

        List<Object> copy = new ArrayList<>();

        for (Object value : values) {
            copy.add(value);
        }

        return copy;
    }

    private int[] dims() {

        if (tracksSize) {
            dimensions[0] = storage.size();
        }

        return dimensions;
    }

    private void requireOwner() {
        if (view) {
            throw new UnsupportedOperationException(
                "Views cannot change the size of the array."
            );
        }
    }

    private void requireValid() {
        if (view && storage.version() != expectedVersion) {
            throw new ConcurrentModificationException(
                "The underlying array was structurally modified after this view was created."
            );
        }
    }

    private void requireFull() {

        requireValid();

        if (getSize() != computeSize(dims())) {
            throw new IllegalStateException(
                "Array is not completely filled. Shape: " + Arrays.toString(dims())
                + ", size: " + getSize()
            );
        }
    }

    private void checkAxis(int axis) {
        if (axis < 0 || axis >= dimensions.length) {
            throw new IllegalArgumentException(
                "Axis " + axis + " is out of range for " + dimensions.length
                + " dimensions."
            );
        }
    }

    private void prepareToAdd(int count) {

        if (view) {
            throw new UnsupportedOperationException(
                "Views cannot change the size of the array."
            );
        }

        if (!tracksSize && storage.size() + count > computeSize(dimensions)) {
            throw new IllegalStateException(
                "Array is full. Shape: " + Arrays.toString(dimensions)
            );
        }
    }

    private void checkInsertIndex(int index) {
        if (index < 0 || index > storage.size()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + storage.size()
            );
        }
    }

    private int resolve(int flatIndex) {

        requireValid();

        int size = getSize();

        if (flatIndex < 0 || flatIndex >= size) {
            throw new IndexOutOfBoundsException(
                "Flat index " + flatIndex + " out of bounds for size " + size
            );
        }

        if (contiguous) {
            return offset + flatIndex;
        }

        int position = offset;
        int remaining = flatIndex;

        for (int axis = dimensions.length - 1; axis >= 0; axis--) {
            position += (remaining % dimensions[axis]) * strides[axis];
            remaining /= dimensions[axis];
        }

        return position;
    }

    private int position(int index) {

        requireValid();

        if (dimensions.length != 1) {
            throw new IllegalArgumentException(
                "Expected " + dimensions.length + " indices, but got 1"
            );
        }

        int size = view ? viewSize : storage.size();

        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for axis 0 with size " + size
            );
        }

        return view ? offset + index * strides[0] : index;
    }

    private int storageIndex(int... indices) {

        requireValid();

        int[] shape = dims();

        if (indices.length != shape.length) {
            throw new IllegalArgumentException(
                "Expected " + shape.length + " indices, but got " + indices.length
            );
        }

        int position = offset;

        for (int i = 0; i < indices.length; i++) {

            if (indices[i] < 0 || indices[i] >= shape[i]) {
                throw new IndexOutOfBoundsException(
                    "Index " + indices[i] + " out of bounds for axis " + i
                    + " with size " + shape[i]
                );
            }

            position += indices[i] * strides[i];
        }

        if (position >= storage.size()) {
            throw new IndexOutOfBoundsException(
                "Element at flat index " + position + " has not been added yet."
            );
        }

        return position;
    }

    private void appendRange(JArray source, int from, int to) {

        if (source.contiguous) {
            source.requireValid();
            storage.appendRange(source.storage, source.offset + from, source.offset + to);
            return;
        }

        storage.ensureCapacity(storage.size() + (to - from));

        for (int i = from; i < to; i++) {
            storage.appendFrom(source.storage, source.resolve(i));
        }
    }

    private Storage copyElements() {

        int size = getSize();

        if (contiguous) {
            return storage.copyRange(offset, offset + size);
        }

        Storage result = new Storage();
        result.ensureCapacity(size);

        for (int i = 0; i < size; i++) {
            result.appendFrom(storage, resolve(i));
        }

        return result;
    }

    private JArray removeAxis(int axis, int index) {

        int[] shape = dims();

        if (shape.length == 1) {
            return new JArray(
                storage,
                new int[]{1},
                new int[]{1},
                offset + index * strides[0]
            );
        }

        int[] newShape = new int[shape.length - 1];
        int[] newStrides = new int[shape.length - 1];

        for (int i = 0, j = 0; i < shape.length; i++) {

            if (i == axis) {
                continue;
            }

            newShape[j] = shape[i];
            newStrides[j] = strides[i];
            j++;
        }

        return new JArray(storage, newShape, newStrides, offset + index * strides[axis]);
    }

    public void add(byte value) {
        prepareToAdd(1);
        storage.addByte(value);
    }

    public void add(short value) {
        prepareToAdd(1);
        storage.addShort(value);
    }

    public void add(int value) {
        prepareToAdd(1);
        storage.addInt(value);
    }

    public void add(long value) {
        prepareToAdd(1);
        storage.addLong(value);
    }

    public void add(float value) {
        prepareToAdd(1);
        storage.addFloat(value);
    }

    public void add(double value) {
        prepareToAdd(1);
        storage.addDouble(value);
    }

    public void add(char value) {
        prepareToAdd(1);
        storage.addChar(value);
    }

    public void add(boolean value) {
        prepareToAdd(1);
        storage.addBoolean(value);
    }

    public void add(Object value) {
        prepareToAdd(1);
        storage.add(value);
    }

    public void addAll(Iterable<?> values) {

        List<Object> snapshot = snapshot(values);

        prepareToAdd(snapshot.size());
        storage.insertAll(storage.size(), snapshot);
    }

    public void addAll(Object... values) {
        addAll(Arrays.asList(values));
    }

    public void insert(int index, Object value) {

        prepareToAdd(1);
        checkInsertIndex(index);

        storage.insert(index, value);
    }

    public void insertAll(int index, Iterable<?> values) {

        List<Object> snapshot = snapshot(values);

        prepareToAdd(snapshot.size());
        checkInsertIndex(index);

        storage.insertAll(index, snapshot);
    }

    public void insertAll(int index, Object... values) {
        insertAll(index, Arrays.asList(values));
    }

    public Object get(int index) {
        return storage.get(position(index));
    }

    public Object get(int... indices) {
        return storage.get(storageIndex(indices));
    }

    public Object getFlat(int flatIndex) {
        return storage.get(resolve(flatIndex));
    }

    public byte getByte(int index) {
        return storage.getByte(position(index));
    }

    public byte getByte(int... indices) {
        return storage.getByte(storageIndex(indices));
    }

    public short getShort(int index) {
        return storage.getShort(position(index));
    }

    public short getShort(int... indices) {
        return storage.getShort(storageIndex(indices));
    }

    public int getInt(int index) {
        return storage.getInt(position(index));
    }

    public int getInt(int... indices) {
        return storage.getInt(storageIndex(indices));
    }

    public long getLong(int index) {
        return storage.getLong(position(index));
    }

    public long getLong(int... indices) {
        return storage.getLong(storageIndex(indices));
    }

    public float getFloat(int index) {
        return storage.getFloat(position(index));
    }

    public float getFloat(int... indices) {
        return storage.getFloat(storageIndex(indices));
    }

    public double getDouble(int index) {
        return storage.getDouble(position(index));
    }

    public double getDouble(int... indices) {
        return storage.getDouble(storageIndex(indices));
    }

    public char getChar(int index) {
        return storage.getChar(position(index));
    }

    public char getChar(int... indices) {
        return storage.getChar(storageIndex(indices));
    }

    public boolean getBoolean(int index) {
        return storage.getBoolean(position(index));
    }

    public boolean getBoolean(int... indices) {
        return storage.getBoolean(storageIndex(indices));
    }

    public void set(Object value, int index) {
        storage.set(position(index), value);
    }

    public void set(Object value, int... indices) {
        storage.set(storageIndex(indices), value);
    }

    public void setFlat(Object value, int flatIndex) {
        storage.set(resolve(flatIndex), value);
    }

    public void setByte(byte value, int index) {
        storage.setByte(position(index), value);
    }

    public void setByte(byte value, int... indices) {
        storage.setByte(storageIndex(indices), value);
    }

    public void setShort(short value, int index) {
        storage.setShort(position(index), value);
    }

    public void setShort(short value, int... indices) {
        storage.setShort(storageIndex(indices), value);
    }

    public void setInt(int value, int index) {
        storage.setInt(position(index), value);
    }

    public void setInt(int value, int... indices) {
        storage.setInt(storageIndex(indices), value);
    }

    public void setLong(long value, int index) {
        storage.setLong(position(index), value);
    }

    public void setLong(long value, int... indices) {
        storage.setLong(storageIndex(indices), value);
    }

    public void setFloat(float value, int index) {
        storage.setFloat(position(index), value);
    }

    public void setFloat(float value, int... indices) {
        storage.setFloat(storageIndex(indices), value);
    }

    public void setDouble(double value, int index) {
        storage.setDouble(position(index), value);
    }

    public void setDouble(double value, int... indices) {
        storage.setDouble(storageIndex(indices), value);
    }

    public void setChar(char value, int index) {
        storage.setChar(position(index), value);
    }

    public void setChar(char value, int... indices) {
        storage.setChar(storageIndex(indices), value);
    }

    public void setBoolean(boolean value, int index) {
        storage.setBoolean(position(index), value);
    }

    public void setBoolean(boolean value, int... indices) {
        storage.setBoolean(storageIndex(indices), value);
    }

    public void removeAt(int index) {

        requireOwner();

        if (index < 0 || index >= storage.size()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size " + storage.size()
            );
        }

        storage.removeAt(index);
    }

    public boolean remove(Object value) {

        requireOwner();

        int index = indexOf(value);

        if (index < 0) {
            return false;
        }

        storage.removeAt(index);
        return true;
    }

    public int removeAll(Object value) {
        requireOwner();
        return storage.removeEqual(value);
    }

    public void clear() {
        requireOwner();
        storage.clear();
    }

    public void ensureCapacity(int minimumCapacity) {
        requireOwner();
        storage.ensureCapacity(minimumCapacity);
    }

    public boolean contains(Object value) {
        return indexOf(value) >= 0;
    }

    public int indexOf(Object value) {

        requireValid();

        int size = getSize();

        if (contiguous) {
            int found = storage.indexOf(value, offset, offset + size);
            return found < 0 ? -1 : found - offset;
        }

        for (int i = 0; i < size; i++) {
            if (storage.equalsAt(resolve(i), value)) {
                return i;
            }
        }

        return -1;
    }

    public int lastIndexOf(Object value) {

        requireValid();

        int size = getSize();

        if (contiguous) {
            int found = storage.lastIndexOf(value, offset, offset + size);
            return found < 0 ? -1 : found - offset;
        }

        for (int i = size - 1; i >= 0; i--) {
            if (storage.equalsAt(resolve(i), value)) {
                return i;
            }
        }

        return -1;
    }

    public int count(Object value) {

        requireValid();

        int size = getSize();

        if (contiguous) {
            return storage.count(value, offset, offset + size);
        }

        int count = 0;

        for (int i = 0; i < size; i++) {
            if (storage.equalsAt(resolve(i), value)) {
                count++;
            }
        }

        return count;
    }

    public boolean isEmpty() {
        return getSize() == 0;
    }

    public DataType getType(int... indices) {
        return storage.typeAt(storageIndex(indices));
    }

    public DataType getFlatType(int flatIndex) {
        return storage.typeAt(resolve(flatIndex));
    }

    public int getCapacity() {

        if (view) {
            return viewSize;
        }

        if (tracksSize) {
            return storage.capacity();
        }

        return computeSize(dimensions);
    }

    public JArray copy() {
        requireValid();
        return new JArray(copyElements(), dims());
    }

    public JArray flatten() {
        requireValid();
        return new JArray(copyElements(), new int[]{getSize()});
    }

    public JArray reshape(int... newShape) {

        requireFull();

        int[] resolved = resolveShape(newShape, getSize());

        if (!contiguous) {
            return copy().reshape(resolved);
        }

        return new JArray(storage, resolved, computeStrides(resolved), offset);
    }

    public void reverse() {

        for (int i = 0, j = getSize() - 1; i < j; i++, j--) {
            swapFlat(i, j);
        }
    }

    public JArray concat(JArray other) {
        return concat(other, 0);
    }

    public JArray concat(JArray other, int axis) {

        requireFull();
        other.requireFull();
        checkAxis(axis);

        int[] shape = dims();
        int[] otherShape = other.dims();

        if (otherShape.length != shape.length) {
            throw new IllegalArgumentException(
                "Arrays must have the same number of dimensions."
            );
        }

        for (int i = 0; i < shape.length; i++) {
            if (i != axis && shape[i] != otherShape[i]) {
                throw new IllegalArgumentException(
                    "Shapes " + Arrays.toString(shape) + " and "
                    + Arrays.toString(otherShape)
                    + " are not compatible along axis " + axis + "."
                );
            }
        }

        int[] resultShape = Arrays.copyOf(shape, shape.length);
        resultShape[axis] = shape[axis] + otherShape[axis];

        int outer = 1;
        for (int i = 0; i < axis; i++) {
            outer *= shape[i];
        }

        int inner = 1;
        for (int i = axis + 1; i < shape.length; i++) {
            inner *= shape[i];
        }

        int firstBlock = shape[axis] * inner;
        int secondBlock = otherShape[axis] * inner;

        JArray result = new JArray(resultShape);

        for (int i = 0; i < outer; i++) {
            result.appendRange(this, i * firstBlock, (i + 1) * firstBlock);
            result.appendRange(other, i * secondBlock, (i + 1) * secondBlock);
        }

        return result;
    }

    public void fill(Object value) {

        requireValid();

        int size = getSize();

        if (contiguous) {
            storage.fill(offset, offset + size, value);
            return;
        }

        for (int i = 0; i < size; i++) {
            setFlat(value, i);
        }
    }

    public void swap(int[] first, int[] second) {
        storage.swap(storageIndex(first), storageIndex(second));
    }

    public void swapFlat(int firstIndex, int secondIndex) {
        storage.swap(resolve(firstIndex), resolve(secondIndex));
    }

    @Override
    public Iterator<Object> iterator() {

        requireValid();

        final int expected = storage.version();
        final int size = getSize();

        return new Iterator<Object>() {

            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public Object next() {

                if (storage.version() != expected) {
                    throw new ConcurrentModificationException();
                }

                if (cursor >= size) {
                    throw new NoSuchElementException();
                }

                int index = cursor++;

                return storage.get(contiguous ? offset + index : resolve(index));
            }
        };
    }

    public Object[] toArray() {

        int size = getSize();
        Object[] result = new Object[size];

        for (int i = 0; i < size; i++) {
            result[i] = getFlat(i);
        }

        return result;
    }

    public List<Object> toList() {
        return new ArrayList<>(Arrays.asList(toArray()));
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof JArray)) {
            return false;
        }

        JArray that = (JArray) other;
        int size = getSize();

        if (!Arrays.equals(dims(), that.dims()) || size != that.getSize()) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (!Objects.equals(getFlat(i), that.getFlat(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = Arrays.hashCode(dims());
        int size = getSize();

        for (int i = 0; i < size; i++) {
            result = 31 * result + Objects.hashCode(getFlat(i));
        }

        return result;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        if (getSize() != computeSize(dims())) {
            builder.append("JArray(shape=")
                   .append(Arrays.toString(dims()))
                   .append(", size=")
                   .append(getSize())
                   .append(") ")
                   .append(Arrays.toString(toArray()));
        } else {
            appendNested(builder, dims(), 0, 0);
        }

        return builder.toString();
    }

    private void appendNested(StringBuilder builder, int[] shape, int axis, int start) {

        int block = 1;
        for (int i = axis + 1; i < shape.length; i++) {
            block *= shape[i];
        }

        builder.append('[');

        for (int i = 0; i < shape[axis]; i++) {

            if (i > 0) {
                builder.append(", ");
            }

            int position = start + i * block;

            if (axis == shape.length - 1) {
                builder.append(getFlat(position));
            } else {
                appendNested(builder, shape, axis + 1, position);
            }
        }

        builder.append(']');
    }

    public JArray view() {
        requireFull();
        return new JArray(
            storage,
            Arrays.copyOf(dims(), dimensions.length),
            Arrays.copyOf(strides, strides.length),
            offset
        );
    }

    public boolean isView() {
        return view;
    }

    public JArray transpose() {

        int[] axes = new int[dimensions.length];

        for (int i = 0; i < axes.length; i++) {
            axes[i] = axes.length - 1 - i;
        }

        return transpose(axes);
    }

    public JArray transpose(int... axes) {

        requireFull();

        int[] shape = dims();

        if (axes == null || axes.length != shape.length) {
            throw new IllegalArgumentException(
                "Expected " + shape.length + " axes."
            );
        }

        boolean[] seen = new boolean[shape.length];
        int[] newShape = new int[shape.length];
        int[] newStrides = new int[shape.length];

        for (int i = 0; i < axes.length; i++) {

            checkAxis(axes[i]);

            if (seen[axes[i]]) {
                throw new IllegalArgumentException("Duplicate axis " + axes[i] + ".");
            }

            seen[axes[i]] = true;
            newShape[i] = shape[axes[i]];
            newStrides[i] = strides[axes[i]];
        }

        return new JArray(storage, newShape, newStrides, offset);
    }

    public JArray slice(int start, int end) {
        return slice(0, start, end, 1);
    }

    public JArray slice(int axis, int start, int end) {
        return slice(axis, start, end, 1);
    }

    public JArray slice(int axis, int start, int end, int step) {

        requireFull();
        checkAxis(axis);

        int[] shape = dims();

        if (step < 1) {
            throw new IllegalArgumentException("Step must be positive.");
        }

        if (start < 0 || end > shape[axis] || start > end) {
            throw new IndexOutOfBoundsException(
                "Slice [" + start + ", " + end + ") out of bounds for axis " + axis
                + " with size " + shape[axis]
            );
        }

        int[] newShape = Arrays.copyOf(shape, shape.length);
        int[] newStrides = Arrays.copyOf(strides, strides.length);

        newShape[axis] = (end - start + step - 1) / step;
        newStrides[axis] = strides[axis] * step;

        return new JArray(storage, newShape, newStrides, offset + start * strides[axis]);
    }

    public JArray select(int axis, int index) {

        requireFull();
        checkAxis(axis);

        int size = dims()[axis];

        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for axis " + axis
                + " with size " + size
            );
        }

        return removeAxis(axis, index);
    }

    public JArray squeeze() {

        requireFull();

        int[] shape = dims();
        int kept = 0;

        for (int dimension : shape) {
            if (dimension != 1) {
                kept++;
            }
        }

        if (kept == 0) {
            return new JArray(storage, new int[]{1}, new int[]{1}, offset);
        }

        int[] newShape = new int[kept];
        int[] newStrides = new int[kept];

        for (int i = 0, j = 0; i < shape.length; i++) {
            if (shape[i] != 1) {
                newShape[j] = shape[i];
                newStrides[j] = strides[i];
                j++;
            }
        }

        return new JArray(storage, newShape, newStrides, offset);
    }

    public JArray squeeze(int axis) {

        requireFull();
        checkAxis(axis);

        int size = dims()[axis];

        if (size != 1) {
            throw new IllegalArgumentException(
                "Cannot squeeze axis " + axis + " with size " + size + "."
            );
        }

        return removeAxis(axis, 0);
    }

    public JArray expandDims(int axis) {

        requireFull();

        int[] shape = dims();

        if (axis < 0 || axis > shape.length) {
            throw new IllegalArgumentException(
                "Axis " + axis + " is out of range for " + (shape.length + 1)
                + " dimensions."
            );
        }

        int[] newShape = new int[shape.length + 1];
        int[] newStrides = new int[shape.length + 1];

        for (int i = 0, j = 0; i < newShape.length; i++) {

            if (i == axis) {
                newShape[i] = 1;
                newStrides[i] = axis < shape.length ? shape[axis] * strides[axis] : 1;
            } else {
                newShape[i] = shape[j];
                newStrides[i] = strides[j];
                j++;
            }
        }

        return new JArray(storage, newShape, newStrides, offset);
    }

    public int getSize() {
        return view ? viewSize : storage.size();
    }

    public int getDimensionCount() {
        return dimensions.length;
    }

    public int[] getShape() {
        int[] shape = dims();
        return Arrays.copyOf(shape, shape.length);
    }

    public void setShape(int... newShape) {

        requireOwner();

        if (newShape == null || newShape.length == 0) {
            throw new IllegalArgumentException("Shape cannot be empty.");
        }

        validateShape(newShape);

        int capacity = computeSize(newShape);

        if (capacity < storage.size()) {
            throw new IllegalArgumentException(
                "New shape cannot contain fewer elements than current size."
            );
        }

        if (newShape.length == 1 && newShape[0] != storage.size()) {
            throw new IllegalArgumentException(
                "One-dimensional arrays always match their size."
            );
        }

        this.dimensions = Arrays.copyOf(newShape, newShape.length);
        this.strides = computeStrides(this.dimensions);
        this.tracksSize = newShape.length == 1;
    }

    public int[] getStrides() {
        return Arrays.copyOf(strides, strides.length);
    }
}