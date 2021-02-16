import edu.sc.seis.launch4j.tasks.DefaultLaunch4jTask
import com.adarshr.gradle.testlogger.theme.ThemeType
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import groovy.lang.GroovyObject

plugins {
    kotlin("jvm") version "1.4.21-2"
    id("edu.sc.seis.launch4j") version "2.4.6"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("jacoco")
    id("com.adarshr.test-logger") version "2.1.1"
    id("io.qameta.allure") version "2.8.1"
    id("com.jfrog.artifactory") version "4.18.3"
    id("maven-publish") apply true
}

val allureVersion = "2.13.8"
val kotestVersion = "4.3.2"
val mavenPubName = "mavenExecutable"

group = "no.elhub.dev.tools"
description = "Retrieve SonarScan results from Sonarqube and post them to Phabricator."
val mainClassName = "no.elhub.dev.tools.SonarPhabricatorRunner"

repositories {
    maven("https://jfrog.elhub.cloud/artifactory/elhub-mvn")
}

dependencies {
    implementation("info.picocli:picocli:4.+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21-2")
    implementation("org.apache.httpcomponents:httpclient:4.3.6")
    implementation("org.json:json:20200518")
    implementation("commons-io:commons-io:2.6")
    implementation("com.fasterxml.jackson.core:jackson-core:2.11.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-allure-jvm:$kotestVersion")
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    //sourceSets["main"].java {
    //    srcDir("build/resources/test")
    //}
    //sourceSets["test"].java {
    //    srcDir("build/resources/test")
    //}
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        javaParameters = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

}

jacoco {
    toolVersion = "0.8.4" // Has to be the same as TeamCity
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

testlogger {
    theme = ThemeType.MOCHA
}

allure {
    version = allureVersion
    autoconfigure = false
    aspectjweaver = true
    useJUnit5 {
        version = allureVersion
    }
    downloadLink = "https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline/" +
            "$allureVersion/allure-commandline-$allureVersion.zip"
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set(rootProject.name)
    manifest {
        attributes["Implementation-Title"] = rootProject.name
        attributes["Implementation-Version"] = rootProject.version
        attributes["Main-Class"] = mainClassName
    }
    from(configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    with(tasks.jar.get() as CopySpec)
}

tasks.withType<DefaultLaunch4jTask> {
    outfile = "${rootProject.name}.exe"
    headerType = "console"
    mainClassName = mainClassName
    productName = rootProject.description
}

artifacts {
    add("archives", File("build/launch4j/${rootProject.name}.exe"))
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r|-jre)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

publishing {
    publications {
        create<MavenPublication>(mavenPubName) {
            from(components["java"])
        }
    }
}

fun Project.artifactory(configure: ArtifactoryPluginConvention.() -> Unit): Unit =
    configure(project.convention.getPluginByName<ArtifactoryPluginConvention>("artifactory"))

artifactory {
    publish(delegateClosureOf<PublisherConfig> {
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", mavenPubName)
            setProperty("publishArtifacts", true)
            setProperty("publishPom", true)
        })
    })
}

