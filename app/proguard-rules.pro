# libphonenumber loads its metadata through the class loader as resources. R8 must not
# strip or rename those, or number parsing fails at runtime with a MissingMetadata error
# that never shows up in a debug build.
-keep class com.google.i18n.phonenumbers.** { *; }
-keepclassmembers class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**
