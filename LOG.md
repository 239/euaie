## 1.0.0

- defined better symbols for **move** `/` and **neutral direction** `:`
- implemented sorting the list by file names only or by time
- added global directions reset by pressing **Insert/Delete**
- added file data check for move verification (**-P/--paranoid**)
- switched to semantic versioning

## 0.26.9

- fixed annoying zero division exceptions
- fixed exception with empty diff lines
- improved log access between threads
- printing all changes in auto-mode

## 0.26.8

- Kotter updated to 1.3.0 🎉
- applied sticky list index
- fixed filtering with case-insensitive paths
- added build for macOS on Intel
- automatically adjust time tolerance with (ex)FAT

## 0.26.7

- exit option was renamed: **-x/--exit** to **-Q/--quit**
- exact clock shifts of 1 or 2 hours (DST) are ignored when tolerance is > 0
- effective options are shown in help section in a separate tab
- find row and size/index row were merged
- file size values were fixed by removing floating point calculation
- single path without @ prefix is implicitly treated as argument file or directory containing argument files

## 0.26.6

- added testing binaries for **macos-aarch64** and **windows-amd64**
- old release workflows replaced by a single workflow for all platforms
- fixed crash on Windows (#1)
- reworked help section

## 0.26.5

- use grid in overview for scalable alignment
- show diff for single files as a quick file preview
- show rendering performance on demand

## 0.26.4

- compile and release native executable for **linux-amd64**
- add compressed **linux-amd64-upx** variant

## 0.26.3

- keep old files with `-r`/`--retain`
- show default values for options  
