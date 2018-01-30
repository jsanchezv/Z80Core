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
import z80core.MemIoOps;
import z80core.NotifyOps;
import z80core.Z80;

/**
 *
 * @author jsanchez
 */
public class Z80Exerciser implements NotifyOps {

    private final Z80 z80;
    private final MemIoOps memIo;

    private final byte z80Ram[] = new byte[0x10000];
    private boolean finish = false;
    
    public Z80Exerciser() {
        memIo = new MemIoOps(0, 0);
        memIo.setRam(z80Ram);
        z80 = new Z80(memIo, this);
    }

    @Override
    public int breakpoint(int address, int opcode) {
        // Emulate CP/M Syscall at address 5
        switch (z80.getRegC()) {
            case 0: // BDOS 0 System Reset
                System.out.println("Z80 reset after " + memIo.getTstates() + " t-states");
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
        return opcode;
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
        memIo.reset();
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
        System.out.println("Test zexall.bin executed in " + (System.currentTimeMillis() - start) + " ms.");
//        start = System.currentTimeMillis();
//        exerciser.runTest("zexall.bin");
//        System.out.println("Test zexall.bin executed in " + (System.currentTimeMillis() - start) + " ms.");
//        exerciser.runTest("zexdoc.bin");
        //Test.testFrame(6988800);
        //Test.testFrame(69888);
        //Test.speedTest();
    }
}
