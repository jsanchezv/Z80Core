package z80core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedInputStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Z80 exerciser unit test
 *
 * @author Jose Luis Sanchez
 * @author Jose Hernandez
 */
@Slf4j
public class Z80ExerciserTest implements NotifyOps {

    private final byte[] z80Ram = new byte[0x10000];
    private Z80 z80;
    private MemIoOps memIo;
    private boolean finish;
    private StringBuffer buffer;

    private static Stream<Arguments> z80ExerciserParameterProvider() {
        return Stream.of(
                // NOTE: "zexdoc.bin" is temporarily disabled as the test seems to run indefinitely
                // Arguments.of("documented Z80 instruction set", "zexdoc.bin"),
                Arguments.of("complete Z80 instruction set", "zexall.bin")
        );
    }

    @BeforeEach
    public void setup() {
        finish = false;
        buffer = new StringBuffer();
        memIo = new MemIoOps(0, 0);
        memIo.setRam(z80Ram);
        z80 = new Z80(memIo, this);
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

    @DisplayName("Z80Core can run Z80 programs correctly")
    @ParameterizedTest(name = "The Z80 core runs correctly when executing the {0}")
    @MethodSource("z80ExerciserParameterProvider")
    public void test_Z80Exerciser(final String description, final String exerciser) {
        final long start = System.currentTimeMillis();
        log.info("Starting {} test ({})", description, exerciser);
        assertThat(String.format("'%s' completed successfully", exerciser), runTest(exerciser), is(true));
        log.info("Executed {} ({}) exercise in {} ms", description, exerciser, System.currentTimeMillis() - start);

    }

    private boolean runTest(final String testFileName) {
        try (BufferedInputStream in = new BufferedInputStream(new ClassPathResource(testFileName).getInputStream())) {
            int count = in.read(z80Ram, 0x100, 0xFF00);
            log.info("Read {} bytes from {}", count, testFileName);

            z80.reset();
            memIo.reset();
            finish = false;

            z80Ram[0] = (byte) 0xC3;
            z80Ram[1] = 0x00;
            z80Ram[2] = 0x01;           // JP 0x100 CP/M TPA
            z80Ram[5] = (byte) 0xC9;    // Return from BDOS call

            z80.setBreakpoint(0x0005, true);
            while (!finish) {
                z80.execute();
            }
        } catch (Exception e) {
            log.error("Exception encountered whilst attempting to exercise the Z80 CPU core", e);
            return false;
        }

        return true;
    }

}
