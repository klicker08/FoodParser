import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Set;

import org.json.*;

public class GetRequest {
    //Date must follow format year/month/day exactly
    String year;
    String month;
    String day;
    //Either breakfast, lunch, or dinner
    String meal;

    /**
     * Valid locations:
     * four-lakes-market
     * carsons-market
     * lizs-market
     * gordon-avenue-market
     * rhetas-market
     * lowell-market
     */
    String location;
    ArrayList<FoodItem> menuItems;
    ArrayList<Station> stations;

    public GetRequest(String year, String month, String day, String meal, String location) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.meal = meal;
        this.location = location;

        HttpResponse<String> response = makeRequest();

        if (response.statusCode() == 200) {
            menuItems = parseMenu(response);
            assignStations();
        } else {
            throw new RuntimeException("Network Error");
        }
    }

    private HttpResponse<String> makeRequest() {
        String date = this.year + "/" + this.month + "/" + this.day;

        String requestURI = "https://wisc-housingdining.api.nutrislice.com/menu/api/weeks/school/"
                + this.location + "/menu-type/" + this.meal + "/" + date + "/";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestURI))
                .build();

        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<FoodItem> parseMenu(HttpResponse<String> response) {
        String date = this.year + "-" + this.month + "-" + this.day;

        JSONObject JSON = new JSONObject(response.body());
        JSONArray daysArray = JSON.getJSONArray("days");

        JSONObject day = getCorrectDay(date, daysArray);
        JSONObject menu_info = day.getJSONObject("menu_info");
        JSONArray menuItems = day.getJSONArray("menu_items");

        this.stations = parseStations(menu_info);
        ArrayList<FoodItem> foodItems= parseMenuItems(menuItems);

        return foodItems;
    }

    private JSONObject getCorrectDay(String date, JSONArray days) {
        for (int i = 0; i < days.length(); i++) {
            if (days.getJSONObject(i).get("date").equals(date)) {
                return days.getJSONObject(i);
            }
        }

        throw new RuntimeException("Date Error");
    }

    private ArrayList<FoodItem> parseMenuItems (JSONArray menuItems) {
        ArrayList<FoodItem> foundFood = new ArrayList<>();

        for (int i = 0; i < menuItems.length(); i++) {
            if (menuItems.getJSONObject(i).isNull("food")) {
                continue;
            } else {
                JSONObject foodItem = menuItems.getJSONObject(i).getJSONObject("food");
                JSONObject nutrition = foodItem.getJSONObject("rounded_nutrition_info");

                double calories = nutrition.optDouble("calories", 0.0); // Default value as 0.0
                double gFat = nutrition.optDouble("g_fat", 0.0);
                double gCarbs = nutrition.optDouble("g_carbs", 0.0);
                double mgSodium = nutrition.optDouble("mg_sodium", 0.0);
                double gProtein = nutrition.optDouble("g_protein", 0.0);

                FoodItem item = new FoodItem(foodItem.getString("name"), calories, gFat, gCarbs, mgSodium, gProtein);

                item.setStation(menuItems.getJSONObject(i).getInt("menu_id"));

                foundFood.add(item);
            }
        }
        return foundFood;
    }

    private ArrayList<Station> parseStations(JSONObject menu_info) {
        Set<String> ids = menu_info.keySet();
        ArrayList<Station> stationsList = new ArrayList<Station>();

        for (String id : ids) {
            if (menu_info.get(id) instanceof JSONObject) {
                JSONObject tempStation = menu_info.getJSONObject(id);
                JSONObject section_options = tempStation.getJSONObject("section_options");
                String name = section_options.getString("display_name");
                int id_temp = Integer.parseInt(id);

                Station testStation = new Station(name, id_temp);
                stationsList.add(testStation);
            }

        }

        return stationsList;
    }

    private void assignStations() {
        for (FoodItem item : this.menuItems) {
            int id = item.getStation();

            for (Station station : this.stations) {
                if (station.getId() == id) {
                    station.addItem(item);
                }
            }
        }
    }

    public ArrayList<FoodItem> getMenuItems() {
        return this.menuItems;
    }

    public ArrayList<Station> getStations() {
        return stations;
    }


}
