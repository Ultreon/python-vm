plugins {
    id("java")
    id("groovy")
    application
}

group = "dev.ultreon.pythonvm"
version = "1.0.0"

base {
    archivesName.set("python-vm")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.apache.groovy:groovy:4.0.23")
    implementation("org.apache.groovy:groovy-jsr223:4.0.23")
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "groovy")

    dependencies {
        implementation("org.apache.groovy:groovy:4.0.23")
        implementation("org.apache.groovy:groovy-jsr223:4.0.23")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("compilePython") {
    dependsOn(":compilePyLib")

    doFirst {
        delete(file("build/libs/example-1.0.jar"))
        delete(file("build/tmp/compilePython/"))
    }
    finalizedBy(":testing:compileJava")
    classpath = project(":compiler").sourceSets["main"].runtimeClasspath
    mainClass.set("dev.ultreon.pythonc.App")
    args = listOf("-j", file("build/libs/example-1.0.jar").path, "-o", file("build/classes/java/main/").path, file("src/main/python").path, file("src/main/resources").path)

    group = "python-vm"
    inputs.files("src/main/python", "src/main/resources", "build.gradle.kts", "build/tmp/compilePython")

    notCompatibleWithConfigurationCache("Dynamically compiles python")

    outputs.file("build/libs/example-1.0.jar")
    outputs.dir("build/tmp/compilePython")
}

tasks.register<JavaExec>("compilePyLib") {
    doFirst {
        delete(file("build/libs/pylib-1.0.jar"))
        delete(file("build/tmp/compilePyLib/"))
    }
    finalizedBy(":testing:compileJava")
    classpath = project(":compiler").sourceSets["main"].runtimeClasspath
    mainClass.set("dev.ultreon.pythonc.App")
    args = listOf("-j", file("build/libs/pylib-1.0.jar").path, "-o", file("build/pylib/classes/").path, file("pylib/src/main/python").path)

    group = "python-vm"
    inputs.files("pylib/src/main/python", "pylib/src/main/resources", "build.gradle.kts", "build/tmp/compilePyLib")

    notCompatibleWithConfigurationCache("Dynamically compiles python")

    outputs.file("build/libs/pylib-1.0.jar")
    outputs.dir("build/tmp/compilePyLib")
}

tasks.register<Jar>("jarPython") {
    dependsOn("compilePython")
    finalizedBy(":testing:compileJava")
    archiveClassifier.set("dist")

    inputs.files(file("build/libs/example-1.0.jar"))

    group = "python-vm"
    from(zipTree("build/libs/example-1.0.jar"))
    from(project(":pylib").sourceSets["main"].output)
}

tasks.jar {
    dependsOn("compilePython")
    finalizedBy(":testing:compileJava", "sourcesJar")

    inputs.files(file("build/libs/example-1.0.jar"))
    inputs.files(file("build/libs/pylib-1.0.jar"))
    inputs.files(file("pylib/build/libs/pylib-1.0.jar"))

    group = "python-vm"
    from(zipTree("build/libs/example-1.0.jar"))
    from(zipTree("build/libs/pylib-1.0.jar"))
    from(zipTree("pylib/build/libs/pylib-1.0.jar"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Jar>("sourcesJar") {
    dependsOn("compilePython")
    finalizedBy(":testing:compileJava")

    inputs.files(file("testing/src/main/python"))

    group = "python-vm"
    from("src/main/python")
    archiveClassifier.set("sources")
}

tasks.register<JavaExec>("runPython") {
    dependsOn("compilePython")

    classpath = project(":testing").sourceSets["main"].runtimeClasspath
    mainClass.set("dev.ultreon.test.Main")
    args = listOf("Hello World")

    group = "python-vm"
}
