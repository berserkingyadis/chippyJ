package app;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class CPU {
    static Logger LOG = Logger.getLogger(CPU.class);

    public static final int REGISTER_AMOUNT = 16;
    public static final int STACK_LIMIT = 16;

    //general purpose registers
    //V0 - VF (16 registers)
    //VF is not directly used by the roms and is used as a flag
    public byte[] registers;
    // register I, commonly used to store memory adresses
    public short I;
    // memory adress where the currently executed code is
    public short program_counter;
    //CallStack
    public Stack<Short> stack;

    //times, automatically decremented at a rate of 60 Hz, 60 times in a second
    public int delay_timer;
    public int sound_timer;

    //how many operations the CPU calculates per second
    public static final int OP_PER_SEC = 100;

    public Memory memory = null;
    public Display display = null;
    public Input input = null;
    public short opcode;
    public String opcodestring = "";
    public byte x;
    public byte y;
    public byte n;
    public byte nn;
    public short nnn;
    public byte vX;
    public byte vY;

    public CPU(Memory mem, Display disp, Input inp) {
        memory = mem;
        display = disp;
        input=inp;
        initialize();
    }

    public void initialize() {
        x = 0;
        y = 0;
        vX = 0;
        vY = 0;
        n = 0;
        nn = 0;
        nnn = 0;
        I = 0;
        sound_timer = 0;
        delay_timer = 0;

        registers = new byte[REGISTER_AMOUNT];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        stack = new Stack<>();

        memory.initialize();
        display.registerInput(input);
        display.flush();

        program_counter = memory.INSERT_ROM_ADRESS;

    }

    public void start() {
        //Start the automatic decrementation of the timers
        startTimers();

        while (true) {
            //Read Opcode
            opcode = decodeOpcode(program_counter);
            program_counter += 2;

            //Print Debug Information before processing next step
            //printMachine();

            //Process Opcode and incremen program counter
            process();

            //Decrement Timers
            timers();
            //Wait a bit
            try {
                Thread.sleep(1000/OP_PER_SEC);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void startTimers() {
        new Thread(()->{
            while(true){
                if(delay_timer>0)delay_timer--;
                try {
                    //delay timer decrements at 60Hz (60 times in a second)
                    Thread.sleep(1000/60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while(true) {
                //if not zero => beep
                if (sound_timer > 0) {
                    //TODO: Make the beeping more smooth
                    Toolkit.getDefaultToolkit().beep();
                    sound_timer--;
                }
                try {
                    //sound timer decrements at 60Hz (60 times in a second)
                    Thread.sleep(1000 / 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void printMachine() {
        LOG.debug("----AT: " + Utils.shortToHex(program_counter) + " (" + program_counter + ")------------------------");
        LOG.debug("opcode = " + opcodestring);
        LOG.debug("x = " + Utils.byteToHex(x) + " (" + (x & 0xFF) + ")");
        LOG.debug("y = " + Utils.byteToHex(y) + " (" + (y & 0xFF) + ")");
        LOG.debug("n = " + Utils.byteToHex(n) + " (" + (n & 0xFF) + ")");
        LOG.debug("nn = " + Utils.byteToHex(nn) + " (" + (nn & 0xFF) + ")");
        LOG.debug("nnn = " + Utils.shortToHex(nnn) + " (" + (nnn & 0xFFF) + ")");
        for (int i = 0; i < registers.length; i++) {
            LOG.debug("Register V" + i + " = " + Utils.byteToHex(readRegister(i)) + " (" + (readRegister(i) & 0xFF) + ")");
        }
        LOG.debug("I = " + Utils.shortToHex(I) + " (" + (I & 0xFFFF) + ")");
    }


    public short decodeOpcode(short program_counter) {
        return (short) (((readMemory(program_counter) & 0xFF) << 8) | (readMemory(program_counter + 1) & 0xFF));
    }

    public void updateVariables() {
        opcodestring = Utils.shortToHex(opcode);
        x = Utils.getX(opcode);
        y = Utils.getY(opcode);
        n = Utils.getN(opcode);
        nn = Utils.getNN(opcode);
        nnn = Utils.getNNN(opcode);

        vX = readRegister(x);
        vY = readRegister(y);

    }

    public void process() {
        updateVariables();
        printMachine();
        //Interpret opcode
        //The logic of each opcode was put into methods to make the testing easier
        switch (opcode & 0xF000) {

                /*
                0NNN	Execute machine language subroutine at address NNN
                        This instruction is only used on the old computers on which Chip-8 was originally implemented. It is ignored by modern interpreters.
                00E0	Clear the screen
                00EE	Return from a subroutine
                        The interpreter sets the program counter to the address at the top of the stack, then subtracts 1 from the stack pointer.
                 */
            case 0x0000:
                switch (opcode & 0x00FF) {
                    case 0xE0:
                        flushDisplay();
                        break;
                    case 0xEE:
                        returnFromSubroutine();
                        break;
                    default:
                        LOG.fatal("This Opcode is not Supported: " + opcodestring);
                        System.exit(1);
                }
                break;

                /*
                1NNN	Jump to address NNN
                        The interpreter sets the program counter to nnn.
                 */
            case 0x1000:
                jumpToAddressNNN();
                return;

                /*
                2NNN	Execute subroutine starting at address NNN
                        The interpreter increments the stack pointer, then puts the current PC on the top of the stack. The PC is then set to nnn.
                 */
            case 0x2000:
                executeSubAtNNN();
                return;

                /*
                3XNN	Skip the following instruction if the value of register VX equals NN
                 */
            case 0x3000:
                skipNextIfVXEqNN();
                break;

                /*
                4XNN	Skip the following instruction if the value of register VX is not equal to NN
                 */
            case 0x4000:
                skipNextIfVXnEqNN();
                break;

                /*
                5XY0	Skip the following instruction if the value of register VX is equal to the value of register VY
                 */
            case 0x5000:
                skipNextIfVXEqVY();
                break;

                /*
                6XNN	Store number NN in register VX                writeRegister(x,nn);
                 */
            case 0x6000:
                storeNNinVX();
                break;

                /*
                7XNN	Add the value NN to register VX
                 */
            case 0x7000:
                addNNtoVX();
                break;

                /*
                8XY0	Store the value of register VY in register VX
                8XY1	Set VX to VX OR VY
                8XY2	Set VX to VX AND VY
                8XY3	Set VX to VX XOR VY
                8XY4	Add the value of register VY to register VX
                        Set VF to 01 if a carry occurs
                        Set VF to 00 if a carry does not occur
                8XY5	Subtract the value of register VY from register VX
                        Set VF to 00 if a borrow occurs
                        Set VF to 01 if a borrow does not occur
                8XY6	Store the value of register VY shifted right one bit in register VX
                        Set register VF to the least significant bit prior to the shift
                8XY7    Set register VX to the value of VY minus VX
                        Set VF to 00 if a borrow occurs
                        Set VF to 01 if a borrow does not occur
                8XYE	Store the value of register VY shifted left one bit in register VX
                        Set register VF to the most significant bit prior to the shift
                 */
            case 0x8000:
                switch (opcode & 0x000F) {
                    case 0:
                        storeVYinVX();
                        break;
                    case 1:
                        setVXtoVXorVY();
                        break;
                    case 2:
                        setVXtoVXandVY();
                        break;
                    case 3:
                        setVXtoVXxorVY();
                        break;
                    case 4:
                        addVYtoVXCarry();
                        break;
                    case 5:
                        subVYfromVXCarry();
                        break;
                    case 6:
                        storeVYshiftedRight1inVX();
                        break;
                    case 7:
                        setVXtoVYminusVXborrow();
                        break;
                    case 0xE:
                        storeVYshiftedLeft1inVX();
                        break;
                }
                break;

                /*
                9XY0	Skip the following instruction if the value of register VX is not equal to the value of register VY
                */
            case 0x9000:
                skipNextofVXneVY();
                break;

                /*
                ANNN	Store memory address NNN in register I
                 */
            case 0xA000:
                storeNNNinI();
                break;

                /*
                BNNN	Jump to address NNN + V0
                 */
            case 0xB000:
                jumpToNNNplusV0();
                return;

                /*
                CXNN	Set VX to a random number with a mask of NN
                 */
            case 0xC000:
                setVXtoRNDmaskNN();
                break;

                /*
                DXYN	Draw a sprite at position VX, VY with N bytes of sprite data starting at the address stored in I
                        Set VF to 01 if any set pixels are changed to unset, and 00 otherwise

                        If the sprite is to be visible on the screen,
                        the VX register must contain a value between 00 and 3F, (0-63)
                        and the VY register must contain a value between 00 and 1F. (0-31)
                 */
            case 0xD000:
                drawSpriteFromIlengthNtoVXandVY();
                break;

                /*
                EX9E	Skip the following instruction if the key corresponding to the hex value currently stored in register VX is pressed
                EXA1	Skip the following instruction if the key corresponding to the hex value currently stored in register VX is not pressed
                 */
            case 0xE000:
                switch (opcode & 0x000F) {
                    case 0xE:
                        skipNextIfVXPressed();
                        break;
                    case 0x1:
                        skipNextIfVXNotPressed();
                        break;
                }
                break;
                /*
                FX07	Store the current value of the delay timer in register VX
                FX0A	Wait for a keypress and store the result in register VX
                FX15	Set the delay timer to the value of register VX
                FX18	Set the sound timer to the value of register VX
                FX1E	Add the value stored in register VX to register I
                FX29	Set I to the memory address of the sprite data corresponding to the hexadecimal digit stored in register VX
                FX33	Store the binary-coded decimal equivalent of the value stored in register VX at addresses I, I+1, and I+2
                FX55	Store the values of registers V0 to VX inclusive in memory starting at address I
                        I is set to I + X + 1 after operation
                FX65	Fill registers V0 to VX inclusive with the values stored in memory starting at address I
                        I is set to I + X + 1 after operation
                 */
            case 0xF000:

                switch (opcode & 0x00FF) {
                    case 0x07:
                        storeDelayTimeinVX();
                        break;
                    case 0x0A:
                        wait4KeyStoreInVX();
                        break;
                    case 0x15:
                        setDelayTimerToVX();
                        break;
                    case 0x18:
                        setSoundTimerToVX();
                        break;
                    case 0x1E:
                        addVXtoI();
                        break;
                    case 0x29:
                        setItoMemOfSpriteVX();
                        break;
                    case 0x33:
                        storeBinaryNumbersofVXinIff();
                        break;
                    case 0x55:
                        storeV0toVXinMemAtIff();
                        break;
                    case 0x65:
                        fillV0toVXwithValAtIff();
                        break;
                }
                break;

        }
    }


    /*
        EX9E	Skip the following instruction if the key corresponding to the hex value currently stored in register VX is pressed
                */
    private void skipNextIfVXPressed() {
        byte keyToCheck = readRegister(x);
        if((byte)input.lastKeyPressed==keyToCheck){
            program_counter++;
        }
    }
    /*
        EXA1	Skip the following instruction if the key corresponding to the hex value currently stored in register VX is not pressed

     */
    private void skipNextIfVXNotPressed() {
        byte keyToCheck = readRegister(x);
        if(!((byte)input.lastKeyPressed==keyToCheck)){
            program_counter++;
        }
    }


    /**
        00E0	    Clear the screen
     */
    public void flushDisplay() {
        display.flush();
    }

    /*
        00EE	Return from a subroutine
                The interpreter sets the program counter to the address at the top of the stack, then subtracts 1 from the stack pointer.
     */
    public void returnFromSubroutine() {
        program_counter = stack.pop();

    }

    /*
       1NNN	    Jump to address NNN
               The interpreter sets the program counter to nnn.
    */
    private void jumpToAddressNNN() {
        program_counter = nnn;
    }

    /*
        2NNN	Execute subroutine starting at address NNN
                The interpreter increments the stack pointer, then puts the current PC on the top of the stack. The PC is then set to nnn.
     */
    public void executeSubAtNNN() {
        if (stack.size() == STACK_LIMIT) System.err.println("Stack Overflow: Too many subroutine calls. Exiting ...");
        stack.push(program_counter);
        program_counter = nnn;
    }

    /*
        3XNN	Skip the following instruction if the value of register VX equals NN
     */
    public void skipNextIfVXEqNN() {
        if (vX == nn) program_counter += 2;
    }

    /*
        4XNN	Skip the following instruction if the value of register VX is not equal to NN
    */
    public void skipNextIfVXnEqNN() {
        if (vX != nn) program_counter += 2;
    }

    /*
        5XY0	Skip the following instruction if the value of register VX is equal to the value of register VY
   */
    public void skipNextIfVXEqVY() {
        if (vX == vY) program_counter += 2;
    }

    /*
        6XNN	Store number NN in register VX
     */
    public void storeNNinVX() {
        writeRegister(x, nn);
    }

    /*
       7XNN	    Add the value NN to register VX
        */
    public void addNNtoVX() {
        writeRegister(x, vX + nn);
    }

    /*
        8XY0	Store the value of register VY in register VX
     */
    public void storeVYinVX() {
        writeRegister(x, vY);

    }

    /*
        8XY1	Set VX to VX OR VY
     */
    public void setVXtoVXorVY() {
        writeRegister(x, vX | vY);

    }

    /*
      8XY2	Set VX to VX AND VY
   */
    public void setVXtoVXandVY() {
        LOG.debug(Utils.byteToBin(vX));
        LOG.debug(Utils.byteToBin(vY));
        LOG.debug(" & -----------------");
        LOG.debug(Utils.byteToBin((byte) (vX & vY)));
        writeRegister(x,(byte)(vX & vY));

    }

    /*
        8XY3	Set VX to VX XOR VY
     */
    public void setVXtoVXxorVY() {
        LOG.debug(Utils.byteToBin(vX));
        LOG.debug(Utils.byteToBin(vY));
        LOG.debug(" ^ -----------------");
        LOG.debug(Utils.byteToBin((byte) (vX ^ vY)));
        writeRegister(x,(byte)(vX ^ vY));
    }
    /*
       8XY4	Add the value of register VY to register VX
               Set VF to 01 if a carry occurs
               Set VF to 00 if a carry does not occur
    */
    public void addVYtoVXCarry() {
        int temp = (vX&0xFF)+(vY&0xFF);
        if(temp > 255){
            writeRegister(0xF,1);
        }else{
            writeRegister(0xF,0);
        }
        writeRegister(x,temp&0xFF);
    }

    /*
        8XY5	Subtract the value of register VY from register VX
                Set VF to 00 if a borrow occurs
                Set VF to 01 if a borrow does not occur
     */
    public void subVYfromVXCarry() {
        int temp = (vX&0xFF)-(vY&0xFF);
        if(temp < 0){
            writeRegister(0xF,0);
        }else{
            writeRegister(0xF,1);
        }
        writeRegister(x,temp&0xFF);
    }

    /*
    8XY6	Store the value of register VY shifted right one bit in register VX
            Set register VF to the least significant bit prior to the shift
     */
    public void storeVYshiftedRight1inVX() {
        writeRegister(0xF,vY&0x1);
        LOG.debug("vX: "+Utils.byteToBin(vY));
        LOG.debug("vX: "+Utils.byteToBin((byte) ((vY&0xFF) >> 1)));
        writeRegister(x,(vY&0xFF) >> 1);
        LOG.debug(Utils.byteToBin(registers[x]));
    }

    /*
        8XY7    Set register VX to the value of VY minus VX
                Set VF to 00 if a borrow occurs
                Set VF to 01 if a borrow does not occur
     */
    public void setVXtoVYminusVXborrow() {
        int temp = (vY&0xFF)-(vX&0xFF);
        if(temp < 0){
            writeRegister(0xF,0);
        }else{
            writeRegister(0xF,1);
        }
        writeRegister(x,temp&0xFF);
    }

    /*
       8XYE	Store the value of register VY shifted left one bit in register VX
               Set register VF to the most significant bit prior to the shift
    */
    public void storeVYshiftedLeft1inVX() {
        writeRegister(0xF,(vY&0x80)>>7);
        writeRegister(x,vY<<1);
    }

    /*
       9XY0	Skip the following instruction if the value of register VX is not equal to the value of register VY
    */
    public void skipNextofVXneVY() {
        if (vX != vY) program_counter += 2;

    }

    /*
        ANNN	Store memory address NNN in register I
     */
    public void storeNNNinI() {
        I = nnn;
    }

    /*
        BNNN	Jump to address NNN + V0
     */
    public void jumpToNNNplusV0() {
        program_counter = (short) ((nnn) + (registers[0x0])) ;

    }

    /*
        CXNN	Set VX to a random number with a mask of NN
     */
    public void setVXtoRNDmaskNN() {
        writeRegister(x, (byte)(new Random().nextInt(256) & 0xFF) & nn);
    }

    /*
        DXYN	Draw a sprite at position VX, VY with N bytes of sprite data starting at the address stored in I
                Set VF to 01 if any set pixels are changed to unset, and 00 otherwise

                If the sprite is to be visible on the screen,
                the VX register must contain a value between 00 and 3F, (0-63)
                and the VY register must contain a value between 00 and 1F. (0-31)
     */
    public void drawSpriteFromIlengthNtoVXandVY() {
        LOG.trace("$"+Utils.shortToHex((short)((program_counter-2)&0xFF))+": "+opcodestring);
        LOG.trace("drawSprite");
        LOG.trace("x \t- "+x+" "+Utils.byteToHex(x)+" "+Utils.byteToBin(x));
        LOG.trace("y \t- "+y+" "+Utils.byteToHex(y)+" "+Utils.byteToBin(y));
        LOG.trace("vX \t- "+vX+" "+Utils.byteToHex(vX)+" "+Utils.byteToBin(vX));
        LOG.trace("vY \t- "+vY+" "+Utils.byteToHex(vY)+" "+Utils.byteToBin(vY));
        LOG.trace("I \t- "+I+" "+Utils.shortToHex(I)+" "+Utils.shortToBin(I));

        LOG.trace("[$"+Utils.shortToHex((short)(program_counter&0xFF-2))+"] Drawing "+n+" bytes of sprites at "+vX+"x"+vY+".");
        LOG.trace("Sprite:");
        for (int i = 0; i < n; i++) {
            LOG.trace("[$"+Utils.shortToHex(I)+"]: "+Utils.byteToBin(readMemory(I + i)));
        }
        byte[] data = new byte[n];
        boolean flag = false;
        for (int i = 0; i < n; i++) {
            if (display.drawSprite(vX, (byte)( (vY + i) & 0xFF), readMemory(I + i))) flag = true;
        }
        display.repaint();
        if (flag) registers[0xF] = 0x1;
        else registers[0xF] = 0x0;
    }

    /*
        FX07	Store the current value of the delay timer in register VX
     */
    public void storeDelayTimeinVX() {
        writeRegister(x, delay_timer);

    }

    /*
     FX0A	    Wait for a keypress and store the result in register VX
     */
    public void wait4KeyStoreInVX() {
        input.lastKeyPressed=17;
        while(input.lastKeyPressed==17){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        writeRegister(x,input.lastKeyPressed);
    }
    /*
        FX15	Set the delay timer to the value of register VX
     */
    public void setDelayTimerToVX() {
        delay_timer = vX;
    }

    /*
       FX18	Set the sound timer to the value of register VX
    */
    public void setSoundTimerToVX() {
        sound_timer = vX;
    }

    /*
        FX1E	Add the value stored in register VX to register I
     */
    public void addVXtoI() {
        setIndex(getIndex() + (vX&0xFF));
    }

    /*
        FX29	Set I to the memory address of the sprite data corresponding to the hexadecimal digit stored in register VX
     */
    public void setItoMemOfSpriteVX() {
        LOG.trace("vX: "+Utils.shortToHex(vX));
        LOG.trace("vX * 5: "+Utils.shortToHex((short)((vX&0xFF) * 5)));
        setIndex((vX&0xFF) * 5);
    }

    /*
        FX33	Store the binary-coded decimal equivalent of the value stored in register VX at addresses I, I+1, and I+2
     */
    public void storeBinaryNumbersofVXinIff() {
        writeMemory(I, (vX & 0xFF) / 100);
        writeMemory(I + 1, ((vX & 0xFF) / 10) % 10);
        writeMemory(I + 2, (vX & 0xFF) % 10);

    }

    /*
       FX55	Store the values of registers V0 to VX inclusive in memory starting at address I
               I is set to I + X + 1 after operation
    */
    public void storeV0toVXinMemAtIff() {
        for (int i = 0; i <= x; i++) {
            writeMemory(I + i, readRegister(i));
        }
        setIndex(getIndex() + x + 1);
    }

    /*
        FX65	Fill registers V0 to VX inclusive with the values stored in memory starting at address I
                I is set to I + X + 1 after operation
     */
    public void fillV0toVXwithValAtIff() {
        for (int i = 0; i <= x; i++) {
            writeRegister(i, readMemory(I + i));
        }
        setIndex(getIndex() + x + 1);
    }

    public void timers() {
        if (delay_timer > 0) delay_timer--;
        if (sound_timer > 0) sound_timer--;
    }


    public byte readMemory(int offset) {
        return readMemory((short) offset);
    }

    public byte readMemory(short offset) {
        return (byte) (memory.ram[offset & 0xFFFF] & 0xFF);
    }

    public void writeMemory(int offset, byte data) {
        writeMemory((short) offset, data);
    }

    private void writeMemory(int offset, int data) {
        writeMemory((short) offset, (byte) data);
    }

    public void writeMemory(short offset, byte data) {
        memory.ram[offset & 0xFFFF] = (byte) (data & 0xFF);
    }

    private void writeMemory(short offset, int data) {
        writeMemory(offset, (byte) data);
    }

    public byte readRegister(int nr) {
        return readRegister((byte) nr);
    }

    public byte readRegister(short nr) {
        return readRegister((byte) nr);
    }

    public byte readRegister(byte nr) {
        return (byte) (registers[nr & 0xFF] & 0xFF);
    }

    public void writeRegister(int nr, byte data) {
        writeRegister((byte) nr, data);
    }
    public void writeRegister(int nr, int data){
        writeRegister((byte)nr,(byte)data);
    }
    public void writeRegister(short nr, byte data) {
        writeRegister((byte) nr, data);
    }

    public void writeRegister(byte nr, int data) {
        writeRegister(nr, (byte) data);
    }

    public void writeRegister(byte nr, byte data) {
        registers[nr & 0xFF] = (byte) (data & 0xFF);
    }

    public short getIndex() {
        return (short) (I & 0xFFFF);
    }

    public void setIndex(short num) {
        I = (short) (num & 0xFFFF);
    }

    public void setIndex(int num) {
        setIndex((short) num);
    }


}
