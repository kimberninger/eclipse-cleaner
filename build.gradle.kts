plugins {
  application
  id("com.github.johnrengelman.shadow").version("6.1.0")
}

group = "fop"
version = "2.2.1-Snapshot"

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.google.code.gson:gson:2.8.6")
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

application {
  mainClassName = "fop.project_cleaner.ui.MainGui"
}
