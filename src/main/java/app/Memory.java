package app;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by bers on 6/22/16.
 */
public class Memory {
    static Logger LOG = Logger.getLogger(Memory.class);

    public byte[] ram;
    final short MEMSIZE = 4048;
    public final short INSERT_ROM_ADRESS = 0x200;

    //hexadecimal sprite data
    private byte[] sprite_data = new byte[] {
            (byte)0xF0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xF0, //0, address = $000
            (byte)0x20, (byte)0x60, (byte)0x20, (byte)0x20, (byte)0x70, //1  address = $005
            (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x80, (byte)0xF0, //2  address = $00A
            (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x10, (byte)0xF0, //3  address = $00F
            (byte)0x90, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0x10, //4  address = $014
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x10, (byte)0xF0, //5  address = $019
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x90, (byte)0xF0, //6  address = $01E
            (byte)0xF0, (byte)0x10, (byte)0x20, (byte)0x40, (byte)0x40, //7  address = $023
            (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0xF0, //8  address = $028
            (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0xF0, //9  address = $02D
            (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0x90, //A  address = $032
            (byte)0xE0, (byte)0x90, (byte)0xE0, (byte)0x90, (byte)0xE0, //B  address = $037
            (byte)0xF0, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xF0, //C  address = $03C
            (byte)0xE0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xE0, //D  address = $041
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0xF0, //E  address = $046
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0x80  //F  address = $04B

    };


    /**
     * Loads the ROM file into Memory.
     *
     * @param f
     */
    public void loadROM(File f) throws IOException {

        if(f.length()>(MEMSIZE-INSERT_ROM_ADRESS)){
            LOG.fatal("ROM too big. Maximum allowed size: "+(MEMSIZE-INSERT_ROM_ADRESS)+" bytes Rom size: "+f.length()+" bytes.");
            System.exit(1);
        }
        byte[] rom = Files.readAllBytes(f.toPath());
        LOG.info("ROM loaded. ("+rom.length+" bytes)");
        //Load ROM file into Memory starting at 0x200
        short offset = INSERT_ROM_ADRESS;
        for(int i = 0; i < rom.length; i++){
            ram[offset++]=rom[i];
        }
    }

    public void initialize() {
        ram = new byte[MEMSIZE];

        for (int i = 0; i < ram.length; i++) {
            ram[i]=0;
        }
        //Load the sprite data into the RAM
        for (int i = 0; i < sprite_data.length; i++) {
            ram[i]=(byte)(sprite_data[i] & 0xFF);
        }
    }
}
