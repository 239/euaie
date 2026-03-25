# euaie

- tries to minimize complexity of application and codebase
- plain but effective string-based filter syntax (no glob or regex)
- minimal and easy configuration
- ...

![](web/main.png)
![](web/help.png)

### help

```
Usage: euaie [-hISVx] [-C=<threshold>] [-s=<symlinks>] [-t=<tolerance>]
             [-e[=<exclude>...]]... [-i[=<include>...]]... [@<filename>...]
             <rootL> <rootR>
simple file synchronization
      [@<filename>...]       One or more argument files containing options.
      <rootL>
      <rootR>
  -e, --exclude[=<exclude>...]

  -i, --include[=<include>...]

  -s, --symlinks=<symlinks>  set policy for symbolic links
                             FOLLOW, IGNORE, PRESERVE
  -t, --tolerance=<tolerance>
                             set allowed time difference (ms)
  -x, --exit-when-done       exit when both sides are equal
  -C, --copy-threshold=<threshold>
                             set threshold for interruptable copy mode (MiB)
  -I, --ignore-filter-case   use case insensitive filters
  -S, --stateless            ignore previous state
  -V, --version              print version and exit
```

### related recommendations

- [Unison](https://github.com/bcpierce00/unison)
- [Syncthing](https://github.com/syncthing/syncthing)
- [FreeFileSync](https://freefilesync.org/)
