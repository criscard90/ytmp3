/* Configurazione per la build Android NDK di libmp3lame (LAME 3.100).
 * Equivalente minimale di config.h: Android (bionic) ha tutti gli header
 * standard, niente NASM/assembler, niente mpglib (solo encoding). */
#ifndef LAME_CONFIG_H
#define LAME_CONFIG_H

#define STDC_HEADERS 1
#define HAVE_ERRNO_H 1
#define HAVE_FCNTL_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_STRCHR 1
#define HAVE_MEMCPY 1
#define HAVE_IEEE754_FLOAT 1

/* Aggiunto come fa configure di LAME: il tipo usato da util.h/util.c per i
 * logaritmi veloci. Bionic (Android) non fornisce <ieee754.h>, quindi lo
 * definiamo qui esattamente come farebbe il config.h generato. */
#undef HAVE_IEEE754_FLOAT32_T
#ifndef HAVE_IEEE754_FLOAT32_T
typedef float ieee754_float32_t;
#endif

#define INLINE __inline

#define PACKAGE "lame"
#define PACKAGE_NAME "lame"
#define PACKAGE_VERSION "3.100"
#define VERSION "3.100"
#define LAME_MAJOR_VERSION 3
#define LAME_MINOR_VERSION 100
#define LAME_PATCH_VERSION 0

#endif /* LAME_CONFIG_H */
