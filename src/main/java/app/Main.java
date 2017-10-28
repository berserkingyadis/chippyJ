package app;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Main {
    static Logger LOG = Logger.getLogger(Main.class);
    static final String DISASSEMBLE = "--disassemble";
    static final String DISASSEMBLE_S = "-d";

    static final String CHOOSE_ROM = "--filechooser";
    static final String CHOOSE_ROM_S = "-f";

    static final String HELP = "--help";
    static final String HELP_S = "-h";

    static final String VERSION = "--version";
    static final String VERSION_S = "-v";
    static {
        PropertyConfigurator.configure(Main.class.getResource("/config/log4j.properties"));
    }


    public final static String NAME = "chippyJ";
    public final static String VERSIONNR = "0.42";

    private final static String[] VALID_ROM_EXTENSIONS = new String[]{
            "c8",
            "ch8",
            "chip8",
            "chip-8",
            "rom"
    };

    public static void main(String[] args) {
        LOG.info("Started "+NAME+" v"+VERSIONNR);

        File rom = null;

        boolean disassemble = false;
        boolean choose = false;

        // Load ROM-File
        for(String arg : args){
            switch(arg){
                case CHOOSE_ROM:
                case CHOOSE_ROM_S:
                    choose = true;
                    break;
                case DISASSEMBLE:
                case DISASSEMBLE_S:
                    disassemble= true;
                    break;
                case HELP:
                case HELP_S:
                    printHelp();
                    System.exit(0);
                case VERSION:
                case VERSION_S:
                    printVersion();
                    System.exit(0);
            }
        }
        if(choose){
            JFileChooser chooser =new JFileChooser("src/main/resources/roms");
            chooser.showDialog(null,"Select a rom");
            rom = chooser.getSelectedFile();
        }else{
            rom = new File(args[args.length-1]);

        }
        //Initialize Screen with X times the resolution to acually see something
        Display display = new Display(10);

        //Create Input Listener
        Input input =new Input();

        //Initialize Memory
        Memory memory = new Memory();

        //Inizialize the CPU
        CPU cpu = new CPU(memory,display,input);

        //Load ROM-File into Memory
        try {
            memory.loadROM(rom);
        } catch (IOException e) {
            LOG.fatal("Could not read rom! Exiting ...",e);
            System.exit(1);
        }
        if(!disassemble)cpu.start();
        else new Disassembler(cpu).start();
    }


    static boolean checkRomName(String name){
        for(String extension: VALID_ROM_EXTENSIONS){
            if(name.endsWith("."+extension))return true;
        }
        return false;
    }
    static void printHelp(){
        System.out.println(NAME+" Help:");
        System.out.println("To run the emulator, use the following command:");
        System.out.println("java -jar <jarfile> <path to chip-8 rom>");
        System.out.println();
    }
    static void printVersion(){
        System.out.println(NAME+" version: "+VERSIONNR);
    }
}
