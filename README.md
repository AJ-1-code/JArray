# Dynamic Array Library

A high-performance, multi-dimensional, primitive-first array library for Java. It supports arbitrarily high dimensions and infinitely expanding single-dimensional arrays, all while storing primitive data types directly without object-boxing overhead.

---

## Key Features

* **Zero Boxing Overhead:** Stores Java primitive types in contiguous native arrays, eliminating heap fragmentation and GC pressure.
* **N-Dimensional & Infinite Expansion:** Handle 1D dynamically expanding arrays or arbitrarily high-dimensional tensor structures seamlessly.
* **Zero-Copy Views:** Perform operations like `slice`, `transpose`, `reshape`, and `squeeze` as views over existing data without expensive memory copies.
* **Rich Operations Suite:** Complete set of collection-style operations (`add`, `insert`, `remove`, `addAll`) alongside tensor-style manipulations (`computeStrides`, `flatten`, `concat`).

---

## Performance Notes

* **Current Benchmark (v1.0):** Performs within **~2x of standard `ArrayList`** operations while offering full multi-dimensional stride translation and unboxed primitive storage.
* **Optimization Roadmap:** Version 2.0 targets bounds-check reduction, specialized primitive iterators, and SIMD-friendly memory access patterns to close the performance gap completely.

---

## Installation & Setup

Add the library to your Java project build configuration:

### Maven
```xml
<dependency>
    <groupId>com.yourdomain</groupId>
    <artifactId>custom-array-library</artifactId>
    <version>1.0.0</version>
</dependency>
GradleGroovyimplementation 'com.yourdomain:custom-array-library:1.0.0'
Core API ReferenceBelow is a breakdown of the primary functions available in the library:Creation & InitializationMethodDescriptionof(...)Constructs an array instance with specified elements.filled(...)Instantiates an array filled with a single default value across a target shape.copy()Creates a deep copy of the array and underlying buffer.Shape & Dimension OperationsMethodDescriptiongetShape(), setShape(...)Accesses or mutates the dimensional layout.getDimensionCount()Returns the total number of axes/dimensions.reshape(...)Re-interprets array dimensions without copying underlying buffer data.squeeze(...), expandDims(...)Removes or adds unit dimensions.flatten()Converts multi-dimensional storage into a single continuous 1D representation.Views & ManipulationsMethodDescriptionview()Creates a non-owning structural view over the backing memory buffer.slice(...)Extracts sub-sections along specific axes as zero-copy views.transpose(...)Permutes array axes without physically reordering elements in memory.concat(...)Concatenates two arrays along a specified axis.Element Access & MutatorsMethodDescriptionget(...), getFlat(...)Retrieves elements using multi-dimensional coordinates or direct flat index.set(...), setFlat(...)Sets element values at multi-dimensional or flat positions.add(...), addAll(...)Appends single elements or bulk collections.insert(...), insertAll(...)Inserts elements at specified positions or along axes.removeAt(...), remove(...)Removes elements by index or matching value.clear()Resets size and purges array contents.Quick ExampleJava// Example: Creating and manipulating a multi-dimensional array
// 1. Create a 3x3 array
var arr = MyArray.filled(new int[]{3, 3}, 0);

// 2. Set values
arr.set(new int[]{0, 1}, 42);

// 3. Slice and transpose without copying
var view = arr.slice(0, 1).transpose(1, 0);

System.out.println("Shape: " + Arrays.toString(view.getShape()));
Contributing & DevelopmentFork the RepositoryCreate your Feature Branch (git checkout -b feature/Optimization)Commit your Changes (git commit -m 'Optimize stride computation')Push to the Branch (git push origin feature/Optimization)Open a Pull Request
<ElicitationsGroup message="Where would you like to take your project next?">
  <Elicitation label="Draft a benchmark suite using JMH to measure V1 vs ArrayList" query="Help me write a Java Microbenchmark Harness (JMH) suite to benchmark my custom array library against Java's standard ArrayList."/>
  <Elicitation label="Help optimize index resolution and stride calculations for V2" query="Let's optimize stride calculations and index resolution methods like storageIndex and resolve to improve performance in V2."/>
</ElicitationsGroup>
