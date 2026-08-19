plugins {
    kotlin("js") version "2.0.21"
}

group = "wonder.drawer"
version = "2.0.0"

repositories { mavenCentral() }

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val main by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}

tasks.register<Copy>("publishRuntime") {
    dependsOn("browserProductionWebpack")
    from(layout.buildDirectory.dir("distributions"))
    include("*.js")
    rename { "runtime.js" }
    into(layout.projectDirectory)
}
