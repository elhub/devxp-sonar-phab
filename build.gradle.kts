plugins {
    id("no.elhub.devxp.kotlin-application") version "0.0.2"
}

description = "Retrieve SonarScan results from Sonarqube and post them to Phabricator."

dependencies {
    implementation(platform(libs.elhub.devxp.bom))
    implementation(platform(libs.jetbrains.kotlin.bom))
    implementation(libs.jetbrains.kotlin.stdlibJdk8)
    implementation(libs.logging.slf4JApi)
    implementation(libs.logging.slf4JSimple)
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("info.picocli:picocli:4.6.3")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.json:json:20220924")
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    testImplementation(libs.test.kotestRunnerJunit5)
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
