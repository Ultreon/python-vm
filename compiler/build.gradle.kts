plugins {
  id("java")
  id("antlr")
}

group = "dev.ultreon"
version = "1.0-SNAPSHOT"

base {
  archivesName.set("pyvm-compiler")
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  implementation("org.jetbrains:annotations:15.0")
  implementation("org.ow2.asm:asm:9.8")
  implementation("org.ow2.asm:asm-tree:9.8")
  implementation("org.ow2.asm:asm-util:9.8")
  implementation("org.ow2.asm:asm-analysis:9.8")

  implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
  implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.3")



  antlr("org.antlr:antlr4:4.13.1")
}

tasks.test {
  useJUnitPlatform()
}

tasks.generateGrammarSource {
  arguments = arguments + listOf("-visitor", "-long-messages", "-package", "dev.ultreon.pyvm.compiler.parser")
  outputDirectory = File("build/generated/sources/antlr/java/main/dev/ultreon/pyvm/compiler/parser")
}

sourceSets {
  main {
    antlr {
      srcDir("src/main/antlr")
    }

    java {
      srcDir("build/generated/sources/antlr/java/main")
    }
  }
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "dev.ultreon.pyvm.compiler.Main"
  }
  from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
