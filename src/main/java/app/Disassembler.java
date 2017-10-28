package app;

/**
 * Created by Bers on 25.06.2016.
 */
public class Disassembler {

    private CPU cpu;
    public Disassembler(CPU cpu) {
        this.cpu=cpu;
    }
    public void start(){
            //Go through ram, starting at 0x200 and execute the opcodes
            cpu.program_counter = cpu.memory.INSERT_ROM_ADRESS;

            String comment = "";
            short opcode;

            while(true){
                opcode = (short)(((cpu.memory.ram[cpu.program_counter] & 0xFF)<<8) | (cpu.memory.ram[cpu.program_counter+1] & 0xFF));
                if(opcode==0)System.exit(0);
                comment="";
                switch(opcode & 0xF000){
                    case 0x0000:
                        switch(opcode & 0x00FF){
                            case 0xE0:
                                comment = "00E0\tClear the screen";
                                break;
                            case 0xEE:
                                comment = "00EE\tReturn from a subroutine";
                                break;
                            default:
                                break;
                        }
                        break;
                    case 0x1000:
                        comment = "1NNN\tJump to address NNN";
                        break;
                    case  0x2000:
                        comment = "2NNN\tExecute subroutine starting at address NNN";
                        break;
                    case 0x3000:
                        comment = "3XNN\tSkip the following instruction if the value of register VX equals NN";
                        break;
                    case 0x4000:
                        comment = "4XNN\tSkip the following instruction if the value of register VX is not equal to NN";
                        break;
                    case 0x5000:
                        comment = "5XY0\tSkip the following instruction if the value of register VX is equal to the value of register VY";
                        break;
                    case 0x6000:
                        comment = "6XNN\tStore number NN in register VX";
                        break;
                    case 0x7000:
                        comment = "7XNN\tAdd the value NN to register VX";
                        break;
                    case 0x8000:
                        switch(opcode & 0x000F){
                            case 0:
                                comment = "8XY0\tStore the value of register VY in register VX";
                                break;
                            case 1:
                                comment = "8XY1\tSet VX to VX OR VY";
                                break;
                            case 2:
                                comment = "8XY2\tSet VX to VX AND VY";
                                break;
                            case 3:
                                comment = "8XY3\tSet VX to VX XOR VY";
                                break;
                            case 4:
                                comment = "8XY4\tAdd the value of register VY to register VX";
                                break;
                            case 5:
                                comment = "8XY5\tSubtract the value of register VY from register VX";
                                break;
                            case 6:
                                comment = "8XY6\tStore the value of register VY shifted right one bit in register VX";
                                break;
                            case 7:
                                comment = "8XY7\tSet register VX to the value of VY minus VX";
                                break;
                            case 0xE:
                                comment = "8XYE\tStore the value of register VY shifted left one bit in register VX";
                                break;
                        }
                        break;
                    case 0x9000:
                        comment = "9XY0\tSkip the following instruction if the value of register VX is not equal to the value of register VY";
                        break;
                    case 0xA000:
                        comment = "ANNN\tStore memory address NNN in register I";
                        break;
                    case 0xB000:
                        comment = "BNNN\tJump to address NNN + V0";
                        break;
                    case 0xC000:
                        comment = "CXNN\tSet VX to a random number with a mask of NN";
                        break;
                    case 0xD000:
                        comment = "DXYN\tDraw a sprite at position VX, VY with N bytes of sprite data starting at the address stored in I";
                        break;
                    case 0xE000:
                        switch(opcode & 0x000F){
                            case 0xE:
                                comment = "EX9E\tSkip the following instruction if the key corresponding to the hex value currently stored in register VX is pressed";
                                break;
                            case 0x1:
                                comment = "EXA1\tSkip the following instruction if the key corresponding to the hex value currently stored in register VX is not pressed";
                                break;
                        }
                        break;
                    case 0xF000:
                        switch(opcode & 0x00FF){
                            case 0x07:
                                comment = "FX07\tStore the current value of the delay timer in register VX";
                                break;
                            case 0x0A:
                                comment =  "FX0A\tWait for a keypress and store the result in register VX";
                                break;
                            case 0x15:
                                comment = "FX15\tSet the delay timer to the value of register VX";
                                break;
                            case 0x18:
                                comment = "FX18\tSet the sound timer to the value of register VX";
                                break;
                            case 0x1E:
                                comment = "FX1E\tAdd the value stored in register VX to register I";
                                break;
                            case 0x29:
                                comment = "FX29\tSet I to the memory address of the sprite data corresponding to the hexadecimal digit stored in register VX";
                                break;
                            case 0x33:
                                comment = "FX33\tStore the binary-coded decimal equivalent of the value stored in register VX at addresses I, I+1, and I+2";
                                break;
                            case 0x55:
                                comment = "FX55\tStore the values of registers V0 to VX inclusive in memory starting at address I";
                                break;
                            case 0x65:
                                comment = "FX65\tFill registers V0 to VX inclusive with the values stored in memory starting at address I";
                                break;
                        }
                        break;

                }
                System.out.printf("[$%03X]: %02X%02X // %s\n", cpu.program_counter,cpu.memory.ram[cpu.program_counter],cpu.memory.ram[cpu.program_counter +1],comment);
                cpu.program_counter += 2;

            }


    }
}
