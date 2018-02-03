
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author walid-wam
 */
public class Lancer {
    public static void main(String args[]) throws IOException { 
        
            int nbsite=10;
            for(int i=0;i<nbsite;i++)
            Runtime.getRuntime().exec("cmd /c cd build & cd classes & java Main "+i);  
}
}
