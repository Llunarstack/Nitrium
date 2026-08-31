#include "nitrium/nitrium.h"

#include <cstdlib>
#include <cstring>

#if defined(__AVX2__)
#include <immintrin.h>
#define NITRIUM_HAS_AVX2 1
#else
#define NITRIUM_HAS_AVX2 0
#endif

namespace nitrium {

namespace {

inline bool aabb_visible_scalar(
    float min_x, float min_y, float min_z,
    float max_x, float max_y, float max_z,
    const float* planes
) {
    for (int p = 0; p < 6; ++p) {
        const float a = planes[p * 4 + 0];
        const float b = planes[p * 4 + 1];
        const float c = planes[p * 4 + 2];
        const float d = planes[p * 4 + 3];

        const float px = a >= 0.0f ? max_x : min_x;
        const float py = b >= 0.0f ? max_y : min_y;
        const float pz = c >= 0.0f ? max_z : min_z;

        if (a * px + b * py + c * pz + d < 0.0f) {
            return false;
        }
    }
    return true;
}

} // namespace

std::uint32_t frustum_cull_soa(
    const float* min_x, const float* min_y, const float* min_z,
    const float* max_x, const float* max_y, const float* max_z,
    const float* planes,
    int count
) {
    std::uint32_t mask = 0;

#if NITRIUM_HAS_AVX2
    int i = 0;
    for (; i + 8 <= count; i += 8) {
        std::uint32_t lane_mask = 0;
        for (int lane = 0; lane < 8; ++lane) {
            const int idx = i + lane;
            if (aabb_visible_scalar(
                    min_x[idx], min_y[idx], min_z[idx],
                    max_x[idx], max_y[idx], max_z[idx],
                    planes)) {
                lane_mask |= (1u << lane);
            }
        }
        mask |= (lane_mask << i);
    }
    for (; i < count; ++i) {
        if (aabb_visible_scalar(
                min_x[i], min_y[i], min_z[i],
                max_x[i], max_y[i], max_z[i],
                planes)) {
            mask |= (1u << i);
        }
    }
#else
    for (int i = 0; i < count; ++i) {
        if (aabb_visible_scalar(
                min_x[i], min_y[i], min_z[i],
                max_x[i], max_y[i], max_z[i],
                planes)) {
            mask |= (1u << i);
        }
    }
#endif

    return mask;
}

} // namespace nitrium
