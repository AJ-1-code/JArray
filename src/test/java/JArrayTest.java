package jarray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import jarray.DataType;
import jarray.JArray;

public class JArrayTest {

    private interface TestCase {
        void run() throws Exception;
    }

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        run("mixedTypes", JArrayTest::mixedTypes);
        run("uniformToMixedConversion", JArrayTest::uniformToMixedConversion);
        run("typeChangingSet", JArrayTest::typeChangingSet);
        run("insertAndRemove", JArrayTest::insertAndRemove);
        run("bulkOperations", JArrayTest::bulkOperations);
        run("searching", JArrayTest::searching);
        run("typedAccessors", JArrayTest::typedAccessors);
        run("ndIndexing", JArrayTest::ndIndexing);
        run("partialFill", JArrayTest::partialFill);
        run("transposeView", JArrayTest::transposeView);
        run("sliceAndSelect", JArrayTest::sliceAndSelect);
        run("squeezeAndExpand", JArrayTest::squeezeAndExpand);
        run("reshape", JArrayTest::reshape);
        run("concat", JArrayTest::concat);
        run("reverseSwapFill", JArrayTest::reverseSwapFill);
        run("javaIntegration", JArrayTest::javaIntegration);
        run("viewSafety", JArrayTest::viewSafety);
        run("clear", JArrayTest::clear);
        run("randomizedAgainstArrayList", JArrayTest::randomizedAgainstArrayList);

        System.out.println(passed + " passed, " + failed + " failed");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void run(String name, TestCase test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Throwable error) {
            failed++;
            System.out.println("FAIL " + name + ": " + error);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertThrows(Class<? extends Throwable> type, TestCase test) {
        try {
            test.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + error);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    private static void mixedTypes() {
        JArray array = new JArray();
        array.add(10);
        array.add("Hello");
        array.add(3.14);
        array.add(true);
        array.add(20);

        assertEquals(5, array.getSize());
        assertEquals("Hello", array.get(1));
        assertEquals(DataType.INT, array.getType(0));
        assertEquals(DataType.OBJECT, array.getType(1));
        assertEquals(DataType.DOUBLE, array.getType(2));
        assertEquals(DataType.BOOLEAN, array.getType(3));
        assertEquals(20, array.get(4));
    }

    private static void uniformToMixedConversion() {
        JArray array = new JArray();
        for (int i = 0; i < 100; i++) {
            array.add(i);
        }
        assertEquals(DataType.INT, array.getFlatType(50));
        array.add("late");
        array.add(2.5);
        assertEquals(102, array.getSize());
        assertEquals(77, array.get(77));
        assertEquals("late", array.get(100));
        assertEquals(2.5, array.get(101));
        assertEquals(DataType.INT, array.getType(99));

        JArray reused = new JArray();
        reused.add(1);
        reused.removeAt(0);
        reused.add("text");
        assertEquals("text", reused.get(0));
        assertEquals(DataType.OBJECT, reused.getType(0));
    }

    private static void typeChangingSet() {
        JArray array = JArray.of(1, 2, 3);
        array.set("two", 1);
        assertEquals("two", array.get(1));
        assertEquals(DataType.OBJECT, array.getType(1));
        assertEquals(1, array.get(0));
        assertEquals(3, array.get(2));

        for (int i = 0; i < 1000; i++) {
            array.set(i % 2 == 0 ? (Object) Integer.valueOf(i) : (Object) Double.valueOf(i), 0);
        }

        assertEquals(999.0, array.get(0));
        assertEquals("two", array.get(1));
        assertEquals(3, array.get(2));
    }

    private static void insertAndRemove() {
        JArray array = JArray.of(1, 2, 3);
        array.insert(1, "x");
        assertEquals("[1, x, 2, 3]", array.toString());
        assertTrue(array.remove("x"), "remove existing");
        assertTrue(!array.remove("missing"), "remove missing");
        array.removeAt(0);
        assertEquals("[2, 3]", array.toString());

        JArray uniform = JArray.of(5, 6, 7);
        uniform.insert(0, 4);
        uniform.insert(4, 8);
        assertEquals("[4, 5, 6, 7, 8]", uniform.toString());
        assertEquals(DataType.INT, uniform.getType(2));
        uniform.removeAt(2);
        assertEquals("[4, 5, 7, 8]", uniform.toString());

        JArray strings = JArray.of("a", "b", "c");
        strings.removeAt(2);
        strings.insert(1, "z");
        assertEquals("[a, z, b]", strings.toString());
    }

    private static void bulkOperations() {
        JArray array = JArray.of(1, 2, 3);
        array.addAll(2, 4, 2);
        assertEquals("[1, 2, 3, 2, 4, 2]", array.toString());
        assertEquals(3, array.removeAll(2));
        assertEquals("[1, 3, 4]", array.toString());

        JArray inserted = JArray.of(1, 4);
        inserted.insertAll(1, 2, 3);
        assertEquals("[1, 2, 3, 4]", inserted.toString());
        inserted.insertAll(2, "a", "b");
        assertEquals("[1, 2, a, b, 3, 4]", inserted.toString());

        JArray self = JArray.of(1, 2);
        self.addAll(self);
        assertEquals("[1, 2, 1, 2]", self.toString());

        JArray objects = JArray.of("x", "y", "x");
        assertEquals(2, objects.removeAll("x"));
        assertEquals("[y]", objects.toString());
    }

    private static void searching() {
        JArray array = JArray.of(1, 2, 1, 3, 1);
        assertEquals(0, array.indexOf(1));
        assertEquals(4, array.lastIndexOf(1));
        assertEquals(3, array.count(1));
        assertTrue(array.contains(3), "contains");
        assertTrue(!array.contains(9), "not contains");
        assertTrue(!array.contains(1L), "Long is not Integer");
        assertTrue(!array.contains(null), "no nulls");
        assertTrue(new JArray().isEmpty(), "empty");

        JArray objects = JArray.of("a", null, "b");
        assertEquals(1, objects.indexOf(null));
        assertEquals(2, objects.indexOf("b"));

        JArray doubles = JArray.of(1.5, Double.NaN, 2.5);
        assertEquals(1, doubles.indexOf(Double.NaN));
    }

    private static void typedAccessors() {
        JArray array = new JArray();
        array.add(10);
        array.add(20);
        assertEquals(10, array.getInt(0));
        array.setInt(99, 1);
        assertEquals(99, array.getInt(1));
        assertThrows(IllegalStateException.class, () -> array.getDouble(0));

        array.setDouble(1.5, 0);
        assertEquals(1.5, array.getDouble(0));
        assertEquals(DataType.DOUBLE, array.getType(0));
        assertEquals(99, array.getInt(1));

        JArray longs = new JArray();
        longs.add(5L);
        longs.add(6L);
        assertEquals(11L, longs.getLong(0) + longs.getLong(1));
        JArray flags = JArray.of(true, false);
        assertTrue(flags.getBoolean(0) && !flags.getBoolean(1), "booleans");
        JArray chars = JArray.of('a', 'b');
        assertEquals('b', chars.getChar(1));

        JArray matrix = JArray.of(1, 2, 3, 4).reshape(2, 2);
        assertEquals(4, matrix.getInt(1, 1));
        matrix.setInt(40, 1, 1);
        assertEquals(40, matrix.getInt(1, 1));
        assertThrows(IllegalArgumentException.class, () -> matrix.getInt(1));
    }

    private static void ndIndexing() {
        JArray matrix = new JArray(2, 3);
        for (int i = 1; i <= 6; i++) {
            matrix.add(i);
        }

        assertEquals(6, matrix.get(1, 2));
        assertEquals(5, matrix.getFlat(4));
        assertEquals("[[1, 2, 3], [4, 5, 6]]", matrix.toString());
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(2, 0));
        assertThrows(IllegalStateException.class, () -> matrix.add(7));
        assertThrows(IllegalArgumentException.class, () -> matrix.get(1));

        JArray cube = new JArray(2, 2, 2);
        for (int i = 0; i < 8; i++) {
            cube.add(i);
        }
        assertEquals(5, cube.get(1, 0, 1));
        assertEquals(7, cube.get(1, 1, 1));
    }

    private static void partialFill() {
        JArray matrix = new JArray(2, 2);
        matrix.add(1);
        assertEquals(1, matrix.get(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.get(0, 1));
        assertThrows(IllegalStateException.class, matrix::transpose);
    }

    private static void transposeView() {
        JArray matrix = JArray.of(1, 2, 3, 4, 5, 6).reshape(2, 3);
        JArray transposed = matrix.transpose();

        assertEquals("[3, 2]", Arrays.toString(transposed.getShape()));
        assertEquals(6, transposed.get(2, 1));

        transposed.set(100, 0, 1);
        assertEquals(100, matrix.get(1, 0));
        assertEquals("[1, 100, 2, 5, 3, 6]", transposed.flatten().toString());
        assertEquals("[[1, 100], [2, 5], [3, 6]]", transposed.toString());
        assertEquals(100, transposed.getInt(0, 1));
    }

    private static void sliceAndSelect() {
        JArray matrix = JArray.of(1, 2, 3, 4, 5, 6).reshape(2, 3);

        assertEquals("[[2, 3], [5, 6]]", matrix.slice(1, 1, 3).toString());
        assertEquals("[4, 5, 6]", matrix.select(0, 1).toString());
        assertEquals("[[4, 5, 6]]", matrix.slice(1, 2).toString());
        assertEquals("[0, 2, 4]", JArray.of(0, 1, 2, 3, 4, 5).slice(0, 0, 6, 2).toString());

        JArray column = matrix.select(1, 1);
        column.set(50, 1);
        assertEquals(50, matrix.get(1, 1));
        assertEquals(3, JArray.of(1, 2, 3, 4).slice(1, 4).getSize());
        assertEquals(2, JArray.of(1, 2, 3, 4).slice(1, 4).indexOf(4));
    }

    private static void squeezeAndExpand() {
        JArray matrix = JArray.of(1, 2, 3, 4, 5, 6).reshape(2, 3);
        JArray expanded = matrix.expandDims(0);

        assertEquals("[1, 2, 3]", Arrays.toString(expanded.getShape()));
        assertEquals("[2, 3]", Arrays.toString(expanded.squeeze().getShape()));
        assertEquals("[2, 3]", Arrays.toString(expanded.squeeze(0).getShape()));
        assertThrows(IllegalArgumentException.class, () -> expanded.squeeze(1));
        assertEquals(6, expanded.get(0, 1, 2));
        assertEquals("[2, 3, 1]", Arrays.toString(matrix.expandDims(2).getShape()));
    }

    private static void reshape() {
        JArray flat = JArray.of(1, 2, 3, 4, 5, 6);

        assertEquals("[3, 2]", Arrays.toString(flat.reshape(3, -1).getShape()));
        assertThrows(IllegalArgumentException.class, () -> flat.reshape(4, 2));

        JArray matrix = flat.reshape(2, 3);
        JArray copied = matrix.transpose().reshape(6);
        assertEquals("[1, 4, 2, 5, 3, 6]", copied.toString());
        copied.set(0, 0);
        assertEquals(1, matrix.get(0, 0));

        JArray shared = matrix.reshape(3, 2);
        shared.set(77, 0, 0);
        assertEquals(77, matrix.get(0, 0));
    }

    private static void concat() {
        JArray a = JArray.of(1, 2, 3, 4).reshape(2, 2);
        JArray b = JArray.of(5, 6).reshape(1, 2);
        JArray c = JArray.of(5, 6).reshape(2, 1);

        assertEquals("[[1, 2], [3, 4], [5, 6]]", a.concat(b).toString());
        assertEquals("[[1, 2, 5], [3, 4, 6]]", a.concat(c, 1).toString());
        assertEquals("[1, 2, 3]", JArray.of(1, 2).concat(JArray.of(3)).toString());
        assertEquals("[1, a]", JArray.of(1).concat(JArray.of("a")).toString());
        assertThrows(IllegalArgumentException.class, () -> a.concat(b, 1));
    }

    private static void reverseSwapFill() {
        JArray array = JArray.of(1, 2, 3, 4);

        array.reverse();
        assertEquals("[4, 3, 2, 1]", array.toString());
        array.swapFlat(0, 3);
        assertEquals("[1, 3, 2, 4]", array.toString());
        array.swap(new int[]{0}, new int[]{1});
        assertEquals("[3, 1, 2, 4]", array.toString());

        array.fill(7);
        assertEquals("[7, 7, 7, 7]", array.toString());
        assertEquals(DataType.INT, array.getType(2));
        array.fill("a");
        assertEquals("[a, a, a, a]", array.toString());
        assertEquals(DataType.OBJECT, array.getType(0));

        JArray strings = JArray.of("a", "b", "c");
        strings.reverse();
        assertEquals("[c, b, a]", strings.toString());

        JArray mixed = JArray.of(1, "b", 2.0);
        mixed.reverse();
        assertEquals("[2.0, b, 1]", mixed.toString());
    }

    private static void javaIntegration() {
        JArray array = JArray.of(1, 2, 3);

        int sum = 0;
        for (Object value : array) {
            sum += (Integer) value;
        }
        assertEquals(6, sum);
        assertEquals(3, array.toArray().length);
        assertEquals(Arrays.asList(1, 2, 3), array.toList());

        assertTrue(JArray.of(1, 2).equals(JArray.of(1, 2)), "equal");
        assertEquals(JArray.of(1, 2).hashCode(), JArray.of(1, 2).hashCode());
        assertTrue(!JArray.of(1, 2).equals(JArray.of(1L, 2L)), "int vs long");

        JArray copy = array.copy();
        copy.set(9, 0);
        assertEquals(1, array.get(0));
        assertEquals(9, copy.get(0));

        JArray mixedCopy = JArray.of(1, "a", 2.0).copy();
        assertEquals("[1, a, 2.0]", mixedCopy.toString());

        JArray filled = JArray.filled(0, 2, 2);
        assertEquals("[[0, 0], [0, 0]]", filled.toString());
        assertTrue(!array.getClass().isArray(), "sanity");
    }

    private static void viewSafety() {
        JArray base = JArray.of(1, 2, 3);
        JArray view = base.slice(0, 0, 2);

        assertEquals(2, view.getSize());
        assertTrue(view.isView(), "isView");
        view.set(9, 0);
        assertEquals(9, base.get(0));
        assertThrows(UnsupportedOperationException.class, () -> view.add(1));
        assertThrows(UnsupportedOperationException.class, () -> view.removeAt(0));

        base.add(4);
        assertThrows(ConcurrentModificationException.class, () -> view.get(0));
    }

    private static void clear() {
        JArray array = JArray.of(1, 2, 3);
        array.clear();
        assertEquals(0, array.getSize());
        assertEquals("[0]", Arrays.toString(array.getShape()));
        array.add("x");
        assertEquals("[x]", array.toString());

        JArray matrix = new JArray(2, 2);
        matrix.add(1);
        matrix.clear();
        matrix.add(2);
        assertEquals(2, matrix.get(0, 0));
    }

    private static void randomizedAgainstArrayList() {
        Random random = new Random(12345);
        JArray array = new JArray();
        List<Object> reference = new ArrayList<>();

        for (int step = 0; step < 20000; step++) {
            int action = random.nextInt(8);
            Object value = randomValue(random);

            if (action <= 2 || reference.isEmpty()) {
                array.add(value);
                reference.add(value);
            } else if (action == 3) {
                int index = random.nextInt(reference.size() + 1);
                array.insert(index, value);
                reference.add(index, value);
            } else if (action == 4) {
                int index = random.nextInt(reference.size());
                array.removeAt(index);
                reference.remove(index);
            } else if (action == 5) {
                int index = random.nextInt(reference.size());
                array.set(value, index);
                reference.set(index, value);
            } else if (action == 6) {
                assertEquals(reference.indexOf(value), array.indexOf(value));
                assertEquals(reference.lastIndexOf(value), array.lastIndexOf(value));
            } else if (random.nextInt(50) == 0) {
                int removed = array.removeAll(value);
                int expected = 0;
                for (int i = reference.size() - 1; i >= 0; i--) {
                    if (Objects.equals(reference.get(i), value)) {
                        reference.remove(i);
                        expected++;
                    }
                }
                assertEquals(expected, removed);
            }

            assertEquals(reference.size(), array.getSize());
        }

        assertEquals(reference, array.toList());
    }

    private static Object randomValue(Random random) {
        switch (random.nextInt(4)) {
            case 0:
                return random.nextInt(5);
            case 1:
                return random.nextInt(5) * 1.5;
            case 2:
                return "s" + random.nextInt(3);
            default:
                return random.nextBoolean();
        }
    }
}