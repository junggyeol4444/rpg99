plugins {
    `java-library`
}

allprojects {
    group = "kr.reborn"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    dependencies {
        val implementation by configurations
        val compileOnly by configurations
        compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
        compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
        compileOnly("com.zaxxer:HikariCP:5.1.0")
        compileOnly("mysql:mysql-connector-java:8.0.33")
        compileOnly("org.jetbrains:annotations:24.1.0")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) { into("META-INF/") }.onlyIf { rootProject.file("LICENSE").exists() }
    }
}
