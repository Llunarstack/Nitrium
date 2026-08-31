#include "nitrium/nitrium.h"

#include <cstdlib>
#include <mutex>

namespace nitrium {

namespace {
    std::mutex arena_mutex;
    void* arena_base = nullptr;
    std::size_t arena_capacity = 0;
    std::size_t arena_offset = 0;
    constexpr std::size_t DEFAULT_ARENA = 256 * 1024 * 1024; // 256 MB
}

void* arena_alloc(std::size_t bytes) {
    std::lock_guard lock(arena_mutex);

    if (arena_base == nullptr) {
        arena_capacity = DEFAULT_ARENA;
        arena_base = std::malloc(arena_capacity);
        arena_offset = 0;
    }

    const std::size_t aligned = (bytes + 63) & ~std::size_t(63);
    if (arena_offset + aligned > arena_capacity) {
        arena_offset = 0; // bump reset — caller must not hold stale pointers
    }

    void* ptr = static_cast<char*>(arena_base) + arena_offset;
    arena_offset += aligned;
    return ptr;
}

void arena_reset() {
    std::lock_guard lock(arena_mutex);
    arena_offset = 0;
}

void arena_shutdown() {
    std::lock_guard lock(arena_mutex);
    std::free(arena_base);
    arena_base = nullptr;
    arena_capacity = 0;
    arena_offset = 0;
}

} // namespace nitrium
