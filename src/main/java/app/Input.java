package app;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Created by Bers on 23.06.2016.
 */
public class Input implements KeyListener{


    int[] keymap = {
            KeyEvent.VK_X,  //0
            KeyEvent.VK_1,  //1
            KeyEvent.VK_2,  //2
            KeyEvent.VK_3,  //3
            KeyEvent.VK_Q,  //4
            KeyEvent.VK_W,  //5
            KeyEvent.VK_E,  //6
            KeyEvent.VK_A,  //7
            KeyEvent.VK_S,  //8
            KeyEvent.VK_D,  //9
            KeyEvent.VK_A,  //A
            KeyEvent.VK_C,  //B
            KeyEvent.VK_4,  //C
            KeyEvent.VK_R,  //D
            KeyEvent.VK_F,  //E
            KeyEvent.VK_V   //F
    };

    public int lastKeyPressed;

    @Override
    public void keyPressed(KeyEvent e) {
        for (int i = 0; i < keymap.length; i++) {
            if(e.getKeyCode()==keymap[i]){
                lastKeyPressed=i;
            }
        }
    }

    // ----- NOT NEEDED -----
    @Override
    public void keyTyped(KeyEvent e) {

    }
    @Override
    public void keyReleased(KeyEvent e) {

    }
}
