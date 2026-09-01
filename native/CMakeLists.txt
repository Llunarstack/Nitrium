# Nitrium Native Core (C++20)
cmake_minimum_required(VERSION 3.20)
project(nitrium_native VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

add_library(nitrium_native SHARED
    src/nitrium_jni.cpp
    src/cpu_capabilities.cpp
    src/simd_frustum_cull.cpp
    src/simd_noise3d.cpp
    src/native_compress.cpp
    src/ring_buffer_io.cpp
    src/arena.cpp
)

target_include_directories(nitrium_native PRIVATE
    ${JNI_INCLUDE_DIRS}
    include
)

target_link_libraries(nitrium_native PRIVATE ${JNI_LIBRARIES})

if(MSVC)
    target_compile_options(nitrium_native PRIVATE /arch:AVX2 /O2)
else()
    target_compile_options(nitrium_native PRIVATE -mavx2 -O3)
endif()

set_target_properties(nitrium_native PROPERTIES
    OUTPUT_NAME "nitrium_native"
)
