import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.gradleup.shadow") version "9.0.0-beta11"
}
kotlin { jvmToolchain(21) }
repositories { mavenCentral() }
dependencies {
    testImplementation(kotlin("test"))
}

group = "com.joelburton.mothlang"
version = "1.0"


tasks.test { useJUnitPlatform() }
tasks.jar {
    manifest { attributes["Main-Class"] = "MainKt" }
}
dokka {
    dokkaSourceSets.main {
        documentedVisibilities(
            VisibilityModifier.Private,
            VisibilityModifier.Protected,
            VisibilityModifier.Package,
            VisibilityModifier.Public)
    }
}
