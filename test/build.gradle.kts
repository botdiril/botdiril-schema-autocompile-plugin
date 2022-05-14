import com.botdiril.framework.util.BuildSchemasTask

plugins {
    java
    id("com.botdiril.botdiril-schema-autocompile-plugin")
}

group = "com.botdiril"
version = "0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.3"
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "BotdirilVega"
        url = uri("https://vega.botdiril.com")
    }
}

sourceSets {
    java {
        main {
            compileClasspath += files("$buildDir/generated/botdiril-sql")
        }

        create("test-model-botdiril") {
            java.srcDirs("test-model-botdiril/java")

            val mainSet = sourceSets.main.get()
            compileClasspath += mainSet.compileClasspath + mainSet.runtimeClasspath + mainSet.output
        }
    }
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()

        maven {
            name = "BotdirilVega"
            url = uri("https://vega.botdiril.com")
        }
    }
}

tasks.withType<BuildSchemasTask> {
    addSourceSet(sourceSets["test-model-botdiril"])
}

dependencies {
    implementation("com.botdiril", "botdiril-sql-framework", "0.2.3")

    testImplementation("junit:junit:4.13.2")
}
