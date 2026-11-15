# R8 for the release build: the libraries (Compose, Glance, coroutines, Health Connect...) are optimized and shrunk,
# which is most of the speed. The app's own code is kept as it is, and nothing is renamed, so saved designs
# (kotlinx.serialization), widget receivers, workers and crash logs all work as written.
-dontobfuscate
-keep class com.sidequest.widgetmaker.** { *; }

# Claude's SDK reads and writes its JSON with Jackson, by reflection: keep them whole.
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions

# Referenced by the SDK's libraries but not on Android (desktop Java APIs, annotation processors): never called here.
-dontwarn com.google.auto.value.AutoValue
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn java.lang.reflect.AnnotatedType

# The SDK's JSON schema generator also works by reflection.
-keep class com.github.victools.** { *; }
