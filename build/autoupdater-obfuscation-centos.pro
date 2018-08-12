-injars ../target/AutoUpdaterAPI.jar
-outjars ../target/AutoUpdaterAPI-obfsc.jar

-libraryjars '/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.181-3.b13.el7_5.x86_64/jre/lib/rt.jar'

-target 1.8
-dontshrink
-dontoptimize
-dontwarn
-dontnote **
-overloadaggressively
-adaptclassstrings
-useuniqueclassmembernames
-keeppackagenames net.wesjd,be.maximvdw,com.gargoylesoftware,org.spigotmc,commons-codec,commons-logging,org.jsoup,org.projectlombok
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,LocalVariable*Table,*Annotation*,Synthetic,EnclosingMethod
-renamesourcefileattribute SourceFile

-keep class !com.gamerking195.dev.autoupdaterapi.util.UtilEncryption,!com.gamerking195.dev.autoupdaterapi.util.UtilSpigotCreds { *; }

# Keep - Applications. Keep all application classes, along with their 'main'
# methods.
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Also keep - Enumerations. Keep the special static methods that are required in
# enumeration classes.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Also keep - Database drivers. Keep all implementations of java.sql.Driver.
-keep class * extends java.sql.Driver

# Also keep - Swing UI L&F. Keep all extensions of javax.swing.plaf.ComponentUI,
# along with the special 'createUI' method.
-keep class * extends javax.swing.plaf.ComponentUI {
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent);
}
