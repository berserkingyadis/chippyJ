package app;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Created by bers on 6/22/16.
 */
public class Display {
    static Logger LOG = Logger.getLogger(Display.class);

    public static final int WIDTH = 64;
    public static final int HEIGHT = 32;

    public int scale = 1;

    public JFrame jframe = null;
    // screen-array
    // the color is either white or black
    // black=false
    // white=true
    public boolean screen[][];

    public Display(int scale){
        screen = new boolean[WIDTH][HEIGHT];
        this.scale = scale;

        jframe = new JFrame();
        jframe.setSize(new Dimension(WIDTH*scale,HEIGHT*scale));
        jframe.setTitle(Main.NAME+" "+Main.VERSION);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.add(new JPanel(){
            //Paint the Screen
            @Override
            public void paintComponent(Graphics g){
                g.setColor(Color.BLACK);
                g.fillRect(0,0,64*scale,32*scale);
                g.setColor(Color.WHITE);
                for(int w = 0; w < 64; w++){
                    for(int h = 0; h < 32; h++){
                        if(screen[w][h])g.fillRect(w*scale,h*scale,scale,scale);
                    }
                }
            }
        });
        jframe.setVisible(true);
        LOG.info(WIDTH+"x"+HEIGHT+" monochrome screen with display resolution "
                +jframe.getSize().width+"x"+jframe.getSize().height+" initialized.");
    }

    public void registerInput(Input input){
        jframe.addKeyListener(input);
    }

    public void repaint(){
        jframe.repaint();
    }
    public void flush(){
        for(int w = 0; w < 64; w++){
            for(int h = 0; h < 32; h++){
               screen[w][h]=false;
            }
        }
    }
    
    // Draw the sprite from data at the position x and y and check if a white pixel has been erased.
    public boolean drawSprite(byte x, byte y, byte data) {
        LOG.trace("drawSprite");
        LOG.trace("x \t- "+x+" "+Utils.byteToHex(x)+" "+Utils.byteToBin(x));
        LOG.trace("y \t- "+y+" "+Utils.byteToHex(y)+" "+Utils.byteToBin(y));
        LOG.trace("data \t- "+Utils.byteToHex(data)+" "+Utils.byteToBin(data));

        boolean erased = false;

        byte mask = 0x1;
        for (int i = 7; i >= 0; i--) {
            LOG.trace("i ="+i);
            LOG.trace("data = \t"+Utils.byteToBin(data));
            LOG.trace("mask = \t"+Utils.byteToBin(mask));
            LOG.trace("--------------------");
            LOG.trace("resu = \t"+Utils.byteToBin((byte)(mask & (data&0xFF))));

            if((mask & (data&0xFF))>0){
                LOG.trace("Mask matches!");
                boolean pixel = screen[(((x+i)%WIDTH)&0xFF)][y];
                LOG.trace("Pixel:"+pixel);
                if(pixel)erased=true;
                screen[(x+i)%WIDTH][y]^=true;
            }
            mask<<=1;
        }
        return erased;
    }


}
