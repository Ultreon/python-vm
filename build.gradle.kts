plugins {
    id("java")
    application
}

group = "dev.ultreon.pythonvm"
version = "1.0-SNAPSHOT"

base {
    archivesName.set("python-vm")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("compilePython") {
    doFirst {
        delete(file("build/libs/example-1.0.jar"))
        delete(file("build/pythonc/"))
    }
    finalizedBy(":testing:compileJava")
    classpath = project(":compiler").sourceSets["main"].runtimeClasspath
    mainClass.set("dev.ultreon.pythonc.App")
    args = listOf("-o", file("build/libs/example-1.0.jar").path, file("src/main/python").path, file("src/main/resources").path)

    group = "python-vm"
}

tasks.register<Jar>("jarPython") {
    dependsOn("compilePython")
    finalizedBy(":testing:compileJava")
    archiveClassifier.set("dist")

    inputs.files(file("build/libs/example-1.0.jar"), project(":pylib").sourceSets["main"].output)

    group = "python-vm"
    from(zipTree("build/libs/example-1.0.jar"))
    from(project(":pylib").sourceSets["main"].output)
}

tasks.register<JavaExec>("runPython") {
    dependsOn("compilePython")

    classpath = project(":testing").sourceSets["main"].runtimeClasspath
    mainClass.set("dev.ultreon.test.Main")
    args = listOf("Hello World")

    group = "python-vm"
}
