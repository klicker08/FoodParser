import java.util.ArrayList;

public class Station {
    ArrayList<FoodItem> menuItems;
    String name;
    int id;

    public Station(String name, int id) {
        this.name = name;
        this.id = id;

        this.menuItems = new ArrayList<FoodItem>();
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void addItem(FoodItem item) {
        this.menuItems.add(item);
    }

    public ArrayList<FoodItem> getMenuItems() {
        return this.menuItems;
    }
}
