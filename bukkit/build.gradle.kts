import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":")){
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
        exclude(group = "org.apache.commons", module = "commons-lang3")
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation("org.slf4j:slf4j-api:1.7.36")


    // lombok
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    compileOnly("org.bukkit:bukkit:1.12.2-R0.1-SNAPSHOT")
}


tasks.shadowJar {
    exclude("about.html", "licenses/**", "plugin.xml", "META-INF/**")
    arrayOf(
        "javax.inject", "org.apache.maven", "org.codehaus", "org.eclipse", "org.sonatype", "org.slf4j",
        "org.objectweb.asm", "com.hrakaroo.glob"
    ).forEach { relocate(it, "asteroid_libs.$it") }
}

tasks.create<ProGuardTask>("proguard") {
    dependsOn(tasks.shadowJar)
    injars(tasks.shadowJar)
    outputs.file("build/tmp/proguard-opt.jar")
    outjars("build/tmp/proguard-opt.jar")
    configuration("configuration.pro")
    libraryjars(configurations.compileClasspath)
    libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
    libraryjars("${System.getProperty("java.home")}/jmods/java.base.jmod")
    libraryjars("${System.getProperty("java.home")}/jmods/java.invoke.jmod")
    libraryjars("${System.getProperty("java.home")}/jmods/java.logging.jmod")
    libraryjars("${System.getProperty("java.home")}/jmods/jdk.unsupported.jmod")
}

tasks.create<ShadowJar>("proguardJar") {
    from(tasks["proguard"])

    manifest {
        attributes["Main-Class"] = "org.inksnow.aloader.TestMain"
    }

    archiveClassifier.set("opt")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
    dependsOn(tasks["proguardJar"])
}