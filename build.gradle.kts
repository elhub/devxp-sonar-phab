description = "Retrieve SonarScan results from Sonarqube and post them to Phabricator."

val kotestVersion = "4.3.2"
val jacksonVersion = "2.11.3"

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
