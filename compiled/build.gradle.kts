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

    if (file("../build/example.jar").exists()) {
        implementation(files("../build/example.jar"))
    }

    implementation(project(":pylib"))
}

tasks.test {
    useJUnitPlatform()
}