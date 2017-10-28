package app;

import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Created by Bers on 25.06.2016.
 */
public class Utils {
    static int a = 100;
    public static void main(String[] args) throws InterruptedException {

        new Thread(()->{
            while(true){
                try {
                    Thread.sleep(1000/6);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(a>0)a-=10;
            }
        }).start();

        while(true){
            System.out.println(a);
            Thread.sleep(1000/6);
        }


    }
    static Logger LOG = Logger.getLogger(Utils.class);


    public static void printAsBinary(short b){
        System.out.println(shortToBin(b));
    }
    public static void printAsBinary(byte b){
        System.out.println(byteToBin(b));
    }
    public static String byteToBin(byte b){
        return String.format("0b%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }
    public static String shortToBin(short b){
        return String.format("0b%16s", Integer.toBinaryString(b & 0xFFFF)).replace(' ', '0');
    }

    public static String intToHex(int i){
        return String.format("0x%08X",(i));
    }
    public static String shortToHex(short b){
        return String.format("0x%04X",(b & 0xFFFF));
    }
    public static String byteToHex(byte b){
        return String.format("0x%02X",(b & 0xFF));
    }


    public static byte getX(int num){
        return getNibble(num, 2);
    }
    public static byte getY(int num){
        return getNibble(num, 1);
    }
    public static byte getN(int num) {
        return getNibble(num, 0);
    }

    public static byte getNibble(int num, int pos){
        return (byte)(num >> (4*pos) & 0xF);
    }

    public static byte getNN(int num){
        return (byte)(num & 0x00FF);
    }
    public static short getNNN(int num){
        return (short)(num & 0x0FFF);
    }



}
