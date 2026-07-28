import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9+ has built-in Kotlin support, so the `org.jetbrains.kotlin.android` plugin
    // is not applied (it would error). The `kotlin { }` DSL below comes from AGP.
    id("com.android.library") version "9.3.1"
    id("maven-publish")
}

// Coordinates. On JitPack the group is overridden to `com.github.readmio` and the artifact to the
// repo name, so consumers use `com.github.readmio:miohyphen:<tag>`. `version` comes from the git tag.
group = "com.github.readmio"
version = "0.1.0"

// Toolchain aligned with the Readmio app (AGP 9.3.1 / Kotlin 2.4.0 / JDK 17), so this library can
// be used as an `includeBuild(...)` composite build from that project.
android {
    namespace = "com.readmio.miohyphen"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Expose a `release` variant with sources, so consumers (and JitPack) get a proper AAR.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // MioHyphen is a self-contained pure-Kotlin Liang implementation — no runtime dependencies.
    // JUnit is test-only (not part of the published AAR). The BOM keeps jupiter + launcher aligned.
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Gradle no longer adds the JUnit Platform launcher automatically.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "miohyphen"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("MioHyphen")
                description.set(
                    "Offline, dependency-free Kotlin/Android word hyphenation using LibreOffice " +
                        "(hunspell libhyphen) TeX patterns. Slovak, Czech, Polish, English, German " +
                        "(compound), Spanish, Russian, Ukrainian, Portuguese, Italian and more."
                )
                url.set("https://github.com/readmio/miohyphen")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("readmio")
                        name.set("Readmio")
                    }
                }
                scm {
                    url.set("https://github.com/readmio/miohyphen")
                    connection.set("scm:git:https://github.com/readmio/miohyphen.git")
                }
            }
        }
    }
}
