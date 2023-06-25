-optimizationpasses 10
-optimizations *
-dontobfuscate
-allowaccessmodification
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

-dontwarn org.apache.**
-dontwarn asteroid_libs.**
-dontwarn java.lang.invoke.MethodHandle

-keep class asteroid_libs.org.eclipse.aether.**, asteroid_libs.org.apache.maven.** {
    *;
}

-keepparameternames -keep class org.inksnow.asteroid.** {
    *;
}

-keep enum ** {
    *;
}