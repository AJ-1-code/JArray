
package jarray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;
import jarray.JArray;

public class JArrayBenchmark {

    private static final int[] SIZES = {
        100,
        1_000,
        10_000,
        100_000,
        200_000,
        1_000_000
    };

    private static final int WARMUP_ROUNDS = 5;
    private static final int TEST_ROUNDS = 9;

    private static final int MAX_RANDOM_ACCESS_COUNT = 100_000;
    private static final int MIN_RANDOM_ACCESS_COUNT = 1_000;

    private static final int SEARCH_REPETITIONS = 10;
    private static final int INSERT_REPETITIONS = 10;
    private static final int COPY_REPETITIONS = 10;

    private static volatile Object blackhole;

    private record Sample(long nanos, long checksum) {}

    private record Result(
        int size,
        String name,
        long jNanos,
        long aNanos,
        long operations,
        long jMemory,
        long aMemory
    ) {}

    @FunctionalInterface
    private interface Task {
        long run();
    }

    @FunctionalInterface
    private interface TaskFactory {
        Task create();
    }

    private static final ArrayList<Result> RESULTS = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {


        System.out.println("Warmup rounds: " + WARMUP_ROUNDS);
        System.out.println("Test rounds:   " + TEST_ROUNDS);
        System.out.println("JVM:           " + System.getProperty("java.version"));
        System.out.println("VM:            " + System.getProperty("java.vm.name"));

        for (int size : SIZES) {

            System.out.printf(
                "%nRunning tests for %,d elements...%n",
                size
            );

            benchmarkAppendPreallocated(size);
            benchmarkAppendGrowth(size);
            benchmarkRandomAccess(size);
            benchmarkIteration(size);
            benchmarkSet(size);
            benchmarkSearch(size);
            benchmarkInsertRemove(size);
            benchmarkCopy(size);
            benchmarkMixedTypes(size);

            benchmarkMemory(size);
        }

        printTimingChart();
        printMemoryChart();
    }

    private static void benchmark(
        String name,
        int size,
        long operations,
        TaskFactory jFactory,
        TaskFactory aFactory
    ) {

        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            blackhole = jFactory.create().run();
            blackhole = aFactory.create().run();
        }

        long[] jTimes = new long[TEST_ROUNDS];
        long[] aTimes = new long[TEST_ROUNDS];

        long checksum = 0;

        for (int i = 0; i < TEST_ROUNDS; i++) {

            if (i % 2 == 0) {

                Task jTask = jFactory.create();

                long start = System.nanoTime();
                long result = jTask.run();
                jTimes[i] = System.nanoTime() - start;

                checksum += result;
                blackhole = result;

                Task aTask = aFactory.create();

                start = System.nanoTime();
                result = aTask.run();
                aTimes[i] = System.nanoTime() - start;

                checksum += result;
                blackhole = result;

            } else {

                Task aTask = aFactory.create();

                long start = System.nanoTime();
                long result = aTask.run();
                aTimes[i] = System.nanoTime() - start;

                checksum += result;
                blackhole = result;

                Task jTask = jFactory.create();

                start = System.nanoTime();
                result = jTask.run();
                jTimes[i] = System.nanoTime() - start;

                checksum += result;
                blackhole = result;
            }
        }

        long jMedian = median(jTimes);
        long aMedian = median(aTimes);

        RESULTS.add(new Result(
            size,
            name,
            jMedian,
            aMedian,
            operations,
            0,
            0
        ));

        blackhole = checksum;
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static JArray createJArray(int size) {

        JArray array = new JArray(size);

        for (int i = 0; i < size; i++) {
            array.add(i + 1000);
        }

        return array;
    }

    private static ArrayList<Integer> createArrayList(int size) {

        ArrayList<Integer> array = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            array.add(i + 1000);
        }

        return array;
    }

    private static int[] createRandomIndices(int size) {

        Random random = new Random(12345);

        int count = Math.min(
            MAX_RANDOM_ACCESS_COUNT,
            Math.max(MIN_RANDOM_ACCESS_COUNT, size)
        );

        int[] indices = new int[count];

        for (int i = 0; i < count; i++) {
            indices[i] = random.nextInt(size);
        }

        return indices;
    }

    private static void benchmarkAppendPreallocated(int size) {

        benchmark(
            "Append (preallocated)",
            size,
            size,

            () -> {
                JArray array = new JArray(size);

                return () -> {
                    for (int i = 0; i < size; i++) {
                        array.add(i);
                    }

                    return array.getSize();
                };
            },

            () -> {
                ArrayList<Integer> array = new ArrayList<>(size);

                return () -> {
                    for (int i = 0; i < size; i++) {
                        array.add(i);
                    }

                    return array.size();
                };
            }
        );
    }

    private static void benchmarkAppendGrowth(int size) {

        benchmark(
            "Append (automatic growth)",
            size,
            size,

            () -> {
                JArray array = new JArray();

                return () -> {
                    for (int i = 0; i < size; i++) {
                        array.add(i);
                    }

                    return array.getSize();
                };
            },

            () -> {
                ArrayList<Integer> array = new ArrayList<>();

                return () -> {
                    for (int i = 0; i < size; i++) {
                        array.add(i);
                    }

                    return array.size();
                };
            }
        );
    }

    private static void benchmarkRandomAccess(int size) {

        int[] indices = createRandomIndices(size);

        benchmark(
            "Random access",
            size,
            indices.length,

            () -> {
                JArray array = createJArray(size);

                return () -> {
                    long checksum = 0;

                    for (int index : indices) {
                        int value = (int) array.getFlat(index);
                        checksum += value;
                    }

                    return checksum;
                };
            },

            () -> {
                ArrayList<Integer> array = createArrayList(size);

                return () -> {
                    long checksum = 0;

                    for (int index : indices) {
                        checksum += array.get(index);
                    }

                    return checksum;
                };
            }
        );
    }

    private static void benchmarkIteration(int size) {

        benchmark(
            "Sequential iteration",
            size,
            size,

            () -> {
                JArray array = createJArray(size);

                return () -> {
                    long checksum = 0;

                    for (Object value : array) {
                        checksum += (Integer) value;
                    }

                    return checksum;
                };
            },

            () -> {
                ArrayList<Integer> array = createArrayList(size);

                return () -> {
                    long checksum = 0;

                    for (Integer value : array) {
                        checksum += value;
                    }

                    return checksum;
                };
            }
        );
    }

    private static void benchmarkSet(int size) {

        benchmark(
            "Set existing elements",
            size,
            size,

            () -> {
                JArray array = createJArray(size);

                return () -> {
                    for (int i = 0; i < size; i++) {
                        array.setFlat(i * 2, i);
                    }

                    int value = (int) array.getFlat(size / 2);
                    return value;
                };
            },

            () -> {
                ArrayList<Integer> array = createArrayList(size);

                return () -> {
                    for (int i = 0; i < size; i++) {
                        array.set(i, i * 2);
                    }

                    return array.get(size / 2);
                };
            }
        );
    }

    private static void benchmarkSearch(int size) {

        int searchValue = -1;

        benchmark(
            "Search (unsuccessful indexOf)",
            size,
            (long) size * SEARCH_REPETITIONS,

            () -> {
                JArray array = createJArray(size);

                return () -> {
                    long result = 0;

                    for (int i = 0; i < SEARCH_REPETITIONS; i++) {
                        result += array.indexOf(searchValue);
                    }

                    return result;
                };
            },

            () -> {
                ArrayList<Integer> array = createArrayList(size);

                return () -> {
                    long result = 0;

                    for (int i = 0; i < SEARCH_REPETITIONS; i++) {
                        result += array.indexOf(searchValue);
                    }

                    return result;
                };
            }
        );
    }

    private static void benchmarkInsertRemove(int size) {

        benchmark(
            "Middle insertion + removal",
            size,
            INSERT_REPETITIONS * 2L,

            () -> {
                JArray array = createJArray(size);
                int middle = size / 2;

                return () -> {
                    for (int i = 0; i < INSERT_REPETITIONS; i++) {
                        array.insert(middle, -1);
                        array.removeAt(middle);
                    }

                    return array.getSize();
                };
            },

            () -> {
                ArrayList<Integer> array = createArrayList(size);
                int middle = size / 2;

                return () -> {
                    for (int i = 0; i < INSERT_REPETITIONS; i++) {
                        array.add(middle, -1);
                        array.remove(middle);
                    }

                    return array.size();
                };
            }
        );
    }

    private static void benchmarkCopy(int size) {

        benchmark(
            "Copy",
            size,
            (long) size * COPY_REPETITIONS,

            () -> {
                JArray array = createJArray(size);

                return () -> {
                    long checksum = 0;

                    for (int i = 0; i < COPY_REPETITIONS; i++) {
                        JArray copy = array.copy();
                        checksum += copy.getSize();
                        blackhole = copy;
                    }

                    return checksum;
                };
            },

            () -> {
                ArrayList<Integer> array = createArrayList(size);

                return () -> {
                    long checksum = 0;

                    for (int i = 0; i < COPY_REPETITIONS; i++) {
                        ArrayList<Integer> copy = new ArrayList<>(array);
                        checksum += copy.size();
                        blackhole = copy;
                    }

                    return checksum;
                };
            }
        );
    }

    private static void benchmarkMixedTypes(int size) {

        benchmark(
            "Mixed-type insertion",
            size,
            size,

            () -> {
                JArray array = new JArray(size);

                return () -> {
                    for (int i = 0; i < size; i++) {
                        switch (i % 8) {
                            case 0 -> array.add(i);
                            case 1 -> array.add((double) i);
                            case 2 -> array.add((float) i);
                            case 3 -> array.add((long) i);
                            case 4 -> array.add((byte) i);
                            case 5 -> array.add((short) i);
                            case 6 -> array.add((char) ('A' + i % 26));
                            case 7 -> array.add("Value " + i);
                        }
                    }

                    return array.getSize();
                };
            },

            () -> {
                ArrayList<Object> array = new ArrayList<>(size);

                return () -> {
                    for (int i = 0; i < size; i++) {
                        switch (i % 8) {
                            case 0 -> array.add(i);
                            case 1 -> array.add((double) i);
                            case 2 -> array.add((float) i);
                            case 3 -> array.add((long) i);
                            case 4 -> array.add((byte) i);
                            case 5 -> array.add((short) i);
                            case 6 -> array.add((char) ('A' + i % 26));
                            case 7 -> array.add("Value " + i);
                        }
                    }

                    return array.size();
                };
            }
        );
    }

    private static void benchmarkMemory(int size)
        throws InterruptedException {

        long jMemory = measureMemory(() -> {
            JArray array = new JArray();

            for (int i = 0; i < size; i++) {
                array.add(i + 1000);
            }

            return array;
        });

        long aMemory = measureMemory(() -> {
            ArrayList<Integer> array = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                array.add(i + 1000);
            }

            return array;
        });

        RESULTS.add(new Result(
            size,
            "Retained heap (approx.)",
            0,
            0,
            0,
            jMemory,
            aMemory
        ));
    }

    private static long measureMemory(Supplier<?> allocation)
        throws InterruptedException {

        long[] measurements = new long[3];

        for (int i = 0; i < measurements.length; i++) {

            blackhole = null;

            System.gc();
            Thread.sleep(150);

            Runtime runtime = Runtime.getRuntime();

            long before =
                runtime.totalMemory() - runtime.freeMemory();

            Object result = allocation.get();
            blackhole = result;

            System.gc();
            Thread.sleep(150);

            long after =
                runtime.totalMemory() - runtime.freeMemory();

            measurements[i] = Math.max(0, after - before);

            blackhole = null;
        }

        System.gc();
        Thread.sleep(150);

        return median(measurements);
    }

    private static void printTimingChart() {


        System.out.printf(
            "%-10s | %-32s | %12s | %12s | %12s | %12s | %s%n",
            "Elements",
            "Test",
            "JArray ms",
            "ArrayList ms",
            "JArray ns/op",
            "ArrayList ns/op",
            "Winner"
        );

        System.out.println("");

        for (Result r : RESULTS) {

            if (r.operations() == 0) {
                continue;
            }

            double jMs = r.jNanos() / 1_000_000.0;
            double aMs = r.aNanos() / 1_000_000.0;

            double jNs = (double) r.jNanos() / r.operations();
            double aNs = (double) r.aNanos() / r.operations();

            String winner;

            if (r.jNanos() < r.aNanos()) {
                winner = String.format(
                    "JArray %.2fx",
                    (double) r.aNanos() / r.jNanos()
                );
            } else {
                winner = String.format(
                    "ArrayList %.2fx",
                    (double) r.jNanos() / r.aNanos()
                );
            }

            System.out.printf(
                "%-10s | %-32s | %12.3f | %12.3f | %12.2f | %12.2f | %s%n",
                String.format("%,d", r.size()),
                r.name(),
                jMs,
                aMs,
                jNs,
                aNs,
                winner
            );
        }

        System.out.println("");
    }

    private static void printMemoryChart() {

        System.out.printf(
            "%-12s | %14s | %14s | %14s | %s%n",
            "Elements",
            "JArray KB",
            "ArrayList KB",
            "JArray B/elem",
            "Winner"
        );

        System.out.println("");

        for (Result r : RESULTS) {

            if (r.operations() != 0) {
                continue;
            }

            String winner;

            if (r.jMemory() < r.aMemory()) {
                winner = String.format(
                    "JArray %.2fx less",
                    (double) r.aMemory() / Math.max(1, r.jMemory())
                );
            } else {
                winner = String.format(
                    "ArrayList %.2fx less",
                    (double) r.jMemory() / Math.max(1, r.aMemory())
                );
            }

            System.out.printf(
                "%-12s | %14.2f | %14.2f | %14.2f | %s%n",
                String.format("%,d", r.size()),
                r.jMemory() / 1024.0,
                r.aMemory() / 1024.0,
                (double) r.jMemory() / r.size(),
                winner
            );
        }

        System.out.println("");

        System.out.println(
            "NOTE: Memory figures are approximate heap deltas, not exact object sizes."
        );
    }
}
