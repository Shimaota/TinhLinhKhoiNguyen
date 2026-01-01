# =============================================================================================
#  ProGuard for the iOS build (MobiVM / RoboVM).
#
#  Same idea as desktop/proguard.pro and android/proguard-rules.pro:
#  ONLY obfuscate our own code (com.tlnt.**, com.shimaota.**); every library (libGDX, kryo/kryonet, okhttp,
#  box2dlights, textratypist, jackson, robovm-rt, robovm-cocoatouch...) keeps its names.
#  Mapping: ios/build/obf/mapping.txt — KEEP one per release to retrace stack traces.
# =============================================================================================

-dontwarn **
-dontnote **
# Core code is decompiled -> no bytecode optimization (same as PC/Android), to avoid changing control flow.
-dontoptimize
-dontusemixedcaseclassnames
-keepattributes InnerClasses,EnclosingMethod,Signature,*Annotation*,Exceptions
-renamesourcefileattribute ""
-repackageclasses 'a'
-allowaccessmodification

# New names come from an ASCII dictionary (completely safe for LLVM / Clang / the Mach-O linker / iOS)
-obfuscationdictionary ../desktop/obf/obf-members.txt
-classobfuscationdictionary ../desktop/obf/obf-classes.txt
-packageobfuscationdictionary ../desktop/obf/obf-classes.txt


# ---------- iOS entry points (RoboVM launcher) ----------
-keep public class com.tlnt.ios.IOSLauncher { *; }
-keep public class com.tlnt.ios.IOSSceneDelegate { *; }
-keep class com.tlnt.ios.JavaxAesBackend { *; }
-keep class com.tlnt.ios.OggToM4aAudio { *; }

# ---------- Core classes the iOS launcher calls by name ----------
-keep public class com.tlnt.core.GameMain { public <init>(); }
-keep class com.tlnt.ui.widget.KeyboardInsets { *; }
-keep class com.tlnt.util.AesDecryptor { *; }

# ---------- Same as PC / Android ----------
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
