# Z80Core
#### A Z80 core written in Java.

This core is a slightly modified core extracted from [JSpeccy](https://github.com/jsanchezv/JSpeccy), ready to be used at any emulator that needs a Z80 CPU.

This code emulates the Z80 CPU full instruction set including:

* Undocumented instructions as SLL.
* Undocumented flags 3 & 5 from F register.
* Hidden register MEMPTR (known as WZ in official Zilog documentation).
* Strict execution order for every instruction.
* Precise timing for all instructions, totally decoupled from the core

An example that executes the CP/M v2.2 tests ZEXALL & ZEXDOC is included as an use case.
