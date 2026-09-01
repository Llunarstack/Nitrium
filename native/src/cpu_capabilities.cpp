#include "nitrium/nitrium.h"

#if defined(_MSC_VER)
#include <intrin.h>
#elif defined(__GNUC__) || defined(__clang__)
#include <cpuid.h>
#endif

#include <cstring>

namespace nitrium {

namespace {

enum class CpuVendorId : int {
    UNKNOWN = 0,
    INTEL = 1,
    AMD = 2,
};

CpuVendorId g_vendor = CpuVendorId::UNKNOWN;
int g_logical_cores = 1;
int g_physical_cores = 1;
bool g_avx2 = false;
bool g_avx512 = false;
bool g_probed = false;

void cpuid(int leaf, int subleaf, int out[4]) {
#if defined(_MSC_VER)
    __cpuidex(out, leaf, subleaf);
#else
    __cpuid_count(leaf, subleaf, out[0], out[1], out[2], out[3]);
#endif
}

void probe_once() {
    if (g_probed) {
        return;
    }
    g_probed = true;

    int info[4] = {0, 0, 0, 0};
    cpuid(0, 0, info);
    const int max_leaf = info[0];

    char vendor[13] = {};
    std::memcpy(vendor + 0, &info[1], 4);
    std::memcpy(vendor + 4, &info[3], 4);
    std::memcpy(vendor + 8, &info[2], 4);

    if (std::strcmp(vendor, "GenuineIntel") == 0) {
        g_vendor = CpuVendorId::INTEL;
    } else if (std::strcmp(vendor, "AuthenticAMD") == 0) {
        g_vendor = CpuVendorId::AMD;
    }

    cpuid(1, 0, info);
    g_logical_cores = (info[1] >> 16) & 0xFF;
    if (g_logical_cores <= 0) {
        g_logical_cores = 1;
    }

    g_physical_cores = g_logical_cores;

    if (max_leaf >= 0x0B) {
        // Extended topology: subleaf 0 (SMT level) EBX = logical processors per core;
        // subleaf 1 (core level) EBX = logical processors per package. Physical cores are the
        // package total divided by the per-core thread count — NOT subleaf 0 directly, which is
        // just the SMT width (e.g. 2 on a hyper-threaded CPU).
        int smt[4] = {0, 0, 0, 0};
        int core[4] = {0, 0, 0, 0};
        cpuid(0x0B, 0, smt);
        cpuid(0x0B, 1, core);
        const int threads_per_core = smt[1] & 0xFFFF;
        const int logical_per_package = core[1] & 0xFFFF;
        if (threads_per_core > 0 && logical_per_package >= threads_per_core) {
            g_physical_cores = logical_per_package / threads_per_core;
            g_logical_cores = logical_per_package;
        }
    }

    if (g_physical_cores <= 0) {
        g_physical_cores = 1;
    }

    if (max_leaf >= 7) {
        cpuid(7, 0, info);
        g_avx2 = (info[1] & (1 << 5)) != 0;
        g_avx512 = (info[1] & (1 << 16)) != 0; // AVX-512F
    }
}

} // namespace

int cpu_vendor() {
    probe_once();
    return static_cast<int>(g_vendor);
}

int cpu_logical_cores() {
    probe_once();
    return g_logical_cores;
}

int cpu_physical_cores() {
    probe_once();
    return g_physical_cores;
}

bool cpu_has_avx2() {
    probe_once();
    return g_avx2;
}

bool cpu_has_avx512() {
    probe_once();
    return g_avx512;
}

} // namespace nitrium
