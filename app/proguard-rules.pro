# The app itself uses no reflection or serialization.

# Google's QR scanner (ML Kit code scanner) sets itself up through reflection that its
# own rules do not fully cover: with R8 full mode, getting a scanner client crashed
# with a NullPointerException inside the library. Keep its classes whole.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_code_scanner.** { *; }
