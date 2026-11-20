# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---- ShareAPK: libraries loaded reflectively / by service lookup ----
# Netty (Ktor server engine) uses reflection, service loaders and optional dependencies.
-keep class io.netty.** { *; }
-dontwarn io.netty.**
# Ktor server
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
# BouncyCastle (self-signed TLS certificate)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
# kotlinx.serialization: keep generated serializers
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.example.** { kotlinx.serialization.KSerializer serializer(...); }
-keep @kotlinx.serialization.Serializable class com.example.** { *; }
# Optional JVM-only dependencies referenced by the libraries above
-dontwarn org.slf4j.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.**
-dontwarn reactor.blockhound.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.**
-dontwarn com.google.protobuf.**
-dontwarn com.oracle.svm.**
-dontwarn javax.naming.**
-dontwarn java.lang.management.**
-dontwarn sun.security.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.openjsse.**

# Shizuku starts the UserService by class name (reflection) in its own process.
-keep class com.example.installer.ShellService { <init>(...); }
-keep class com.example.installer.IShellService** { *; }
