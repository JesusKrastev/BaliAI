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

# --- Firebase Firestore Data Models ---
# Evita que Proguard ofusque las clases que mapean campos desde/hacia Firestore
-keep class com.jesuskrastev.bali.data.remote.firestore.entities.** { *; }
-keep class com.jesuskrastev.bali.domain.model.** { *; }

# --- Room Database Entities ---
# Mantiene las clases relacionadas con la base de datos local para evitar fallos de instanciación
-keep class com.jesuskrastev.bali.data.local.room.entities.** { *; }

# Mantener atributos Genéricos y Anotaciones para Gson/Firestore
-keepattributes Signature
-keepattributes *Annotation*

# --- Cryptography & Security Provider (Fix BadPaddingException) ---
# Keep all javax.crypto and java.security classes to prevent decryption failures
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class javax.net.ssl.** { *; }
-keep interface javax.crypto.** { *; }
-keep interface java.security.** { *; }

# Keep Android security provider classes
-keep class android.security.** { *; }
-keep class androidx.security.** { *; }
-keep class com.android.org.conscrypt.** { *; }

# Keep cipher-related classes for encryption/decryption
-keepclassmembers class * {
    *** *Cipher*(...);
    *** *Crypto*(...);
    *** *Decrypt*(...);
    *** *Encrypt*(...);
}

# DataStore Preferences (if using encrypted ones)
-keep class android.content.SharedPreferences { *; }
-keep class androidx.datastore.** { *; }

# Firebase Security Rules & SSL/TLS
-keep class com.google.android.gms.net.** { *; }
-keep class android.net.ssl.** { *; }

# Prevent obfuscation of inner classes in crypto providers
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Certificate & Keystore Handling ---
# Keep certificate and keystore related classes
-keep class java.security.cert.** { *; }
-keep class sun.security.** { *; }
-keep class com.sun.** { *; }
-keep class org.bouncycastle.** { *; }

# Conscrypt (used for SSL/TLS)
-keep class org.conscrypt.** { *; }
-keep interface org.conscrypt.** { *; }

# Keep all inner classes and their members
-keepclasseswithmembers class * {
    private static final byte[] *;
}

# Preserve all provider implementations
-keep class java.security.Provider { *; }
-keep class * extends java.security.Provider { *; }
-keep class java.security.SecureRandom { *; }
-keep class * extends java.security.MessageDigestSpi { *; }
-keep class * extends javax.crypto.CipherSpi { *; }
-keep class * extends java.security.KeyFactorySpi { *; }
-keep class * extends java.security.KeyStoreSpi { *; }
-keep class * extends java.security.SignatureSpi { *; }

# Keep parameterized types
-keepattributes TypeAnnotatedVisibility,RuntimeVisibleTypeAnnotations

# --- ASN.1 & PKCS#12 Handling (for certificate/keystore parsing) ---
# Critical for "safe contents entry" error fixes
-keep class sun.security.pkcs.** { *; }
-keep class sun.security.provider.** { *; }
-keep class sun.security.x509.** { *; }
-keep class sun.security.asn1.** { *; }
-keep class com.sun.org.apache.xerces.** { *; }
-keep class com.sun.org.apache.xalan.** { *; }

# Keep all constructors and methods of security classes
-keepclasseswithmembers class java.security.KeyStore {
    <init>(...);
    <methods>;
}

-keepclasseswithmembers class javax.crypto.Cipher {
    <init>(...);
    <methods>;
}

# --- FIREBASE & GOOGLE PLAY SERVICES (CRITICAL) ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep interface com.google.firebase.** { *; }
-keep interface com.google.android.gms.** { *; }

# --- PREVENT OBFUSCATION OF CRYPTO/SECURITY ---
# Do NOT obfuscate crypto-related packages
-keep,allowobfuscation class sun.security.** { *; }
-keep,allowobfuscation class java.security.** { *; }
-keep,allowobfuscation class javax.crypto.** { *; }

# Keep all reflection-accessible methods
-keepclasseswithmembers class * {
    *** *Cipher*(...);
    *** *DecryptCipherInputStream(...);
    *** *Key(...);
    *** *Certificate(...);
}

# Keep source file and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable,Exceptions
-renamesourcefileattribute SourceFile