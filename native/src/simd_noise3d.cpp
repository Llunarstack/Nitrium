#include "nitrium/nitrium.h"

#include <cmath>
#include <cstdint>
#include <cstring>

#if defined(__AVX2__)
#include <immintrin.h>
#define NITRIUM_HAS_AVX2 1
#else
#define NITRIUM_HAS_AVX2 0
#endif

namespace nitrium {

namespace {

inline float fade(float t) {
    return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
}

inline float lerp(float a, float b, float t) {
    return a + t * (b - a);
}

// Fast deterministic 3D value noise (Perlin-style gradient approximation).
inline float value_noise_3d(float x, float y, float z, std::uint32_t seed) {
    const int xi = static_cast<int>(std::floor(x)) & 255;
    const int yi = static_cast<int>(std::floor(y)) & 255;
    const int zi = static_cast<int>(std::floor(z)) & 255;

    const float xf = x - std::floor(x);
    const float yf = y - std::floor(y);
    const float zf = z - std::floor(z);

    const float u = fade(xf);
    const float v = fade(yf);
    const float w = fade(zf);

    auto hash = [&](int a, int b, int c) -> float {
        std::uint32_t h = seed;
        h ^= static_cast<std::uint32_t>(a) * 374761393u;
        h ^= static_cast<std::uint32_t>(b) * 668265263u;
        h ^= static_cast<std::uint32_t>(c) * 2246822519u;
        h = (h ^ (h >> 13)) * 1274126177u;
        return static_cast<float>(static_cast<int>(h) & 1023) / 512.0f - 1.0f;
    };

    const float x00 = lerp(hash(xi, yi, zi), hash(xi + 1, yi, zi), u);
    const float x10 = lerp(hash(xi, yi + 1, zi), hash(xi + 1, yi + 1, zi), u);
    const float x01 = lerp(hash(xi, yi, zi + 1), hash(xi + 1, yi, zi + 1), u);
    const float x11 = lerp(hash(xi, yi + 1, zi + 1), hash(xi + 1, yi + 1, zi + 1), u);

    const float y0 = lerp(x00, x10, v);
    const float y1 = lerp(x01, x11, v);
    return lerp(y0, y1, w);
}

inline float sample_coarse(const float* grid, int gx, int gy, int gz, int sx, int sy, int sz) {
    return grid[gx + gy * sx + gz * sx * sy];
}

} // namespace

void noise3d_fill_coarse(
    float* out,
    int size_x,
    int size_y,
    int size_z,
    float origin_x,
    float origin_y,
    float origin_z,
    float step_x,
    float step_y,
    float step_z,
    std::uint32_t seed
) {
    const int count = size_x * size_y * size_z;
    int index = 0;

#if NITRIUM_HAS_AVX2
    int batch = 0;
    alignas(32) float xs[8];
    alignas(32) float ys[8];
    alignas(32) float zs[8];
    alignas(32) float results[8];

    for (int gz = 0; gz < size_z; ++gz) {
        for (int gy = 0; gy < size_y; ++gy) {
            for (int gx = 0; gx < size_x; ++gx) {
                xs[batch] = origin_x + static_cast<float>(gx) * step_x;
                ys[batch] = origin_y + static_cast<float>(gy) * step_y;
                zs[batch] = origin_z + static_cast<float>(gz) * step_z;
                ++batch;

                if (batch == 8) {
                    for (int lane = 0; lane < 8; ++lane) {
                        results[lane] = value_noise_3d(xs[lane], ys[lane], zs[lane], seed);
                    }
                    std::memcpy(out + index, results, 8 * sizeof(float));
                    index += 8;
                    batch = 0;
                }
            }
        }
    }

    for (int lane = 0; lane < batch; ++lane) {
        out[index++] = value_noise_3d(xs[lane], ys[lane], zs[lane], seed);
    }
#else
    for (int gz = 0; gz < size_z; ++gz) {
        for (int gy = 0; gy < size_y; ++gy) {
            for (int gx = 0; gx < size_x; ++gx) {
                const float x = origin_x + static_cast<float>(gx) * step_x;
                const float y = origin_y + static_cast<float>(gy) * step_y;
                const float z = origin_z + static_cast<float>(gz) * step_z;
                out[index++] = value_noise_3d(x, y, z, seed);
            }
        }
    }
#endif

    (void)count;
}

void noise3d_trilinear_fill(
    float* out,
    int chunk_size_x,
    int chunk_size_y,
    int chunk_size_z,
    const float* coarse,
    int coarse_x,
    int coarse_y,
    int coarse_z,
    int step_x,
    int step_y,
    int step_z
) {
    for (int z = 0; z < chunk_size_z; ++z) {
        const float fz = static_cast<float>(z) / static_cast<float>(step_z);
        const int gz0 = z / step_z;
        const int gz1 = gz0 + 1 < coarse_z ? gz0 + 1 : gz0;
        const float tz = fz - static_cast<float>(gz0);

        for (int y = 0; y < chunk_size_y; ++y) {
            const float fy = static_cast<float>(y) / static_cast<float>(step_y);
            const int gy0 = y / step_y;
            const int gy1 = gy0 + 1 < coarse_y ? gy0 + 1 : gy0;
            const float ty = fy - static_cast<float>(gy0);

            for (int x = 0; x < chunk_size_x; ++x) {
                const float fx = static_cast<float>(x) / static_cast<float>(step_x);
                const int gx0 = x / step_x;
                const int gx1 = gx0 + 1 < coarse_x ? gx0 + 1 : gx0;
                const float tx = fx - static_cast<float>(gx0);

                const float c000 = sample_coarse(coarse, gx0, gy0, gz0, coarse_x, coarse_y, coarse_z);
                const float c100 = sample_coarse(coarse, gx1, gy0, gz0, coarse_x, coarse_y, coarse_z);
                const float c010 = sample_coarse(coarse, gx0, gy1, gz0, coarse_x, coarse_y, coarse_z);
                const float c110 = sample_coarse(coarse, gx1, gy1, gz0, coarse_x, coarse_y, coarse_z);
                const float c001 = sample_coarse(coarse, gx0, gy0, gz1, coarse_x, coarse_y, coarse_z);
                const float c101 = sample_coarse(coarse, gx1, gy0, gz1, coarse_x, coarse_y, coarse_z);
                const float c011 = sample_coarse(coarse, gx0, gy1, gz1, coarse_x, coarse_y, coarse_z);
                const float c111 = sample_coarse(coarse, gx1, gy1, gz1, coarse_x, coarse_y, coarse_z);

                const float x00 = lerp(c000, c100, tx);
                const float x10 = lerp(c010, c110, tx);
                const float x01 = lerp(c001, c101, tx);
                const float x11 = lerp(c011, c111, tx);
                const float y0 = lerp(x00, x10, ty);
                const float y1 = lerp(x01, x11, ty);
                out[x + y * chunk_size_x + z * chunk_size_x * chunk_size_y] = lerp(y0, y1, tz);
            }
        }
    }
}

void noise3d_mark_high_gradient(
    const std::uint8_t* out_mask,
    std::uint8_t* mask,
    int chunk_size_x,
    int chunk_size_y,
    int chunk_size_z,
    const float* density,
    float threshold
) {
    const int plane = chunk_size_x * chunk_size_y;
    for (int z = 1; z < chunk_size_z - 1; ++z) {
        for (int y = 1; y < chunk_size_y - 1; ++y) {
            for (int x = 1; x < chunk_size_x - 1; ++x) {
                const int i = x + y * chunk_size_x + z * plane;
                const float center = density[i];
                const float dx = std::fabs(density[i + 1] - density[i - 1]);
                const float dy = std::fabs(density[i + chunk_size_x] - density[i - chunk_size_x]);
                const float dz = std::fabs(density[i + plane] - density[i - plane]);
                const float gradient = dx + dy + dz;
                if (gradient > threshold) {
                    mask[i] = 1;
                }
                (void)out_mask;
                (void)center;
            }
        }
    }
}

} // namespace nitrium
