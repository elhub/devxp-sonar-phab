plugins {
    id("no.elhub.devxp.kotlin-application") version "0.1.0"
}

description = "Retrieve SonarScan results from Sonarqube and post them to Phabricator."

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.logging.slf4j.api)
    implementation(libs.logging.slf4j.simple)
    implementation(libs.cli.picocli)
    implementation(libs.apache.commons.io)
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.json:json:20220924")
    implementation(platform(libs.serialization.jackson.bom))
    implementation(libs.serialization.jackson.core)
    implementation(libs.serialization.jackson.databind)
    testImplementation(libs.test.kotest.runner.junit5)
    testImplementation(libs.test.kotest.assertions.core)
    testImplementation(libs.test.mockk)
}

val applicationMainClass: String by project

application {
    mainClass.set(applicationMainClass)
}

val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
}
listOf(tasks["distZip"], tasks["distTar"]).forEach {
    it.dependsOn(tasks["shadowJar"])
}
tasks["startScripts"].dependsOn(tasks["shadowJar"])
tasks["startShadowScripts"].dependsOn(tasks["jar"])

tasks["assemble"].dependsOn(tasks["shadowJar"])
