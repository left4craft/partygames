plugins {
    java
    alias(libs.plugins.shadow)
}

group = "me.sisko"
version = "2.0.0"
description = "Left4Craft's minigame rotation: TNT run, spleef, quake, KOTH and friends"

java {
    // Paper 26.2's API class files are major version 69 (Java 25), so the
    // compiler has to be at least 25 to read them.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

dependencies {
    // Provided by the server. Also puts Adventure, Gson and Guava on the
    // compile classpath, none of which get shaded.
    compileOnly(libs.paper.api)

    // Exposes %partygames_*% placeholders to other plugins. Optional at
    // runtime; the expansion only registers when PlaceholderAPI is present.
    compileOnly(libs.placeholderapi)

    implementation(libs.json)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.shadowJar {
    archiveClassifier = ""

    // Several plugins on this network ship JSON parsers, connection pools and
    // Postgres drivers of their own. Keeping ours under our package stops them
    // fighting over the classpath.
    listOf(
        "org.json",
        "com.zaxxer.hikari",
        "org.postgresql",
    ).forEach { relocate(it, "me.sisko.partygames.lib.$it") }

    dependencies {
        // Provided by Paper.
        exclude(dependency("org.slf4j:.*"))
        // Compile-time-only annotation stubs pulled in transitively.
        exclude(dependency("org.checkerframework:.*"))
    }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/maven/**")

    // The service-file transformer has to see every copy of
    // META-INF/services/java.sql.Driver before it can merge them.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    // Only the shaded jar is installable; keep the thin one out of the way.
    archiveClassifier = "thin"
}
