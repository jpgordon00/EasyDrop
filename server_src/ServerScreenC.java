import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/**
 * @author Jacob Gordon
 * @version 1.0
 * @date 7/7/19
 **/
public class ServerScreenC {

    /**
     * TextArea that contains all the text for the com.net.server to log.
     */
    @FXML
    public TextArea textArea;

    /**
     * Function that's called when the view is created.
     * Set the TextArea to be not editable.
     */
    @FXML
    private void initialize() {
        textArea.setEditable(false);
    }
}
