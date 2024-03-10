plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "supa.dupa"
version = "0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    maven {
        name = "Sonatype Snapshots (Legacy)"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("commons-io:commons-io:2.13.0")
    implementation("dev.kord:kord-core:0.13.1")
    implementation("dev.kord:kord-voice:0.10.0")
    implementation("dev.kord:kord-core-voice:0.10.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("dev.arbjerg:lavaplayer:2.0.1")
    implementation("org.json:json:20230618")
    implementation("io.insert-koin:koin-core:3.5.0")
    implementation(libs.kord.extensions)
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass.set("supa.duap.MainKt")
}


tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "supa.duap.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
