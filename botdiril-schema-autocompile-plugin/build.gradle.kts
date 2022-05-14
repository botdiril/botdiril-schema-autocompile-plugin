plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "com.botdiril"
version = "0.1.1"

gradlePlugin {
    plugins.create("autoCompiler") {
        id = "com.botdiril.botdiril-schema-autocompile-plugin"
        implementationClass = "com.botdiril.framework.util.AutoCompilePlugin"
    }
}

tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.4.2"
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
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
        create("test-model-botdiril") {
            java.srcDirs("test-model-botdiril/java")

            val mainSet = sourceSets.main.get()
            compileClasspath += mainSet.compileClasspath + mainSet.output
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()

        maven {
            name = "Vega"
            url = uri("https://vega.botdiril.com/")
            credentials {
                val vegaUsername: String? by project
                val vegaPassword: String? by project

                username = vegaUsername
                password = vegaPassword
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
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

dependencies {
    implementation("org.apache.commons", "commons-lang3", "3.11")

    testImplementation("junit:junit:4.13.2")
}
