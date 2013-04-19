/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

/**
 *
 * @author Mihai Pîțu
 */
public class InvalidParameterException extends Exception {
    
    public InvalidParameterException() {
        super();
    }
    
    public InvalidParameterException(String exception) {
        super(exception);
    }
}
