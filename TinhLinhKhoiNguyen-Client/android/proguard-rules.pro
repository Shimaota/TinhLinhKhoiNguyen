# R8 for the Android release build (android/build.gradle, -Pobf=off to disable). Same idea as desktop/proguard.pro: ONLY
# obfuscate our own code (com.tlnt.**, com.shimaota.**); every library (libGDX, kryo/kryonet, okhttp, box2dlights,
# textratypist, jackson...) keeps its names. Unlike PC, R8 also processes the libraries (no libraryjars), hence the -keep rules.
# Mapping: android/build/outputs/mapping/release/mapping.txt — KEEP one per release for retracing.

-dontwarn **
-dontnote **
# Core code is decompiled -> no bytecode optimization (same as PC), to avoid changing control flow.
-dontoptimize
-dontusemixedcaseclassnames
-keepattributes InnerClasses,EnclosingMethod,Signature,*Annotation*,Exceptions
-renamesourcefileattribute ""
-repackageclasses 'a'
-allowaccessmodification

# New names come from a dictionary (ASCII: safe for dex / every Android device)
-obfuscationdictionary ../desktop/obf/obf-members.txt
-classobfuscationdictionary ../desktop/obf/obf-classes.txt
-packageobfuscationdictionary ../desktop/obf/obf-classes.txt

# ---------- Libraries: keep everything (equivalent to libraryjars on PC) ----------
-keep class !com.tlnt.**,!com.shimaota.** { *; }

# ---------- Android entry points ----------
-keep public class com.tlnt.android.AndroidLauncher { *; }
-keep class com.tlnt.android.R
-keep class com.tlnt.android.R$* { *; }

# ---------- Same as PC ----------
# Kryo packets: FieldSerializer writes fields IN NAME ORDER -> renaming = protocol mismatch with the server.
-keep class com.tlnt.net.DataPacket { *; }
-keep class com.tlnt.net.FragmentPacket { *; }

# Enum / native / Serializable — standard rules
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
