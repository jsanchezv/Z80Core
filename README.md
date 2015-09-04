# Z80Core
A Z80 core written in Java
This core is a slightly modified core extracted from JSpeccy, ready to be used at any emulator that needs a Z80 CPU.

This core emulates the Z80 CPU full instruction set including:
- Undocumented instructions as SLL.
- Undocumented flags 3 & 5 from F register.
- Hidden register MEMPTR
- Precise sequencing of each instruction

A test that executes the CP/M v2.2 tests ZEXALL & ZEXDOC is included as a use case.
