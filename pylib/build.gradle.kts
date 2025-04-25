plugins {
    id("java")
}

group = "dev.ultreon.pythonvm"
version = "1.0"

base {
    archivesName.set("pylib")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.apache.commons:commons-math3:3.6.1")
}

tasks.test {
    useJUnitPlatform()
}
