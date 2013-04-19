/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

/**
 *
 * @author Mihai Pîțu
 */
public class InvalidOperationException extends Exception {
    
    public InvalidOperationException() {
        super();
    }
    
    public InvalidOperationException(String exception) {
        super(exception);
    }
}