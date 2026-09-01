#include "nitrium/nitrium.h"

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

#if NITRIUM_HAS_AVX2
inline std::uint32_t frustum_cull_avx2_batch(
    const float* min_x, const float* min_y, const float* min_z,
    const float* max_x, const float* max_y, const float* max_z,
    const float* planes,
    int offset
) {
    const __m256 min_xv = _mm256_loadu_ps(min_x + offset);
    const __m256 min_yv = _mm256_loadu_ps(min_y + offset);
    const __m256 min_zv = _mm256_loadu_ps(min_z + offset);
    const __m256 max_xv = _mm256_loadu_ps(max_x + offset);
    const __m256 max_yv = _mm256_loadu_ps(max_y + offset);
    const __m256 max_zv = _mm256_loadu_ps(max_z + offset);

    __m256 visible = _mm256_castsi256_ps(_mm256_set1_epi32(-1));

    for (int p = 0; p < 6; ++p) {
        const float a = planes[p * 4 + 0];
        const float b = planes[p * 4 + 1];
        const float c = planes[p * 4 + 2];
        const float d = planes[p * 4 + 3];

        const __m256 av = _mm256_set1_ps(a);
        const __m256 bv = _mm256_set1_ps(b);
        const __m256 cv = _mm256_set1_ps(c);
        const __m256 dv = _mm256_set1_ps(d);

        const __m256 sign_mask = _mm256_set1_ps(-0.0f);
        const __m256 px = _mm256_blendv_ps(max_xv, min_xv, _mm256_and_ps(av, sign_mask));
        const __m256 py = _mm256_blendv_ps(max_yv, min_yv, _mm256_and_ps(bv, sign_mask));
        const __m256 pz = _mm256_blendv_ps(max_zv, min_zv, _mm256_and_ps(cv, sign_mask));

        const __m256 dist = _mm256_add_ps(
            _mm256_add_ps(_mm256_mul_ps(av, px), _mm256_mul_ps(bv, py)),
            _mm256_add_ps(_mm256_mul_ps(cv, pz), dv)
        );

        visible = _mm256_and_ps(visible, _mm256_cmp_ps(dist, _mm256_setzero_ps(), _CMP_GE_OQ));
    }

    const int mask8 = _mm256_movemask_ps(visible);
    return static_cast<std::uint32_t>(mask8);
}
#endif

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
        const std::uint32_t lane_mask = frustum_cull_avx2_batch(
            min_x, min_y, min_z, max_x, max_y, max_z, planes, i
        );
        mask |= lane_mask << i;
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
