plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.inksnow.aloader"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // lombok
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    testCompileOnly("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")

    implementation("org.apache.maven:maven-repository-metadata:4.0.0-alpha-5")
    implementation("org.apache.maven:maven-model:4.0.0-alpha-5"){
        exclude("org.apache.maven", "maven-api-meta")
        exclude("javax.annotation", "javax.annotation-api")
    }

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    // relocate("io.papermc.lib", "org.inksnow.ankh.core.libs.paperlib")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}