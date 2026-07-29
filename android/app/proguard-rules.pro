# Tzafon release rules.

# M9b — Room entities are serialized to/from Firestore by field NAME via Gson.
# R8 must not rename or strip their fields, or synced documents corrupt.
-keep class com.thefoxworks.tzafon.data.db.*Entity { *; }
-keepclassmembers class com.thefoxworks.tzafon.data.db.*Entity { <fields>; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# DM-EXPORT-1 (v2.11.0 FR-DATA-3) — the export/import path serializes domain
# models by field name via Gson (DataExporter). R8 must not rename or strip
# their fields, or the exported JSON loses the descriptive keys DM-EXPORT-1
# promised and reads back as {"a":…,"b":…,"c":…}.
-keep class com.thefoxworks.tzafon.domain.model.** { *; }
-keepclassmembers class com.thefoxworks.tzafon.domain.model.** { <fields>; }
-keep class com.thefoxworks.tzafon.domain.recurrence.** { *; }
-keepclassmembers class com.thefoxworks.tzafon.domain.recurrence.** { <fields>; }
-keep class com.thefoxworks.tzafon.data.transfer.Export** { *; }
-keepclassmembers class com.thefoxworks.tzafon.data.transfer.Export** { <fields>; }

# Gson type machinery (safe alongside Gson's bundled consumer rules).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# Firebase, Credential Manager and googleid ship their own consumer rules.
