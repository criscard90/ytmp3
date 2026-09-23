# Regole ProGuard/R8 (disabilitate in questa build: minifyEnabled=false).
# ffmpeg-kit espone JNI: regole conservative già incluse nell'AAR.
-keep class com.arthenica.** { *; }
-keep class com.criscard90.ytmp3.** { *; }
