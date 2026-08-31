# Nitrium Native Core

C++20 library providing SIMD frustum culling, native arena allocation, and (future) Vulkan/RT backends.

## Requirements

- CMake 3.20+
- C++20 compiler (MSVC 2022, GCC 12+, or Clang 15+)
- JDK 21+ (`JAVA_HOME` set for JNI headers)

## Build (Windows)

```powershell
.\gradlew.bat buildNative
.\gradlew.bat copyNative
.\gradlew.bat build
```

## Build (manual)

```bash
cd native
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release
```

Output: `native/build/Release/nitrium_native.dll` (Windows) or `native/build/libnitrium_native.so` (Linux).

## Architecture

| Module | Purpose |
|--------|---------|
| `simd_frustum_cull.cpp` | AVX2 SoA AABB vs frustum (8-wide batches) |
| `arena.cpp` | 256 MB bump allocator outside JVM heap |
| `nitrium_jni.cpp` | JNI bindings to Java `dev.nitrium.nativecore` |

## Java integration

- `NitriumNativeLoader` — loads DLL/SO from system path or mod JAR
- `SimdFrustumCuller` — zero-copy `GetPrimitiveArrayCritical` JNI path
- `NativeMemoryArena` — Java `MemorySegment` + native bump pool
- `NitriumAzdoBackend` — OpenGL 4.4 persistent buffers + MDI (Iris-safe)

If native library is missing, all features degrade to Java fallbacks automatically.

## Roadmap

- **Phase 2:** Full AVX-512 16-wide cull, Zstd decompress in native
- **Phase 3:** Vulkan 1.3 command buffer recording (optional, breaks Iris)
- **Phase 4:** BLAS/TLAS hardware RT for Photon/SEUS shadow rays
