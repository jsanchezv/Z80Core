package z80core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner, NotifyOps {

    private final Z80 z80;
    private final MemIoOps memIo;
    private final byte[] z80Ram = new byte[0x10000];
    private boolean finish = false;

    public Application() {
        memIo = new MemIoOps(0, 0);
        memIo.setRam(z80Ram);
        z80 = new Z80(memIo, this);
    }

    @Override
    public void run(String... args) {
        final long start = System.currentTimeMillis();

        String testName = "zexall.bin";
        runTest(testName);
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
            case 2 -> System.out.print((char) z80.getRegE()); // BDOS 2 console char output
            case 9 -> {
                // BDOS 9 console string output (string terminated by "$")
                int strAddr = z80.getRegDE();
                while (z80Ram[strAddr] != '$') {
                    System.out.print((char) z80Ram[strAddr++]);
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

    private void runTest(final String testName) {
//        try (BufferedInputStream in = new BufferedInputStream(new ClassPathResource(testName, Application.class).getInputStream())) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(testName))) {
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
            throw new RuntimeException("Exception encountered whilst attempting to exercise the Z80 CPU core", e);
        }
    }

    public static void main(String[] args) {

        new SpringApplicationBuilder(Application.class).headless(true).run(args);
    }

}
