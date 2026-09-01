#pragma once

#include <cstddef>
#include <cstdint>

namespace nitrium {

const char* version();

// Runtime CPU feature detection (CPUID)
int cpu_vendor(); // 0=unknown, 1=Intel, 2=AMD
int cpu_logical_cores();
int cpu_physical_cores();
bool cpu_has_avx2();
bool cpu_has_avx512();

// Arena allocator — bump pool outside JVM heap
void* arena_alloc(std::size_t bytes);
void arena_reset();
void arena_shutdown();

// SIMD frustum cull: returns visibility bitmask (bit i = entity i visible)
// planes: 6 frustum planes × 4 floats (a,b,c,d) = 24 floats
std::uint32_t frustum_cull_soa(
    const float* min_x, const float* min_y, const float* min_z,
    const float* max_x, const float* max_y, const float* max_z,
    const float* planes,
    int count
);

// 3D noise: fill coarse grid samples (SIMD-batched when AVX2 available)
void noise3d_fill_coarse(
    float* out,
    int size_x, int size_y, int size_z,
    float origin_x, float origin_y, float origin_z,
    float step_x, float step_y, float step_z,
    std::uint32_t seed
);

// Trilinear upsample from coarse grid into per-block density buffer
void noise3d_trilinear_fill(
    float* out,
    int chunk_size_x, int chunk_size_y, int chunk_size_z,
    const float* coarse,
    int coarse_x, int coarse_y, int coarse_z,
    int step_x, int step_y, int step_z
);

// Mark voxels needing full-resolution resample (gradient > threshold)
void noise3d_mark_high_gradient(
    const std::uint8_t* out_mask,
    std::uint8_t* mask,
    int chunk_size_x, int chunk_size_y, int chunk_size_z,
    const float* density,
    float threshold
);

// Packet compression (LZ77-lite with raw fallback)
int packet_compress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap);
int packet_decompress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap);

// Ring-buffer async chunk I/O (queue side implemented; disk drain still TODO)
bool ring_buffer_init(std::size_t capacity_bytes);
void ring_buffer_shutdown();
std::size_t ring_buffer_pending();
std::size_t ring_buffer_free();
bool ring_buffer_write(const std::uint8_t* input, std::size_t length);
bool ring_buffer_read(std::uint8_t* output, std::size_t length);
bool chunk_io_submit_async(const std::uint8_t* payload, std::size_t length);

// Dequeue one pending write from the ring buffer. Returns length written to output, or 0 if empty.
int chunk_io_poll_write(std::uint8_t* output, int output_cap);

} // namespace nitrium
