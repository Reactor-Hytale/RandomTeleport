plugins {
    id("java")
}

group = "codes.reactor"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://reactor-hytale.github.io/maven")
    }
    maven {
        name = "hytale-pre-release"
        url = uri("https://maven.hytale.com/pre-release")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    compileOnly("org.jetbrains:annotations:26.0.2-1")

    compileOnly("com.hypixel.hytale:Server:2026.01.22-6f8bdbdc4")

    compileOnly("codes.reactor:reactor-sdk:1.1.0")
}


tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName = "rxRTP"
}

allprojects {
    apply<JavaPlugin>()

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
}