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

std::size_t pending_bytes() {
    if (g_ring.head >= g_ring.tail) {
        return g_ring.head - g_ring.tail;
    }
    return g_ring.capacity - g_ring.tail + g_ring.head;
}

std::size_t free_bytes() {
    return g_ring.capacity - pending_bytes();
}

bool write_bytes(const std::uint8_t* input, std::size_t length) {
    if (length == 0 || length > g_ring.capacity || free_bytes() < length) {
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

bool read_bytes(std::uint8_t* output, std::size_t length) {
    if (length == 0 || length > pending_bytes()) {
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
    return pending_bytes();
}

std::size_t ring_buffer_free() {
    return free_bytes();
}

bool ring_buffer_write(const std::uint8_t* input, std::size_t length) {
    return write_bytes(input, length);
}

bool ring_buffer_read(std::uint8_t* output, std::size_t length) {
    return read_bytes(output, length);
}

bool chunk_io_submit_async(const std::uint8_t* payload, std::size_t length) {
    if (length <= 0 || length > 0x7FFFFFFF) {
        return false;
    }

    const auto encoded_length = static_cast<std::uint32_t>(length);
    if (!write_bytes(reinterpret_cast<const std::uint8_t*>(&encoded_length), sizeof(encoded_length))) {
        return false;
    }

    return write_bytes(payload, length);
}

int chunk_io_poll_write(std::uint8_t* output, int output_cap) {
    if (output == nullptr || output_cap <= 0 || pending_bytes() < sizeof(std::uint32_t)) {
        return 0;
    }

    std::uint32_t payload_len = 0;
    if (!read_bytes(reinterpret_cast<std::uint8_t*>(&payload_len), sizeof(payload_len))) {
        return 0;
    }

    if (payload_len == 0 || payload_len > static_cast<std::uint32_t>(output_cap)) {
        return -1;
    }

    if (pending_bytes() < payload_len) {
        return 0;
    }

    if (!read_bytes(output, payload_len)) {
        return -1;
    }

    return static_cast<int>(payload_len);
}

} // namespace nitrium
