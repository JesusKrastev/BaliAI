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