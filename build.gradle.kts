import com.adarshr.gradle.testlogger.theme.ThemeType
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

plugins {
    kotlin("jvm") version "1.4.31"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("jacoco")
    id("com.adarshr.test-logger") version "2.1.1"
    id("io.qameta.allure") version "2.8.1"
    id("com.jfrog.artifactory") version "4.18.3"
    id("maven-publish") apply true
}

val allureVersion = "2.13.8"
val kotestVersion = "4.3.2"
val jacksonVersion = "2.11.3"
val mavenPubName = "mavenJavaBinary"

description = "Retrieve SonarScan results from Sonarqube and post them to Phabricator."
val mainClassName = "no.elhub.tools.sonarphab.SonarPhabricatorKt"

repositories {
    maven("https://jfrog.elhub.cloud/artifactory/elhub-mvn")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.httpcomponents:httpclient:4.3.6")
    implementation("info.picocli:picocli:4.6.1")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("commons-io:commons-io:2.8.0")
    implementation("org.json:json:20200518")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-allure-jvm:$kotestVersion")
}

/*
 * Compile setup
 */
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        javaParameters = true
    }
}

/*
 * Test setup
 */
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

/*
 * Application Code
 * - Create a fat jar for deployment
 * - Run it after the jar command and as part of the assemble task
 */
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
    mustRunAfter(tasks.get("jar"))
}

tasks.get("assemble").dependsOn(tasks.get("fatJar"))

publishing {
    publications {
        create<MavenPublication>(mavenPubName) {
            from(components["java"])
        }
    }
}

artifactory {
    setContextUrl("https://jfrog.elhub.cloud/artifactory")
    publish(delegateClosureOf<PublisherConfig> {
        repository(delegateClosureOf<groovy.lang.GroovyObject> {
            setProperty("repoKey", project.findProperty("binaryrepo") ?: "elhub-bin-test-local")
            setProperty("username", project.findProperty("mavenuser") ?: "nouser")
            setProperty("password", project.findProperty("mavenpass") ?: "nopass")
        })
        defaults(delegateClosureOf<groovy.lang.GroovyObject> {
            invokeMethod("publications", mavenPubName)
            setProperty("publishArtifacts", true)
            setProperty("publishPom", false)
        })
    })
    resolve(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig> {
        setProperty("repoKey", "repo")
    })
}

tasks.get("artifactoryPublish").dependsOn(tasks.get("assemble"))

tasks.get("publish").dependsOn(tasks.get("artifactoryPublish"))
