recipes:
    just -l

build-jar:
    ./gradlew shadowJar

build-native-image-gradle:
    ./gradlew nativeCompile

[linux]
build-native-image-podman: build-jar
    podman run --rm --volume ./_tui/build/jar:/app \
        ghcr.io/graalvm/native-image-community:25 -jar euaie.jar

[linux]
build-native-image-podman-static-nolibc: build-jar
    podman run --rm --volume ./_tui/build/jar:/app \
        ghcr.io/graalvm/native-image-community:25 --static-nolibc -jar euaie.jar

# ⚠️ broken UI and poor performance ⚠️
[linux]
build-native-image-podman-static-musl: build-jar
    podman run --rm --volume ./_tui/build/jar:/app \
        ghcr.io/graalvm/native-image-community:25-muslib --static --libc=musl -jar euaie.jar

clean:
    ./gradlew clean

native-image-agent +arguments: build-jar
    java -agentlib:native-image-agent=config-merge-dir=_tui/src/main/resources/META-INF/native-image/euaie/ \
        -jar _tui/build/jar/euaie.jar {{ arguments }}

upx:
    upx _tui/build/native/nativeCompile/*
