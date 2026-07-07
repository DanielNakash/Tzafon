# Tzafon release rules.

# M9b — Room entities are serialized to/from Firestore by field NAME via Gson.
# R8 must not rename or strip their fields, or synced documents corrupt.
-keep class com.thefoxworks.tzafon.data.db.*Entity { *; }
-keepclassmembers class com.thefoxworks.tzafon.data.db.*Entity { <fields>; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Gson type machinery (safe alongside Gson's bundled consumer rules).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# Firebase, Credential Manager and googleid ship their own consumer rules.
