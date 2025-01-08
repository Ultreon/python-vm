plugins {
    id("java")
}

group = "dev.ultreon.quantum"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":"))

    implementation(project(":pylib"))
}

tasks.compileJava.get().dependsOn(":compilePython")

tasks.test {
    useJUnitPlatform()
}