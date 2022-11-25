plugins {
  @Suppress("DSL_SCOPE_VIOLATION")
  alias(libs.plugins.kotlin.multiplatform)
}

group = "network.obrien"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  val nativeTarget = when (System.getProperty("os.name")) {
    "Mac OS X" -> when (System.getProperty("os.arch")) {
      in listOf("aarch64", "arm-v8", "arm64") -> macosArm64("native")
      in listOf("x86-64", "x86_64", "amd64", "x64") -> macosX64("native")
      else -> throw GradleException("Host architecture is not supported.")
    }
    "Linux" -> linuxX64("native")
    else -> throw GradleException("Host OS is not supported.")
  }

  nativeTarget.apply {
    binaries {
      executable {
        entryPoint = "main"
      }
    }
  }

  sourceSets {
    val nativeMain by getting {
      dependencies {
        implementation(libs.clikt)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.engine)
      }
    }

    val nativeTest by getting
  }
}
