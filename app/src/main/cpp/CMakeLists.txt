# libmp3lame (LAME 3.100) + wrapper JNI, compilati con NDK via CMake.
# Nessun assembler/NASM: solo C portabile (takehiro/newmdct C puro).
cmake_minimum_required(VERSION 3.22.1)

project(ytmp3lame C CXX)

set(LAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/lame/libmp3lame)

# Tutti i sorgenti tranne il frontend a riga di comando (main.c non presente
# in libmp3lame; mpglib_interface.c serve solo al decoding: escluso).
file(GLOB LAME_SOURCES
    ${LAME_DIR}/bitstream.c
    ${LAME_DIR}/encoder.c
    ${LAME_DIR}/fft.c
    ${LAME_DIR}/gain_analysis.c
    ${LAME_DIR}/id3tag.c
    ${LAME_DIR}/lame.c
    ${LAME_DIR}/newmdct.c
    ${LAME_DIR}/presets.c
    ${LAME_DIR}/psymodel.c
    ${LAME_DIR}/quantize.c
    ${LAME_DIR}/quantize_pvt.c
    ${LAME_DIR}/reservoir.c
    ${LAME_DIR}/set_get.c
    ${LAME_DIR}/tables.c
    ${LAME_DIR}/takehiro.c
    ${LAME_DIR}/util.c
    ${LAME_DIR}/vbrquantize.c
    ${LAME_DIR}/VbrTag.c
    ${LAME_DIR}/version.c
)

add_library(ytmp3lame SHARED
    lame_jni.cpp
    ${LAME_SOURCES}
)

target_include_directories(ytmp3lame PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/lame
    ${LAME_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/lame/include
)

# Abilita config.h minimale + header standard; niente ASM.
target_compile_definitions(ytmp3lame PRIVATE
    HAVE_CONFIG_H=1
    STDC_HEADERS=1
    HAVE_STDINT_H=1
    HAVE_STDLIB_H=1
    HAVE_STRING_H=1
    HAVE_INTTYPES_H=1
    HAVE_ERRNO_H=1
    HAVE_FCNTL_H=1
    HAVE_STRCHR=1
    HAVE_MEMCPY=1
    HAVE_IEEE754_FLOAT=1
    NDEBUG
)

target_compile_options(ytmp3lame PRIVATE -O3 -ffast-math)

target_link_libraries(ytmp3lame log m)
