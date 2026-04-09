recipes:
    just -l

[linux]
[no-cd]
path:
    echo $PATH

agent +arguments:
    java -agentlib:native-image-agent=config-merge-dir=_tui/src/main/resources/META-INF/native-image/euaie/ \
        -jar _tui/build/jar/euaie.jar {{ arguments }}
