#include "nitrium/nitrium.h"

#include <cstdint>
#include <cstring>

namespace nitrium {

namespace {

constexpr std::uint32_t NITRIUM_PACKET_MAGIC = 0x4E495452u; // "NITR"

} // namespace

// Identity wrapper for now. TODO: swap in libdeflate/zstd (SIMD).
int packet_compress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap) {
    const int needed = 8 + input_len;
    if (output_cap < needed || input_len < 0) {
        return -1;
    }

    std::memcpy(output, &NITRIUM_PACKET_MAGIC, 4);
    std::memcpy(output + 4, &input_len, 4);
    std::memcpy(output + 8, input, static_cast<std::size_t>(input_len));
    return needed;
}

int packet_decompress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap) {
    if (input_len < 8) {
        return -1;
    }

    std::uint32_t magic = 0;
    std::memcpy(&magic, input, 4);
    if (magic != NITRIUM_PACKET_MAGIC) {
        return -1;
    }

    int payload_len = 0;
    std::memcpy(&payload_len, input + 4, 4);
    if (payload_len < 0 || 8 + payload_len > input_len || payload_len > output_cap) {
        return -1;
    }

    std::memcpy(output, input + 8, static_cast<std::size_t>(payload_len));
    return payload_len;
}

} // namespace nitrium
