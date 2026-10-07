# Dependency provenance

Runtime direct dependencies: Kotlin stdlib, kotlinx.coroutines, AndroidX Activity, Lifecycle, DocumentFile, Compose UI and Material 3/icons. These projects use Apache-2.0. Java ZIP, SHA-256, BitmapFactory and VideoView are platform APIs; no ZIP, hash, image loader or USB GPL library is bundled.

The Expressive theme uses explicitly pinned Material 3 1.5.0-alpha09 with stable Compose BOM 2025.11.01. Its AndroidX graphics-shapes dependency is Apache-2.0. Glass panels and the backdrop are drawn locally with Compose; no third-party glass effect package or reference-image assets are bundled.

Android Gradle Plugin and Gradle are build-time dependencies. JUnit 4.13.2 (EPL-1.0), Hamcrest (BSD-3-Clause) and Robolectric (Apache-2.0) are test-only. The application does not package test dependencies.

Review the complete resolved transitive dependency graph and upstream NOTICE files before a commercial release:

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

Repository code and the launcher/widget vectors were created for this project. The layout uses general file-manager conventions and Material components; no Amaze/Material Files source, UI assets or screenshots were consulted or copied.
