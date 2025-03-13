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
    implementation(libs.annotations)
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8