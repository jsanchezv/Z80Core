/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import z80core.Clock;
import z80core.Z80operations;
import z80core.Z80;

/**
 *
 * @author jsanchez
 */
public class Z80Exerciser implements Z80operations {

    private final Z80 z80;
    private final Clock clock;
    
    private final byte z80Ram[] = new byte[0x10000];
    private final byte z80Ports[] = new byte[0x10000];
    private boolean finish = false;
    
    public Z80Exerciser() {
        z80 = new Z80(this);
        this.clock = Clock.getInstance();
    }

    @Override
    public int fetchOpcode(int address) {
        // 3 clocks to fetch opcode from RAM and 1 execution clock
        clock.addTstates(4);
        return z80Ram[address] & 0xff;
    }

    @Override
    public int peek8(int address) {
        clock.addTstates(3); // 3 clocks for read byte from RAM
        return z80Ram[address] & 0xff;
    }

    @Override
    public void poke8(int address, int value) {
        clock.addTstates(3); // 3 clocks for write byte to RAM
        z80Ram[address] = (byte)value;
    }

    @Override
    public int peek16(int address) {
        int lsb = peek8(address);
        int msb = peek8(address + 1);
        return (msb << 8) | lsb;
    }

    @Override
    public void poke16(int address, int word) {
        poke8(address, word);
        poke8(address + 1, word >>> 8);
    }

    @Override
    public int inPort(int port) {
        clock.addTstates(4); // 4 clocks for read byte from bus
        return z80Ports[port] & 0xff;
    }

    @Override
    public void outPort(int port, int value) {
        clock.addTstates(4); // 4 clocks for write byte to bus
        z80Ports[port] = (byte)value;
    }

    @Override
    public void contendedStates(int address, int tstates) {
        // Additional clocks to be added on some instructions
        clock.addTstates(tstates);
    }

    @Override
    public void breakpoint() {
        // Emulate CP/M Syscall at address 5
        switch (z80.getRegC()) {
            case 0: // BDOS 0 System Reset
                System.out.println("Z80 reset after " + clock.getTstates() + " t-states");
                finish = true;
                break;
            case 2: // BDOS 2 console char output
                System.out.print((char) z80.getRegE());
                break;
            case 9: // BDOS 9 console string output (string terminated by "$")
                int strAddr = z80.getRegDE();
                while (z80Ram[strAddr] != '$') {
                    System.out.print((char) z80Ram[strAddr++]);
                }
                break;
            default:
                System.out.println("BDOS Call " + z80.getRegC());
                finish = true;
        }
    }

    @Override
    public void execDone() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void runTest(String testName) {
        try (BufferedInputStream in = new BufferedInputStream( new FileInputStream(testName) )) {
            int count = in.read(z80Ram, 0x100, 0xFF00);
            System.out.println("Readed " + count + " bytes from " + testName);
        } catch (IOException ex) {
            Logger.getLogger(Z80Exerciser.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        z80.reset();
        clock.reset();
        finish = false;

        z80Ram[0] = (byte)0xC3;
	z80Ram[1] = 0x00;
	z80Ram[2] = 0x01;	// JP 0x100 CP/M TPA
        z80Ram[5] = (byte)0xC9; // Return from BDOS call

        System.out.println("Starting test " + testName);
        z80.setBreakpoint(0x0005, true);
        while(!finish) {
            z80.execute();
        }
        System.out.println("Test " + testName + " ended.");
    }

    public static void main(String[] args) {
        // TODO code application logic here
        Z80Exerciser exerciser = new Z80Exerciser();
        long start = System.currentTimeMillis();
        exerciser.runTest("zexall.bin");
        System.out.println("Test zexall.bin executed in " + (System.currentTimeMillis() - start));
        exerciser.runTest("zexdoc.bin");
        //Test.testFrame(6988800);
        //Test.testFrame(69888);
        //Test.speedTest();
    }
    
}
