package z80exerciser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import z80core.MemIoOps;
import z80core.NotifyOps;
import z80core.Z80;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

@SpringBootApplication
@Slf4j
public class Z80Exerciser implements CommandLineRunner, NotifyOps {

    private final Z80 z80;
    private final MemIoOps memIo;
    private final byte[] z80Ram = new byte[0x10000];
    private boolean finish;
    private StringBuffer buffer;

    public Z80Exerciser() {
        finish = false;
        buffer = new StringBuffer();
        memIo = new MemIoOps(0, 0);
        memIo.setRam(z80Ram);
        z80 = new Z80(memIo, this);
    }

    @Override
    public void run(String... args) {
        final long start = System.currentTimeMillis();

        String testName = "zexall.bin";
        try (BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(ZexallBin.data))) {
            int count = in.read(z80Ram, 0x100, 0xFF00);
            log.info("Read {} bytes from {}", count, testName);

            z80.reset();
            memIo.reset();
            finish = false;

            z80Ram[0] = (byte) 0xC3;
            z80Ram[1] = 0x00;
            z80Ram[2] = 0x01;           // JP 0x100 CP/M TPA
            z80Ram[5] = (byte) 0xC9;    // Return from BDOS call

            log.info("Starting test {}", testName);
            z80.setBreakpoint(0x0005, true);
            while (!finish) {
                z80.execute();
            }
            log.info("Test {} completed", testName);
        } catch (Exception e) {
            log.error("Exception encountered whilst attempting to exercise the Z80 CPU core", e);
        }
        log.info("");
        log.info("Test '{}' was executed in {} ms", testName, System.currentTimeMillis() - start);
    }

    @Override
    public int breakpoint(final int address, final int opcode) {
        // Emulate CP/M Syscall at address 5
        switch (z80.getRegC()) {
            case 0 -> {
                // BDOS 0 System Reset
                log.info("Z80 reset after {} t-states", memIo.getTstates());
                finish = true;
            }
            case 2 -> log.info("{}", (char) z80.getRegE()); // BDOS 2 console char output
            case 9 -> {
                // BDOS 9 console string output (string terminated by "$")
                int strAddr = z80.getRegDE();
                while (z80Ram[strAddr] != '$') {
                    if (z80Ram[strAddr] == 0x0A) {
                        // Ignore line feed
                        strAddr++;
                    } else if (z80Ram[strAddr] == 0x0D) {
                        // Display the message and reset the buffer when a carriage return is detected
                        // TODO: capture the test outcome and fail the test if any of the outcomes was a failure.
                        log.info("{}", buffer);
                        buffer = new StringBuffer();
                        strAddr++;
                    } else {
                        // Append characters to the message
                        buffer.append((char) z80Ram[strAddr++]);
                    }
                }
            }
            default -> {
                log.info("BDOS Call {}", z80.getRegC());
                finish = true;
            }
        }
        return opcode;
    }

    @Override
    public void execDone() {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    public static void main(String[] args) {

        new SpringApplicationBuilder(Z80Exerciser.class).headless(true).run(args);
    }

}
