#include "nitrium/nitrium.h"

#include <cstdint>
#include <cstring>

namespace nitrium {

namespace {

constexpr std::uint32_t NITRIUM_PACKET_MAGIC = 0x4E495452u; // "NITR"
constexpr std::uint32_t NITRIUM_RAW_FLAG = 0x00000000u;
constexpr std::uint32_t NITRIUM_LZ_FLAG = 0x00000001u;
constexpr int LZ_WINDOW = 4096;
constexpr int LZ_MIN_MATCH = 4;
constexpr int LZ_MAX_MATCH = 255; // match length is encoded in a single byte

bool lz_compress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap, int* out_len) {
    if (input_len <= 0 || output_cap < 12) {
        return false;
    }

    // Emit a pure token stream; the 12-byte packet header is added by packet_compress.
    int write = 0;
    int read = 0;

    while (read < input_len) {
        int best_len = 0;
        int best_dist = 0;

        const int window_start = read > LZ_WINDOW ? read - LZ_WINDOW : 0;
        for (int candidate = read - 1; candidate >= window_start; --candidate) {
            int match = 0;
            while (read + match < input_len
                   && match < LZ_MAX_MATCH
                   && input[candidate + match] == input[read + match]) {
                ++match;
            }
            if (match >= LZ_MIN_MATCH && match > best_len) {
                best_len = match;
                best_dist = read - candidate;
            }
        }

        if (best_len >= LZ_MIN_MATCH) {
            if (write + 4 > output_cap) {
                return false;
            }
            output[write++] = 0xFF;
            output[write++] = static_cast<std::uint8_t>(best_dist & 0xFF);
            output[write++] = static_cast<std::uint8_t>((best_dist >> 8) & 0xFF);
            output[write++] = static_cast<std::uint8_t>(best_len);
            read += best_len;
        } else {
            if (write + 2 > output_cap) {
                return false;
            }
            output[write++] = 0x00;
            output[write++] = input[read++];
        }
    }

    *out_len = write;
    return true;
}

bool lz_decompress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap, int* out_len) {
    if (input_len < 0) {
        return false;
    }

    // input is the pure token stream (packet header already stripped by the caller).
    int read = 0;
    int write = 0;

    while (read < input_len) {
        const std::uint8_t tag = input[read++];
        if (tag == 0x00) {
            if (read >= input_len || write >= output_cap) {
                return false;
            }
            output[write++] = input[read++];
        } else if (tag == 0xFF) {
            if (read + 3 > input_len) {
                return false;
            }
            const int dist = input[read] | (input[read + 1] << 8);
            const int len = input[read + 2];
            read += 3;
            if (dist <= 0 || write - dist < 0 || write + len > output_cap) {
                return false;
            }
            for (int i = 0; i < len; ++i) {
                output[write] = output[write - dist];
                ++write;
            }
        } else {
            return false;
        }
    }

    *out_len = write;
    return true;
}

} // namespace

int packet_compress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap) {
    if (input_len < 0) {
        return -1;
    }

    if (input_len == 0) {
        if (output_cap < 12) {
            return -1;
        }
        std::memcpy(output, &NITRIUM_PACKET_MAGIC, 4);
        const std::uint32_t flags = NITRIUM_RAW_FLAG;
        std::memcpy(output + 4, &flags, 4);
        const std::uint32_t original = 0;
        std::memcpy(output + 8, &original, 4);
        return 12;
    }

    alignas(64) std::uint8_t scratch[65536];
    int compressed_len = 0;
    const bool lz_ok = input_len <= static_cast<int>(sizeof(scratch))
        && lz_compress(input, input_len, scratch, static_cast<int>(sizeof(scratch)), &compressed_len);

    const bool use_lz = lz_ok && compressed_len + 12 < input_len + 12;
    const int needed = use_lz ? compressed_len + 12 : input_len + 12;
    if (output_cap < needed) {
        return -1;
    }

    std::memcpy(output, &NITRIUM_PACKET_MAGIC, 4);
    const std::uint32_t flags = use_lz ? NITRIUM_LZ_FLAG : NITRIUM_RAW_FLAG;
    std::memcpy(output + 4, &flags, 4);
    std::memcpy(output + 8, &input_len, 4);

    if (use_lz) {
        std::memcpy(output + 12, scratch, static_cast<std::size_t>(compressed_len));
        return compressed_len + 12;
    }

    std::memcpy(output + 12, input, static_cast<std::size_t>(input_len));
    return input_len + 12;
}

int packet_decompress(const std::uint8_t* input, int input_len, std::uint8_t* output, int output_cap) {
    if (input_len < 12) {
        return -1;
    }

    std::uint32_t magic = 0;
    std::memcpy(&magic, input, 4);
    if (magic != NITRIUM_PACKET_MAGIC) {
        return -1;
    }

    std::uint32_t flags = 0;
    std::memcpy(&flags, input + 4, 4);
    int original_len = 0;
    std::memcpy(&original_len, input + 8, 4);
    if (original_len < 0 || original_len > output_cap) {
        return -1;
    }

    const int payload_len = input_len - 12;
    if (payload_len < 0) {
        return -1;
    }

    if (flags == NITRIUM_RAW_FLAG) {
        if (payload_len != original_len) {
            return -1;
        }
        std::memcpy(output, input + 12, static_cast<std::size_t>(original_len));
        return original_len;
    }

    if (flags == NITRIUM_LZ_FLAG) {
        int out_len = 0;
        if (!lz_decompress(input + 12, payload_len, output, output_cap, &out_len) || out_len != original_len) {
            return -1;
        }
        return out_len;
    }

    return -1;
}

} // namespace nitrium
