/**
 * This item represents the data to be displayed in the receive screen table.
 *
 * @author Jacob Gordon
 * @version 1.0
 * @date 15 Jul 2019
 **/
public class TableItem {

    private String name;
    private String type;
    private String size;
    private String time;


    public String uid;

    public TableItem() {
        name = "";
        type = "";
        size = "";
        time = "";
    }

    public TableItem(String name, String type, String size, String time) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean hasField(String str) {
        return name.toLowerCase().startsWith(str) || type.toLowerCase().startsWith(str) || size.toLowerCase().startsWith(str) || time.toLowerCase().startsWith(str);
    }
}
