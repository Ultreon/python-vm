plugins {
    id("java")
    id("java-library")
}

group = "dev.ultreon"
version = "1.0-SNAPSHOT"

base {
    archivesName.set("pyvm-sdk")
}

repositories {
    mavenCentral()
}

configurations {
    create("dist") {
        isCanBeConsumed = true
        isCanBeResolved = true
        named("implementation").get().extendsFrom(this)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    api("org.jetbrains:annotations:15.0")
    api("net.bytebuddy:byte-buddy:1.14.9")

    api(project(":compiler"))
    configurations["dist"](files("build/pydist/classes"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Exec>("compileSDK") {
    group = "build"
    description = "Compiles the Python SDK"

    inputs.dir("src/main/python")
    inputs.files(fileTree(project(":compiler").tasks.jar.get().archiveFile))
    outputs.dir("build/pydist")

    commandLine("python", "setup.py")
    workingDir = file("src/main/python")
    environment("PYVM_COMPILER", file(project(":compiler").tasks.jar.get().archiveFile))
    environment("PYVM_PARSER", rootProject.file("pyvm/compiler/build/parser.pyz"))
    environment("PYVM_CLASSPATH", sourceSets.main.get().runtimeClasspath.asPath)
    standardOutput = System.out
    errorOutput = System.err

    args(file("src/main/python"), file("build/pydist"), "python")

    dependsOn(":compiler:jar")
    finalizedBy("jar")
    tasks.compileJava.get().dependsOn(this)
}

tasks.jar {
    from(file("main/python/build/dist/classes"))
    from(configurations["dist"])
}
