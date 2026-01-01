# ProGuard for the PC build (task :desktop:obfJar). ONLY obfuscates our own code (com.tlnt.**, com.shimaota.**): libraries
# (libGDX, kryo/kryonet, okhttp, box2dlights, textratypist, jackson, lwjgl...) are libraryjars and stay untouched.
# Mapping: desktop/build/obf/mapping.txt — KEEP one per release to read stack traces (retrace).
# Renaming style / optimization are toggled in desktop/build.gradle (-Pobf=..., -Popt=on).

-dontwarn **
-dontnote **
-dontusemixedcaseclassnames
-keepattributes InnerClasses,EnclosingMethod,Signature,*Annotation*,Exceptions
-renamesourcefileattribute ""
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively

# Entry points
-keep public class com.shimaota.DesktopLauncher { public static void main(java.lang.String[]); }

# Kryo packets (KryoNet): FieldSerializer writes fields IN NAME ORDER -> renaming a field = protocol mismatch with the server.
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
