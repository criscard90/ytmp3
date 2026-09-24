// Wrapper JNI: inizializza LAME, codifica PCM interleaved short -> MP3, flush/close.
// Usato da NativeMp3Encoder per produrre MP3 320 kbps senza FFmpeg.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include "lame.h"

extern "C" {

// gfp salvato come jlong opaco
JNIEXPORT jlong JNICALL
Java_com_criscard90_ytmp3_audio_LameBridge_nInit(
        JNIEnv *env, jclass, jint inSampleRate, jint numChannels,
        jint outSampleRate, jint bitrate, jint quality) {
    lame_global_flags *gfp = lame_init();
    if (!gfp) return 0;
    lame_set_in_samplerate(gfp, inSampleRate);
    lame_set_num_channels(gfp, numChannels);
    lame_set_out_samplerate(gfp, outSampleRate > 0 ? outSampleRate : inSampleRate);
    lame_set_brate(gfp, bitrate);
    lame_set_quality(gfp, quality);
    lame_set_write_id3tag_automatic(gfp, 0); // ID3 gestiti dall'app, non qui
    if (lame_init_params(gfp) < 0) {
        lame_close(gfp);
        return 0;
    }
    return (jlong)(intptr_t)gfp;
}

JNIEXPORT jint JNICALL
Java_com_criscard90_ytmp3_audio_LameBridge_nEncodeInterleaved(
        JNIEnv *env, jclass, jlong handle,
        jshortArray pcm, jint numSamples, jbyteArray mp3buf) {
    lame_global_flags *gfp = (lame_global_flags *)(intptr_t)handle;
    if (!gfp || !pcm || !mp3buf) return -1;
    jshort *in = env->GetShortArrayElements(pcm, nullptr);
    jbyte *out = env->GetByteArrayElements(mp3buf, nullptr);
    if (!in || !out) {
        if (in) env->ReleaseShortArrayElements(pcm, in, JNI_ABORT);
        if (out) env->ReleaseByteArrayElements(mp3buf, out, JNI_ABORT);
        return -1;
    }
    jsize outSize = env->GetArrayLength(mp3buf);
    int encoded = lame_encode_buffer_interleaved(
            gfp, in, numSamples, (unsigned char *)out, outSize);
    env->ReleaseShortArrayElements(pcm, in, JNI_ABORT);
    env->ReleaseByteArrayElements(mp3buf, out, 0);
    return encoded;
}

JNIEXPORT jint JNICALL
Java_com_criscard90_ytmp3_audio_LameBridge_nFlush(
        JNIEnv *env, jclass, jlong handle, jbyteArray mp3buf) {
    lame_global_flags *gfp = (lame_global_flags *)(intptr_t)handle;
    if (!gfp || !mp3buf) return -1;
    jbyte *out = env->GetByteArrayElements(mp3buf, nullptr);
    if (!out) return -1;
    jsize outSize = env->GetArrayLength(mp3buf);
    int encoded = lame_encode_flush(gfp, (unsigned char *)out, outSize);
    env->ReleaseByteArrayElements(mp3buf, out, 0);
    return encoded;
}

JNIEXPORT void JNICALL
Java_com_criscard90_ytmp3_audio_LameBridge_nClose(
        JNIEnv *, jclass, jlong handle) {
    lame_global_flags *gfp = (lame_global_flags *)(intptr_t)handle;
    if (gfp) lame_close(gfp);
}

} // extern "C"
