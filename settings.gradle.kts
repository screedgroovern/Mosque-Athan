pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // PREFER_SETTINGS rather than FAIL_ON_PROJECT_REPOS, and only because of Kotlin/Wasm:
    // the browser toolchain adds its own Node and Yarn download repositories from inside
    // the plugin, which FAIL_ON_PROJECT_REPOS rejects outright — `wasmJsBrowserDistribution`
    // cannot run under it. The repositories below are still the ones that get consulted
    // first, so the hygiene this setting exists for is kept; what is given up is the
    // guarantee that *nothing* else can be added.
    repositoriesMode.set(
        RepositoriesMode.PREFER_SETTINGS
    )

    repositories {
        google()
        mavenCentral()

        // The Kotlin/Wasm toolchain downloads Node and Yarn itself, and with
        // FAIL_ON_PROJECT_REPOS above it is not allowed to add the repositories for them.
        // Declaring them here is what makes `wasmJsBrowserDistribution` runnable at all;
        // the content filters keep these two from being consulted for anything else.
        ivy("https://nodejs.org/dist/") {
            name = "Node Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        // wasm-opt, which the production wasm build runs over the binary.
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "Binaryen Distributions"
            patternLayout { artifact("version_[revision]/[module]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

rootProject.name = "MosqueAthan"
include(":app")
include(":core:model")
include(":core:prayer")
include(":core:protocol")
include(":ui:design")
include(":ui:display")
include(":ui:settings")
include(":webApp")
