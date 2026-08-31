#include "nitrium/nitrium.h"

#include <jni.h>

namespace nitrium {

const char* version() {
    return "0.1.0-avx2";
}

} // namespace nitrium

extern "C" {

JNIEXPORT jstring JNICALL Java_dev_nitrium_nativecore_NitriumNative_nitriumVersion(JNIEnv* env, jclass) {
    return env->NewStringUTF(nitrium::version());
}

JNIEXPORT jboolean JNICALL Java_dev_nitrium_nativecore_NitriumNative_hasAvx2(JNIEnv*, jclass) {
#if defined(__AVX2__)
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL Java_dev_nitrium_nativecore_NativeMemoryArena_nativeAlloc(JNIEnv*, jclass, jlong bytes) {
    return reinterpret_cast<jlong>(nitrium::arena_alloc(static_cast<std::size_t>(bytes)));
}

JNIEXPORT void JNICALL Java_dev_nitrium_nativecore_NativeMemoryArena_nativeReset(JNIEnv*, jclass) {
    nitrium::arena_reset();
}

JNIEXPORT void JNICALL Java_dev_nitrium_nativecore_NativeMemoryArena_nativeShutdown(JNIEnv*, jclass) {
    nitrium::arena_shutdown();
}

JNIEXPORT jint JNICALL Java_dev_nitrium_nativecore_SimdFrustumCuller_nativeCullSoa(
    JNIEnv* env,
    jclass,
    jfloatArray min_x,
    jfloatArray min_y,
    jfloatArray min_z,
    jfloatArray max_x,
    jfloatArray max_y,
    jfloatArray max_z,
    jfloatArray planes,
    jint count
) {
    auto* px = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(min_x, nullptr));
    auto* py = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(min_y, nullptr));
    auto* pz = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(min_z, nullptr));
    auto* qx = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(max_x, nullptr));
    auto* qy = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(max_y, nullptr));
    auto* qz = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(max_z, nullptr));
    auto* pl = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(planes, nullptr));

    const auto mask = nitrium::frustum_cull_soa(
        px, py, pz, qx, qy, qz, pl, count
    );

    env->ReleasePrimitiveArrayCritical(min_x, px, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(min_y, py, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(min_z, pz, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(max_x, qx, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(max_y, qy, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(max_z, qz, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(planes, pl, JNI_ABORT);

    return static_cast<jint>(mask);
}

JNIEXPORT void JNICALL Java_dev_nitrium_nativecore_SimdNoise3D_nativeFillCoarse(
    JNIEnv* env,
    jclass,
    jfloatArray out,
    jint size_x,
    jint size_y,
    jint size_z,
    jfloat origin_x,
    jfloat origin_y,
    jfloat origin_z,
    jfloat step_x,
    jfloat step_y,
    jfloat step_z,
    jint seed
) {
    auto* buffer = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(out, nullptr));
    nitrium::noise3d_fill_coarse(
        buffer, size_x, size_y, size_z,
        origin_x, origin_y, origin_z,
        step_x, step_y, step_z,
        static_cast<std::uint32_t>(seed)
    );
    env->ReleasePrimitiveArrayCritical(out, buffer, 0);
}

JNIEXPORT void JNICALL Java_dev_nitrium_nativecore_SimdNoise3D_nativeTrilinearFill(
    JNIEnv* env,
    jclass,
    jfloatArray out,
    jint chunk_x,
    jint chunk_y,
    jint chunk_z,
    jfloatArray coarse,
    jint coarse_x,
    jint coarse_y,
    jint coarse_z,
    jint step_x,
    jint step_y,
    jint step_z
) {
    auto* out_ptr = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(out, nullptr));
    auto* coarse_ptr = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(coarse, nullptr));
    nitrium::noise3d_trilinear_fill(
        out_ptr, chunk_x, chunk_y, chunk_z,
        coarse_ptr, coarse_x, coarse_y, coarse_z,
        step_x, step_y, step_z
    );
    env->ReleasePrimitiveArrayCritical(coarse, coarse_ptr, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(out, out_ptr, 0);
}

JNIEXPORT void JNICALL Java_dev_nitrium_nativecore_SimdNoise3D_nativeMarkHighGradient(
    JNIEnv* env,
    jclass,
    jbyteArray mask,
    jint chunk_x,
    jint chunk_y,
    jint chunk_z,
    jfloatArray density,
    jfloat threshold
) {
    auto* mask_ptr = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(mask, nullptr));
    auto* density_ptr = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(density, nullptr));
    nitrium::noise3d_mark_high_gradient(
        nullptr,
        reinterpret_cast<std::uint8_t*>(mask_ptr),
        chunk_x, chunk_y, chunk_z,
        density_ptr,
        threshold
    );
    env->ReleasePrimitiveArrayCritical(density, density_ptr, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(mask, mask_ptr, 0);
}

JNIEXPORT jint JNICALL Java_dev_nitrium_nativecore_NativePacketCompressor_nativeCompress(
    JNIEnv* env,
    jclass,
    jbyteArray input,
    jbyteArray output
) {
    const jsize input_len = env->GetArrayLength(input);
    const jsize output_cap = env->GetArrayLength(output);
    auto* in_ptr = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(input, nullptr));
    auto* out_ptr = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(output, nullptr));

    const int written = nitrium::packet_compress(
        reinterpret_cast<const std::uint8_t*>(in_ptr),
        static_cast<int>(input_len),
        reinterpret_cast<std::uint8_t*>(out_ptr),
        static_cast<int>(output_cap)
    );

    env->ReleasePrimitiveArrayCritical(output, out_ptr, written > 0 ? 0 : JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(input, in_ptr, JNI_ABORT);
    return written;
}

JNIEXPORT jint JNICALL Java_dev_nitrium_nativecore_NativePacketCompressor_nativeDecompress(
    JNIEnv* env,
    jclass,
    jbyteArray input,
    jbyteArray output
) {
    const jsize input_len = env->GetArrayLength(input);
    const jsize output_cap = env->GetArrayLength(output);
    auto* in_ptr = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(input, nullptr));
    auto* out_ptr = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(output, nullptr));

    const int written = nitrium::packet_decompress(
        reinterpret_cast<const std::uint8_t*>(in_ptr),
        static_cast<int>(input_len),
        reinterpret_cast<std::uint8_t*>(out_ptr),
        static_cast<int>(output_cap)
    );

    env->ReleasePrimitiveArrayCritical(output, out_ptr, written > 0 ? 0 : JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(input, in_ptr, JNI_ABORT);
    return written;
}

JNIEXPORT jboolean JNICALL Java_dev_nitrium_nativecore_NativeChunkIo_nativeInitRingBuffer(JNIEnv*, jclass, jlong capacity) {
    return nitrium::ring_buffer_init(static_cast<std::size_t>(capacity)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_dev_nitrium_nativecore_NativeChunkIo_nativeShutdownRingBuffer(JNIEnv*, jclass) {
    nitrium::ring_buffer_shutdown();
}

JNIEXPORT jlong JNICALL Java_dev_nitrium_nativecore_NativeChunkIo_nativePendingBytes(JNIEnv*, jclass) {
    return static_cast<jlong>(nitrium::ring_buffer_pending());
}

JNIEXPORT jboolean JNICALL Java_dev_nitrium_nativecore_NativeChunkIo_nativeSubmitWrite(JNIEnv* env, jclass, jbyteArray payload) {
    const jsize length = env->GetArrayLength(payload);
    auto* bytes = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(payload, nullptr));
    const bool ok = nitrium::chunk_io_submit_async(reinterpret_cast<const std::uint8_t*>(bytes), static_cast<std::size_t>(length));
    env->ReleasePrimitiveArrayCritical(payload, bytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
