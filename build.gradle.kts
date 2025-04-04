plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.0.0-beta11"
}
kotlin { jvmToolchain(21) }
repositories { mavenCentral() }
dependencies {
//    testImplementation(kotlin("test"))
}


//tasks.test { useJUnitPlatform() }
tasks.jar {
    manifest { attributes["Main-Class"] = "MainKt" }
}
