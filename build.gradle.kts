import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
  application
  id("com.diffplug.spotless") version "7.2.1"
  // Shadow (fat-JAR packaging) is deferred — will be added when packaging is needed
  id("org.checkerframework") version "0.6.59"
  id("org.openjfx.javafxplugin") version "0.1.0"
  jacoco
  kotlin("jvm") version "2.2.0"
}

// # Custom tasks

// ## runWithJacoco — run app with JaCoCo agent for manual coverage collection
tasks.register<JavaExec>("runWithJacoco") {
  group = "verification"
  description = "Run the app with JaCoCo agent to collect coverage manually"
  mainClass.set(application.mainClass)
  classpath = sourceSets["main"].runtimeClasspath

  doFirst {
    val gitIsClean: Boolean = run {
      val p = ProcessBuilder("git", "status", "--porcelain").redirectErrorStream(true).start()
      val out = p.inputStream.bufferedReader().readText().trim()
      p.waitFor()
      out.isEmpty()
    }
    val gitCommitHash: String = run {
      val p = ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start()
      val out = p.inputStream.bufferedReader().readText().trim()
      p.waitFor()
      out
    }

    val destfile =
        File("./build/jacoco").let { dir ->
          val user = System.getProperty("user.name") ?: "anonymous"
          val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
          val status = if (gitIsClean) "clean" else "dirty"
          File(dir, "${gitCommitHash}-${status}-${user}-${ts}.exec")
        }

    jvmArgs =
        listOf("-javaagent:${jacocoAgentJarFile.get().absolutePath}=destfile=${destfile.toPath()}")
  }
}

val jacocoRuntime by
    configurations.creating {
      isCanBeConsumed = false
      isCanBeResolved = true
      attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }

val jacocoAgentJarFile = provider {
  zipTree(jacocoRuntime.singleFile).matching { include("jacocoagent.jar") }.singleFile
}

// ## quickCheck — fast static checks only (no tests)
tasks.register("quickCheck") {
  description = "Runs only fast static checks like spotlessCheck"
  group = "verification"
  dependsOn("spotlessDiagnose", "spotlessCheck")
}

// ## -PignoreTestFailures — allow jacocoTestReport to run even when tests fail
val ignoreTestFailures: Boolean =
    project.findProperty("ignoreTestFailures")?.toString()?.let {
      when (it) {
        "true" -> true
        "false" -> false
        else ->
            error(
                "Invalid value for -PignoreTestFailures: \"$it\". Use \"true\" or \"false\" only."
            )
      }
    } ?: false

// # Build configuration

repositories { mavenCentral() }

application { mainClass.set("affr.app.AFFrMain") }

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

// FXML and CSS live under src/main/resources/ following standard Gradle/JavaFX layout.
// Monaco Editor assets are copied in by a separate task.
tasks.processResources { dependsOn(copyMonacoIntoResources) }

// NOTE: Update jvmTarget when upgrading the Java toolchain
val jvmTarget = "25"
// NOTE: Update when Kotlin adds JVM 25 target support; JVM 24 bytecode runs on JVM 25 without issue
val kotlinJvmTarget = "24"
val jvmTargetVersion = jvmTarget.toInt()

java {
  toolchain { languageVersion = JavaLanguageVersion.of(jvmTargetVersion) }
  sourceCompatibility = JavaVersion.toVersion(jvmTarget)
  targetCompatibility = JavaVersion.toVersion(jvmTarget)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(
      org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(kotlinJvmTarget)
  )
}

javafx {
  version = "25"
  // javafx.swing is intentionally excluded — VTK is hosted via Python/Trame WebView
  modules("javafx.base", "javafx.graphics", "javafx.controls", "javafx.fxml", "javafx.web")
}

dependencies {
  implementation("info.picocli:picocli:4.7.7")
  implementation("io.reactivex.rxjava2:rxjava:2.2.21")
  implementation("io.reactivex:rxjavafx:2.0.2")
  implementation("net.objecthunter:exp4j:0.4.6")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("com.zaxxer:nuprocess:3.0.0")
  implementation("org.slf4j:slf4j-api:2.0.17")
  implementation("org.slf4j:jul-to-slf4j:2.0.17")
  // NOTE: SSH library — check for security updates at each release
  // https://mvnrepository.com/artifact/com.github.mwiede/jsch
  implementation("com.github.mwiede:jsch:2.28.0")
  runtimeOnly("ch.qos.logback:logback-classic:1.5.32")

  // Checker Framework — compile-time null safety
  annotationProcessor("org.checkerframework:checker:3.51.0")
  implementation("org.checkerframework:checker-qual:3.51.0")

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
  testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
  // TODO: check for an openjfx-monocle release aligned with JavaFX 25
  testImplementation("org.testfx:openjfx-monocle:21.0.2")

  jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.13")
}

// # Monaco Editor (embedded code editor for user subroutines)

val npmInstall by
    tasks.registering(Exec::class) {
      group = "node"
      description = "Install npm dependencies"

      val npmExecutable = if (Os.isFamily(Os.FAMILY_WINDOWS)) "npm.cmd" else "npm"
      commandLine(npmExecutable, "install")

      inputs.file("package.json")
      outputs.dir("node_modules")
    }

val copyMonacoIntoResources by
    tasks.registering(Copy::class) {
      group = "build"
      description = "Copies Monaco Editor assets from node_modules into resources"

      dependsOn(npmInstall)

      from("node_modules/monaco-editor/min/vs")
      into("src/main/resources/affr/app/editor/monaco/vs")
    }

// # Run configuration — inject default CLI args for development runs
//
// Defaults are set at configuration time (config-cache compatible).
// Override with: ./gradlew run --args="--profile=production --tutorial-dir /other/path"
val javaToolchainService = extensions.getByType<JavaToolchainService>()
val toolchainLauncher =
    javaToolchainService.launcherFor { languageVersion = JavaLanguageVersion.of(jvmTargetVersion) }

tasks.withType<JavaExec>().configureEach {
  javaLauncher.set(toolchainLauncher)
  standardOutput = System.out
  errorOutput = System.out
}

tasks.named<JavaExec>("run").configure {
  val tutorialDir = layout.projectDirectory.dir("submodule/case/gui/tutorials/").asFile.absolutePath
  args("--profile=debug", "--tutorial-dir", tutorialDir)
}

// # Code formatting — enforced by Spotless at build time

spotless {
  java { googleJavaFormat("1.28.0") }

  kotlin {
    ktfmt("0.61")
    target("**/*.kt", "**/*.kts")
    targetExclude(".cache/**/*")
  }
}

// # Null safety — Checker Framework NullnessChecker
// ./gradlew nullCheck
// ./gradlew nullCheck -PnullCheckArgs="-Awarns"
val nullCheck by
    tasks.registering(JavaCompile::class) {
      source = sourceSets.main.get().allJava
      classpath = sourceSets.main.get().compileClasspath
      destinationDirectory.set(layout.buildDirectory.dir("nullcheck"))

      options.compilerArgs.addAll(
          listOf(
              "-processor",
              "org.checkerframework.checker.nullness.NullnessChecker",
              "-Astubs=${projectDir}/src/main/astub/javafx.astub",
          )
      )

      val args = project.findProperty("nullCheckArgs") as? String
      if (!args.isNullOrBlank()) {
        options.compilerArgs.addAll(args.split(" "))
      }
    }

// # Test configuration

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
  ignoreFailures = ignoreTestFailures
  // Headless rendering for CI (no display server required).
  // testfx.headless on its own does not select Monocle; the glass/monocle/prism
  // properties below are what actually enable the headless GL pipeline.
  systemProperty("testfx.headless", "true")
  systemProperty("glass.platform", "Monocle")
  systemProperty("monocle.platform", "Headless")
  systemProperty("prism.order", "sw")
  systemProperty("java.awt.headless", "true")

  // openjfx-monocle 21.x reaches into JavaFX-internal Glass packages that
  // JavaFX 25 no longer exports to the unnamed module. Open them explicitly
  // until an openjfx-monocle release aligned with JavaFX 25 is available.
  jvmArgs(
      "--add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED",
      "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
      "--add-exports=javafx.graphics/com.sun.glass.ui.delegate=ALL-UNNAMED",
      "--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
      "--add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED",
      "--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
  )
}

jacoco { toolVersion = "0.8.13" }

tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }

  classDirectories.setFrom(
      files(classDirectories.files.map { fileTree(it) { exclude("**/test/**") } })
  )
}
