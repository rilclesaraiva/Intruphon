-dontobfuscate
# R8 only runs on release. If something works in debug but breaks in release
# (usually reflection, e.g. serialization), add a -keep rule here; never disable R8.

# NewPipeExtractor: reads some classes by reflection (timeago patterns, protobuf messages)
# and embeds Rhino for JavaScript. Keep it whole rather than guess which parts.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn java.beans.**
-dontwarn com.google.protobuf.**
