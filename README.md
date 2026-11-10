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
