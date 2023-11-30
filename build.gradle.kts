plugins {
    kotlin("multiplatform") version "1.9.21"
}

repositories {
    mavenCentral()
}

kotlin {
    mingwX64("native") {
        binaries {
            executable {
                // prevent terminal from opening
                linkerOpts("-mwindows")
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }
}