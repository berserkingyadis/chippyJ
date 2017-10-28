# chippyJ

My first emulation project: An Emulator for the Chip-8 interpreter written in Java.

## Progress
- Initiaization of the CPU/Memory/Display: done.
- Initialization of Font-Data: done.
- Loading ROMs: done.
- Implementing Opcodes: done.
- Tests for all Opcodes: 97% done.
- Input: in progress.
- Sound: in progress.
- Bugfixing: in progress.


##  Building
You will need Maven and Java 8 JDK. ```mvn package``` in the root directory suffices.

## Running
You wil need the Java 8 JRE. All internal dependencies will be bundled into a runnable JAR-File
```java -jar JChip-8.jar```
## Roms
Some roms can be downloaded here: http://www.chip8.com/?page=109
## Reference
My implementation is mainly based off these 2 Documentations of Chip-8:
- http://mattmik.com/files/chip8/mastering/chip8.html
- http://devernay.free.fr/hacks/chip8/C8TECH10.HTM

I got some inspiration of this project while developing:
- https://github.com/craigthomas/Chip8Java

Also this guy:
- https://www.youtube.com/watch?v=rpLoS7B6T94



