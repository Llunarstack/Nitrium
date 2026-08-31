#include "nitrium/nitrium.h"

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <vector>

namespace nitrium {

namespace {

struct RingBufferState {
    std::vector<std::uint8_t> data;
    std::size_t head = 0;
    std::size_t tail = 0;
    std::size_t capacity = 0;
};

RingBufferState g_ring;

} // namespace

bool ring_buffer_init(std::size_t capacity_bytes) {
    if (capacity_bytes == 0) {
        return false;
    }
    g_ring.data.assign(capacity_bytes, 0);
    g_ring.head = 0;
    g_ring.tail = 0;
    g_ring.capacity = capacity_bytes;
    return true;
}

void ring_buffer_shutdown() {
    g_ring.data.clear();
    g_ring.head = 0;
    g_ring.tail = 0;
    g_ring.capacity = 0;
}

std::size_t ring_buffer_pending() {
    if (g_ring.head >= g_ring.tail) {
        return g_ring.head - g_ring.tail;
    }
    return g_ring.capacity - g_ring.tail + g_ring.head;
}

std::size_t ring_buffer_free() {
    return g_ring.capacity - ring_buffer_pending();
}

bool ring_buffer_write(const std::uint8_t* input, std::size_t length) {
    if (length == 0 || length > g_ring.capacity || ring_buffer_free() < length) {
        return false;
    }

    const std::size_t first_chunk = std::min(length, g_ring.capacity - g_ring.head);
    std::memcpy(g_ring.data.data() + g_ring.head, input, first_chunk);
    if (length > first_chunk) {
        std::memcpy(g_ring.data.data(), input + first_chunk, length - first_chunk);
    }

    g_ring.head = (g_ring.head + length) % g_ring.capacity;
    return true;
}

bool ring_buffer_read(std::uint8_t* output, std::size_t length) {
    if (length == 0 || length > ring_buffer_pending()) {
        return false;
    }

    const std::size_t first_chunk = std::min(length, g_ring.capacity - g_ring.tail);
    std::memcpy(output, g_ring.data.data() + g_ring.tail, first_chunk);
    if (length > first_chunk) {
        std::memcpy(output + first_chunk, g_ring.data.data(), length - first_chunk);
    }

    g_ring.tail = (g_ring.tail + length) % g_ring.capacity;
    return true;
}

// For now this only queues the write into the ring buffer. TODO: real async I/O
// (Windows overlapped / Linux io_uring) to drain it to disk off-thread.
bool chunk_io_submit_async(const std::uint8_t* payload, std::size_t length) {
    return ring_buffer_write(payload, length);
}

} // namespace nitrium
