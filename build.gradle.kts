plugins {
    id("no.elhub.devxp.kotlin-application") version "0.0.12"
}

description = "Retrieve SonarScan results from Sonarqube and post them to Phabricator."

dependencies {
    //implementation(platform(libs.elhub.devxp.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.logging.slf4j.api)
    implementation(libs.logging.slf4j.simple)
    implementation("info.picocli:picocli:4.7.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.json:json:20220924")
    implementation(platform(libs.serialization.jackson.bom))
    implementation(libs.serialization.jackson.core)
    implementation(libs.serialization.jackson.databind)
    testImplementation(libs.test.kotest.runner.junit5)
    testImplementation(libs.test.kotest.assertions.core)
    testImplementation("io.mockk:mockk:1.13.3")
}

val applicationMainClass : String by project

application {
    mainClass.set(applicationMainClass)
}

val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
}

tasks["assemble"].dependsOn(tasks["shadowJar"])
