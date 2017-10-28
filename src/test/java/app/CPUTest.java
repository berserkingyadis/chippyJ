package app;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by bers on 6/26/16.
 * Tests all the Opcodes
 */
public class CPUTest {
    Logger LOG = Logger.getLogger(CPUTest.class);
    static {
        PropertyConfigurator.configure(Main.class.getResource("/config/log4j.properties"));
    }
    CPU cpu;
    Memory mem;
    Display dis;
    Display mdis;
    Input min;

    @Before
    public void init(){
        mem = new Memory();
        mdis = mock(Display.class);
        min = mock(Input.class);
        cpu = new CPU(mem,mdis,min);
    }
    /**
     * 00E0	Clear the screen
     */
    @Test
    public void flushDisplayTest() {
        /* This Code should actually be in the displaytest.
        //Fill display with true bits
        for (int i = 0; i < cpu.display.WIDTH; i++) {
            for (int j = 0; j < cpu.display.HEIGHT; j++) {
                cpu.display.screen[i][j]=true;
            }
        }
        cpu.flushDisplay();
        //Check if everything is false
        for (int i = 0; i < cpu.display.WIDTH; i++) {
            for (int j = 0; j < cpu.display.HEIGHT; j++) {
                assertFalse(cpu.display.screen[i][j]);
            }
        }
        */
        short opcode = (short)0x00E0;
        cpu.opcode=opcode;
        cpu.process();
        // 2 Times because it gets flushed in the initialization process
        verify(mdis,times(2)).flush();

    }

    /*
        00EE	Return from a subroutine
                The interpreter sets the program counter to the address at the top of the stack, then subtracts 1 from the stack pointer.
     */
    @Test
    public void returnFromSubroutineTest() {
        //program_counter = stack.pop();

        short address = (short)(0xFFFF);
        cpu.stack.push(address);

        short opcode = (short)(0x00EE);
        cpu.opcode = opcode;
        cpu.process();
        assertEquals(0,cpu.stack.size());
        assertEquals(address, cpu.program_counter);
    }

    /*
       1NNN	    Jump to address NNN
               The interpreter sets the program counter to nnn.
    */
    @Test
    public void jumpToAddressNNNTest() {
        //program_counter = nnn;

        short address = (short)(0x321);
        short opcode = (short)(0x1321);

        cpu.opcode = opcode;
        cpu.process();

        assertEquals(address,cpu.program_counter);
        assertEquals(address,cpu.nnn);

    }

    /*
        2NNN	Execute subroutine starting at address NNN
                The interpreter increments the stack pointer, then puts the current PC on the top of the stack. The PC is then set to nnn.
     */
    @Test
    public void executeSubAtNNNTest() {
        /*
        if (stack.size() == 16) System.err.println("Stack Overflow: Too many subroutine calls. Exiting ...");
        stack.push(program_counter);
        program_counter = nnn;
        */
        short before = (short)0x00E0;
        short before_mem_location = (short)0x200;

        short opcode = (short)0x2333;
        short address = (short)0x333;

        cpu.opcode=before;
        cpu.process();
        cpu.opcode=opcode;
        cpu.process();

        assertEquals(1,cpu.stack.size());
        assertEquals(address,cpu.program_counter);
        assertEquals(before_mem_location,cpu.stack.peek().shortValue());

    }

    /*
        3XNN	Skip the following instruction if the value of register VX equals NN
     */
    @Test
    public void skipNextIfVXEqNNTest() {
        //if (vX == nn) program_counter += 2;
        cpu.registers[0]=(byte)0x00;

        short opcode0 = (short)0x3000;
        short opcode1 = (short)0x3001;
        cpu.opcode=opcode0;
        cpu.process();

        //pc should be incremented by 2
        assertEquals((short)0x202,cpu.program_counter);
        cpu.opcode=opcode1;
        cpu.process();
        //pc should not be incremented by 2
        assertEquals((short)0x202,cpu.program_counter);


    }

    /*
        4XNN	Skip the following instruction if the value of register VX is not equal to NN
    */
    @Test
    public void skipNextIfVXnEqNNTest() {
        //if (vX != nn) program_counter += 2;
        cpu.registers[0]=(byte)0x00;

        short opcode0 = (short)0x4000;
        short opcode1 = (short)0x4001;
        cpu.opcode=opcode0;
        cpu.process();

        //pc should not be incremented by 2
        assertEquals((short)0x200,cpu.program_counter);
        cpu.opcode=opcode1;
        cpu.process();
        //pc should be incremented by 2
        assertEquals((short)0x202,cpu.program_counter);
    }

    /*
        5XY0	Skip the following instruction if the value of register VX is equal to the value of register VY
   */
    @Test
    public void skipNextIfVXEqVYTest() {
        //if (vX == vY) program_counter += 2;
        cpu.registers[0] = (byte)0x00;
        cpu.registers[1] = (byte)0x00;
        cpu.registers[2] = (byte)0x01;
        cpu.opcode = (short)0x5010;
        cpu.process();
        assertEquals(0x202,cpu.program_counter);
        cpu.opcode = (short)0x5020;
        cpu.process();
        assertEquals(0x202,cpu.program_counter);
    }

    /*
        6XNN	Store number NN in register VX
     */
    @Test
    public void storeNNinVXTest() {
        //writeRegister(x, nn);
        cpu.opcode = (short)0x60EE;
        cpu.process();
        assertEquals((byte)0xEE,cpu.registers[0]);

    }

    /*
       7XNN	    Add the value NN to register VX
        */
    @Test
    public void addNNtoVXTest() {
        // writeRegister(x, vX + nn);
        cpu.registers[0] = (byte)0x05;

        cpu.opcode = (short)0x7001;
        cpu.process();
        assertEquals((byte)0x06,cpu.registers[0]&0xFF);
    }

    /*
        8XY0	Store the value of register VY in register VX
     */
    @Test
    public void storeVYinVXTest() {
        cpu.registers[0] = (byte)0x02;
        cpu.registers[1] = (byte)0x03;

        cpu.opcode = (short)0x8010;
        cpu.process();
        assertEquals((byte)0x03,cpu.registers[0]&0xFF);
    }

    /*
        8XY1	Set VX to VX OR VY
     */
    @Test
    public void setVXtoVXorVYTest() {
        //writeRegister(x, vX | vY);
        cpu.registers[0] = (byte)0xF0;
        cpu.registers[1] = (byte)0x0F;
        cpu.opcode = (short)0x8011;
        cpu.process();
        assertEquals((byte)0xFF,cpu.registers[0]);
    }

    /*
      8XY2	Set VX to VX AND VY
   */
    @Test
    public void setVXtoVXandVYTest() {
        //registers[x] = (byte) (vX & vY);
        cpu.registers[0] = (byte)0xB2;
        cpu.registers[1] = (byte)0x81;
        cpu.opcode = (short)0x8012;
        cpu.process();
        assertEquals((byte)0x80,cpu.registers[0]);
        cpu.registers[0] = (byte)0xF0;
        cpu.registers[1] = (byte)0x0F;
        cpu.opcode = (short)0x8012;
        cpu.process();
        assertEquals((byte)0x0,cpu.registers[0]);
    }

    /*
        8XY3	Set VX to VX XOR VY
     */
    @Test
    public void setVXtoVXxorVYTest() {
        //registers[x] = (byte) (vX ^ vY);
        cpu.registers[0] = (byte)0b11111000;
        cpu.registers[1] = (byte)0b00011111;
        cpu.registers[2] = (byte)0b11111111;
        byte res1 = (byte)0b11100111;
        byte res2 = (byte)0b11100000;

        cpu.opcode = (short)0x8013;
        cpu.process();
        assertEquals(res1,cpu.registers[0]);

        cpu.opcode= (short)0x8123;
        cpu.process();
        assertEquals(res2,cpu.registers[1]);
    }

    /*
       8XY4	Add the value of register VY to register VX
               Set VF to 01 if a carry occurs
               Set VF to 00 if a carry does not occur
    */
    @Test
    public void addVYtoVXCarryTest() {
        cpu.registers[0] = (byte)0x50;
        cpu.registers[1] = (byte)0x80;
        cpu.opcode = (short)0x8014;
        cpu.process();
        assertEquals((byte)0xD0,cpu.registers[0]);
        assertEquals((byte)0x00,cpu.registers[0xF]);

        cpu.registers[1] = (byte)0x80;
        cpu.registers[2] = (byte)0x80;
        cpu.opcode = (short)0x8124;
        cpu.process();
        assertEquals((byte)0x00,cpu.registers[1]);
        assertEquals((byte)0x01,cpu.registers[0xF]);

        cpu.registers[2] = (byte)0x80;
        cpu.registers[3] = (byte)0xFF;
        cpu.opcode = (short)0x8234;
        cpu.process();
        assertEquals((byte)0x7F,cpu.registers[2]);
        assertEquals((byte)0x01,cpu.registers[0xF]);

        cpu.registers[2] = (byte)0x00;
        cpu.registers[3] = (byte)0x00;
        cpu.opcode = (short)0x8234;
        cpu.process();
        assertEquals(0,cpu.registers[2]);
        assertEquals(0,cpu.registers[0xF]);

    }

    /*
        8XY5	Subtract the value of register VY from register VX
                Set VF to 00 if a borrow occurs
                Set VF to 01 if a borrow does not occur
     */
    @Test
    public void subVYfromVXCarryTest() {
        cpu.registers[0] = (byte)0x80;
        cpu.registers[1] = (byte)0x50;
        cpu.opcode = (short)0x8015;
        cpu.process();
        assertEquals((byte)0x30,cpu.registers[0]);
        assertEquals((byte)0x01,cpu.registers[0xF]);

        cpu.registers[1] = (byte)0x80;
        cpu.registers[2] = (byte)0x80;
        cpu.opcode = (short)0x8125;
        cpu.process();
        assertEquals((byte)0x00,cpu.registers[1]);
        assertEquals((byte)0x01,cpu.registers[0xF]);

        cpu.registers[2] = (byte)0x80;
        cpu.registers[3] = (byte)0xFF;
        cpu.opcode = (short)0x8235;
        cpu.process();
        assertEquals((byte)0x81,cpu.registers[2]);
        assertEquals((byte)0x00,cpu.registers[0xF]);

        cpu.registers[2] = (byte)0x00;
        cpu.registers[3] = (byte)0x00;
        cpu.opcode = (short)0x8235;
        cpu.process();
        assertEquals(0,cpu.registers[2]);
        assertEquals(01,cpu.registers[0xF]);
    }

    /*
    8XY6	Store the value of register VY shifted right one bit in register VX
            Set register VF to the least significant bit prior to the shift
     */
    @Test
    public void storeVYshiftedRight1inVXTest() {

        cpu.registers[1] = (byte)0b10000000;
        cpu.opcode = (short)0x8016;
        cpu.process();
        assertEquals(0,cpu.registers[0xF]);
        assertEquals((byte)0b01000000,cpu.registers[0]);

        cpu.opcode = (short)0x8106;
        cpu.process();
        assertEquals(0,cpu.registers[0xF]);
        assertEquals((byte)0b00100000,cpu.registers[1]);

        cpu.registers[1] = (byte)0b00000001;
        cpu.opcode = (short)0x8016;
        cpu.process();
        assertEquals(1,cpu.registers[0xF]);
        assertEquals(0,cpu.registers[0]);

    }

    /*
        8XY7    Set register VX to the value of VY minus VX
                Set VF to 00 if a borrow occurs
                Set VF to 01 if a borrow does not occur
     */
    @Test
    public void setVXtoVYminusVXborrowTest() {
        cpu.registers[0] = (byte)0x80;
        cpu.registers[1] = (byte)0x50;
        cpu.opcode = (short)0x8107;
        cpu.process();
        assertEquals((byte)0x30,cpu.registers[1]);
        assertEquals((byte)0x01,cpu.registers[0xF]);

        cpu.registers[1] = (byte)0x80;
        cpu.registers[2] = (byte)0x80;
        cpu.opcode = (short)0x8217;
        cpu.process();
        assertEquals((byte)0x00,cpu.registers[2]);
        assertEquals((byte)0x01,cpu.registers[0xF]);

        cpu.registers[2] = (byte)0x80;
        cpu.registers[3] = (byte)0xFF;
        cpu.opcode = (short)0x8327;
        cpu.process();
        assertEquals((byte)0x81,cpu.registers[3]);
        assertEquals((byte)0x00,cpu.registers[0xF]);

        cpu.registers[2] = (byte)0x00;
        cpu.registers[3] = (byte)0x00;
        cpu.opcode = (short)0x8327;
        cpu.process();
        assertEquals(0,cpu.registers[3]);
        assertEquals(1,cpu.registers[0xF]);

    }

    /*
       8XYE	Store the value of register VY shifted left one bit in register VX
               Set register VF to the most significant bit prior to the shift
    */
    @Test
    public void storeVYshiftedLeft1inVXTest() {
        /*
        registers[0xF] = (byte) (vX & 0x80);
        registers[x] = (byte) (vY << 1);
        */
        cpu.registers[1] = (byte)0b00001000;
        cpu.opcode = (short)0x801E;
        cpu.process();
        assertEquals(0,cpu.registers[0xF]);
        assertEquals((byte)0b00010000,cpu.registers[0]);

        cpu.opcode = (short)0x810e;
        cpu.process();
        assertEquals(0,cpu.registers[0xF]);
        assertEquals((byte)0b00100000,cpu.registers[1]);

        cpu.registers[1] = (byte)0b10000000;
        cpu.opcode = (short)0x801E;
        cpu.process();
        assertEquals(1,cpu.registers[0xF]);
        assertEquals(0,cpu.registers[0]);

    }

    /*
       9XY0	Skip the following instruction if the value of register VX is not equal to the value of register VY
    */
    @Test
    public void skipNextofVXneVYTest() {
        //if (vX != vY) program_counter += 2;
        cpu.registers[0]=(byte)0x00;
        cpu.registers[1]=(byte)0x01;

        cpu.opcode = (short)0x9010;
        cpu.process();

        assertEquals((short)0x202,cpu.program_counter);
        cpu.registers[1]--;
        cpu.process();
        assertEquals((short)0x202,cpu.program_counter);
    }

    /*
        ANNN	Store memory address NNN in register I
     */
    @Test
    public void storeNNNinITest() {
        cpu.opcode=(short)0xA123;
        cpu.process();
        assertEquals((short)0x123,cpu.I);

    }

    /*
        BNNN	Jump to address NNN + V0
     */
    @Test
    public void jumpToNNNplusV0Test() {
        //program_counter = (short) (nnn + registers[0x0] & 0xFF);
        cpu.opcode=(short)0xBFF3;
        cpu.registers[0]=(byte)0x02;
        cpu.process();
        assertEquals((short)0xFF5,cpu.program_counter);
    }

    /*
        CXNN	Set VX to a random number with a mask of NN
     */
    @Test
    public void setVXtoRNDmaskNNTest() {
        //
        // writeRegister(x, Utils.randomNumber() & nn);
        cpu.opcode=(short)0xC0FF;
        for (int i = 0; i < 1_000; i++) {
            cpu.process();
            int r = cpu.registers[0]&0xFF;
            assertEquals(true,r<256);
            assertEquals(false,r>=256);
            assertEquals(true,r>=0);
        }
        cpu.opcode=(short)0xC00F;
        for (int i = 0; i < 1_000; i++) {
            cpu.process();
            int r = cpu.registers[0]&0xFF;
            assertEquals(true,r<128);
            assertEquals(false,r>=128);
            assertEquals(true,r>=0);
        }
        cpu.opcode=(short)0xC000;
        for (int i = 0; i < 1_000; i++) {
            cpu.process();
            int r = cpu.registers[0]&0xFF;
            assertEquals(0,r);
        }

    }

    /*
        DXYN	Draw a sprite at position VX, VY with N bytes of sprite data starting at the address stored in I
                Set VF to 01 if any set pixels are changed to unset, and 00 otherwise

                If the sprite is to be visible on the screen,
                the VX register must contain a value between 00 and 3F, (0-63)
                and the VY register must contain a value between 00 and 1F. (0-31)
     */
    @Test
    public void drawSpriteFromIlengthNtoVXandVYTest() {
        /*
        byte[] data = new byte[n];
        boolean flag = false;
        for (int i = 0; i < n; i++) {
            if (display.drawSprite(vX, (byte) (((byte) (vY % 0xFF) + i) & 0xff), readMemory(I + i))) flag = true;
            display.repaint();
        }
        if (flag) registers[0xF] = 0x1;
        else registers[0xF] = 0x0;
        */
        cpu.opcode = (short)0xD122;
        cpu.process();
        when(mdis.drawSprite(Mockito.anyByte(),Mockito.anyByte(),Mockito.anyByte())).thenReturn(false);
        verify(mdis,times(2)).drawSprite(Mockito.anyByte(),Mockito.anyByte(),Mockito.anyByte());
        verify(mdis,times(1)).repaint();

    }

    /*
        FX07	Store the current value of the delay timer in register VX
     */
    @Test
    public void storeDelayTimeinVXTest() {
        //writeRegister(x, delay_timer);

    }

    /*
        FX15	Set the delay timer to the value of register VX
     */
    @Test
    public void setDelayTimerToVXTest() {
        // delay_timer = vX;
        cpu.registers[0]=(byte)0x64;
        cpu.opcode = (short)0xF015;
        cpu.process();
        assertEquals(100,cpu.delay_timer);
    }

    /*
       FX18	Set the sound timer to the value of register VX
    */
    @Test
    public void setSoundTimerToVXTest() {
        //sound_timer = vX;
        cpu.registers[0]=(byte)0x64;
        cpu.opcode = (short)0xF018;
        cpu.process();
        assertEquals(100,cpu.sound_timer);
    }

    /*
        FX1E	Add the value stored in register VX to register I
     */
    @Test
    public void addVXtoITest() {
        //setIndex(getIndex() + vX);
        cpu.registers[0]=0x64;
        cpu.I = 0x1;
        cpu.opcode = (short)0xF01E;
        cpu.process();
        assertEquals((short)0x65,cpu.I);

        cpu.registers[0]=(byte)0xFE;
        cpu.I = 0x1;
        cpu.opcode = (short)0xF01E;
        cpu.process();
        LOG.debug(Utils.shortToHex(cpu.I));
        assertEquals((short)0x00FF,cpu.I);
    }

    /*
        FX29	Set I to the memory address of the sprite data corresponding to the hexadecimal digit stored in register VX
     */
    @Test
    public void setItoMemOfSpriteVXTest() {
        //setIndex((vX&0xFF) * 5);
        cpu.registers[0]=0x00;
        cpu.registers[1]=0x01;
        cpu.registers[2]=0x02;
        cpu.registers[3]=0x03;
        cpu.registers[4]=0x04;
        cpu.registers[5]=0x05;
        cpu.registers[6]=0x06;
        cpu.registers[7]=0x07;
        cpu.registers[8]=0x08;
        cpu.registers[9]=0x09;
        cpu.registers[0xA]=0x0A;
        cpu.registers[0xB]=0x0B;
        cpu.registers[0xC]=0x0C;
        cpu.registers[0xD]=0x0D;
        cpu.registers[0xE]=0x0E;
        cpu.registers[0xF]=0x0F;

        cpu.opcode=(short)0xF029;
        cpu.process();
        assertEquals(0x0,cpu.I);
        cpu.opcode=(short)0xF129;
        cpu.process();
        assertEquals(0x5,cpu.I);
        cpu.opcode=(short)0xF229;
        cpu.process();
        assertEquals(0xA,cpu.I);
        cpu.opcode=(short)0xF329;
        cpu.process();
        assertEquals(0xF,cpu.I);
        cpu.opcode=(short)0xF429;
        cpu.process();
        assertEquals(0x14,cpu.I);
        cpu.opcode=(short)0xF529;
        cpu.process();
        assertEquals(25,cpu.I);
        cpu.opcode=(short)0xF629;
        cpu.process();
        assertEquals(30,cpu.I);
        cpu.opcode=(short)0xF729;
        cpu.process();
        assertEquals(35,cpu.I);
        cpu.opcode=(short)0xF829;
        cpu.process();
        assertEquals(40,cpu.I);
        cpu.opcode=(short)0xF929;
        cpu.process();
        assertEquals(45,cpu.I);
        cpu.opcode=(short)0xFA29;
        cpu.process();
        assertEquals(50,cpu.I);
        cpu.opcode=(short)0xFB29;
        cpu.process();
        assertEquals(55,cpu.I);
        cpu.opcode=(short)0xFC29;
        cpu.process();
        assertEquals(60,cpu.I);
        cpu.opcode=(short)0xFD29;
        cpu.process();
        assertEquals(0x41,cpu.I);
        cpu.opcode=(short)0xFE29;
        cpu.process();
        assertEquals(0x46,cpu.I);
        cpu.opcode=(short)0xFF29;
        cpu.process();
        assertEquals(0x4B,cpu.I);

    }

    /*
        FX33	Store the binary-coded decimal equivalent of the value stored in register VX at addresses I, I+1, and I+2
     */
    @Test
    public void storeBinaryNumbersofVXinIffTest() {
        /*
        writeMemory(I, (vX & 0xFF) / 100);
        writeMemory(I + 1, ((vX & 0xFF) / 10) % 10);
        writeMemory(I + 2, (vX & 0xFF) % 10);
        */
        cpu.registers[0]=0;
        cpu.I = (short)0x300;

        cpu.opcode=(short)0xF033;
        cpu.process();

        assertEquals(0,cpu.memory.ram[0x300]);
        assertEquals(0,cpu.memory.ram[0x301]);
        assertEquals(0,cpu.memory.ram[0x302]);

        cpu.registers[0]=(byte)123;
        cpu.I = (short)0x300;

        cpu.opcode=(short)0xF033;
        cpu.process();

        assertEquals(1,cpu.memory.ram[0x300]);
        assertEquals(2,cpu.memory.ram[0x301]);
        assertEquals(3,cpu.memory.ram[0x302]);

        cpu.registers[0]=(byte)255;
        cpu.I = (short)0xF00;

        cpu.opcode=(short)0xF033;
        cpu.process();

        assertEquals(2,cpu.memory.ram[0xF00]);
        assertEquals(5,cpu.memory.ram[0xF01]);
        assertEquals(5,cpu.memory.ram[0xF02]);



    }

    /*
       FX55	Store the values of registers V0 to VX inclusive in memory starting at address I
               I is set to I + X + 1 after operation
    */
    @Test
    public void storeV0toVXinMemAtIffTest() {
        /*
        for (int i = 0; i <= x; i++) {
            writeMemory(I + i, readRegister(i));
        }
        */
        cpu.opcode = (short) 0xFE55;
        cpu.I = (short)0x300;
        byte t0 = (byte)0x01;
        byte t1 = (byte)0x0A;
        byte t2 = (byte)0x10;
        byte t3 = (byte)0xA0;
        byte t4 = (byte)0x11;
        byte t5 = (byte)0xAA;
        byte t6 = (byte)0x0F;
        byte t7 = (byte)0xF0;
        byte t8 = (byte)0xFF;
        byte t9 = (byte)0x22;
        byte tA = (byte)0x33;
        byte tB = (byte)0x44;
        byte tC = (byte)0x55;
        byte tD = (byte)0x66;
        byte tE = (byte)0x77;
        cpu.registers[0] = t0;
        cpu.registers[1] = t1;
        cpu.registers[2] = t2;
        cpu.registers[3] = t3;
        cpu.registers[4] = t4;
        cpu.registers[5] = t5;
        cpu.registers[6] = t6;
        cpu.registers[7] = t7;
        cpu.registers[8] = t8;
        cpu.registers[9] = t9;
        cpu.registers[0xA] = tA;
        cpu.registers[0xB] = tB;
        cpu.registers[0xC] = tC;
        cpu.registers[0xD] = tD;
        cpu.registers[0xE] = tE;


        cpu.process();

        assertEquals(t0,cpu.memory.ram[0x300]);
        assertEquals(t1,cpu.memory.ram[0x301]);
        assertEquals(t2,cpu.memory.ram[0x302]);
        assertEquals(t3,cpu.memory.ram[0x303]);
        assertEquals(t4,cpu.memory.ram[0x304]);
        assertEquals(t5,cpu.memory.ram[0x305]);
        assertEquals(t6,cpu.memory.ram[0x306]);
        assertEquals(t7,cpu.memory.ram[0x307]);
        assertEquals(t8,cpu.memory.ram[0x308]);
        assertEquals(t9,cpu.memory.ram[0x309]);
        assertEquals(tA,cpu.memory.ram[0x30A]);
        assertEquals(tB,cpu.memory.ram[0x30B]);
        assertEquals(tC,cpu.memory.ram[0x30C]);
        assertEquals(tD,cpu.memory.ram[0x30D]);
        assertEquals(tE,cpu.memory.ram[0x30E]);
        assertEquals((short)0x30F,cpu.getIndex());
    }
    /*
        FX65	Fill registers V0 to VX
        inclusive with the values stored in memory starting at address I
                I is set to I + X + 1 after operation
     */
    @Test
    public void fillV0toVXwithValAtIffTest() {
        /*
        for (int i = 0; i <= x; i++) {
            writeRegister(i, readMemory(I + i));
        }
        setIndex(getIndex() + x + 1);
        */
        cpu.opcode = (short) 0xFE65;
        cpu.I = (short)0x300;
        byte t0 = (byte)0x01;
        byte t1 = (byte)0x0A;
        byte t2 = (byte)0x10;
        byte t3 = (byte)0xA0;
        byte t4 = (byte)0x11;
        byte t5 = (byte)0xAA;
        byte t6 = (byte)0x0F;
        byte t7 = (byte)0xF0;
        byte t8 = (byte)0xFF;
        byte t9 = (byte)0x22;
        byte tA = (byte)0x33;
        byte tB = (byte)0x44;
        byte tC = (byte)0x55;
        byte tD = (byte)0x66;
        byte tE = (byte)0x77;
        cpu.memory.ram[0x300] = t0;
        cpu.memory.ram[0x301] = t1;
        cpu.memory.ram[0x302] = t2;
        cpu.memory.ram[0x303] = t3;
        cpu.memory.ram[0x304] = t4;
        cpu.memory.ram[0x305] = t5;
        cpu.memory.ram[0x306] = t6;
        cpu.memory.ram[0x307] = t7;
        cpu.memory.ram[0x308] = t8;
        cpu.memory.ram[0x309] = t9;
        cpu.memory.ram[0x30A] = tA;
        cpu.memory.ram[0x30B] = tB;
        cpu.memory.ram[0x30C] = tC;
        cpu.memory.ram[0x30D] = tD;
        cpu.memory.ram[0x30E] = tE;

        cpu.process();

        assertEquals(t0,cpu.registers[0]);
        assertEquals(t1,cpu.registers[1]);
        assertEquals(t2,cpu.registers[2]);
        assertEquals(t3,cpu.registers[3]);
        assertEquals(t4,cpu.registers[4]);
        assertEquals(t5,cpu.registers[5]);
        assertEquals(t6,cpu.registers[6]);
        assertEquals(t7,cpu.registers[7]);
        assertEquals(t8,cpu.registers[8]);
        assertEquals(t9,cpu.registers[9]);
        assertEquals(tA,cpu.registers[0xA]);
        assertEquals(tB,cpu.registers[0xB]);
        assertEquals(tC,cpu.registers[0xC]);
        assertEquals(tD,cpu.registers[0xD]);
        assertEquals(tE,cpu.registers[0xE]);
        assertEquals((short)0x30F,cpu.getIndex());

    }

    @Test
    public void delayTimerTest(){
        cpu.startTimers();
        cpu.delay_timer=60;
        assertEquals(true,cpu.delay_timer>0);
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(false,cpu.delay_timer>0);
    }

    @Test
    public void soundTimerTest(){
        cpu.startTimers();
        cpu.sound_timer=60;
        assertEquals(true,cpu.sound_timer>0);
        try {
            Thread.sleep(1300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(false,cpu.sound_timer>0);
    }

}
