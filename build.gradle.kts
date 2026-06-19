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
        // folia-api는 paper-api의 포크로, 동일 패키지의 모든 클래스를 포함하면서
        // Folia 전용 스케줄러 API까지 추가 제공한다. 둘 다 compileOnly로 선언하면
        // 둘 다 동일한 Gradle capability(org.spigotmc:spigot-api)를 선언해
        // 충돌(Cannot select module with conflict on capability)이 발생하므로 하나만 사용.
        compileOnly("dev.folia:folia-api:1.21.4-R0.1-SNAPSHOT")
        compileOnly("com.zaxxer:HikariCP:5.1.0")
        compileOnly("mysql:mysql-connector-java:8.0.33")
        compileOnly("org.jetbrains:annotations:24.1.0")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE")) { into("META-INF/") }.onlyIf { rootProject.file("LICENSE").exists() }
    }
}
