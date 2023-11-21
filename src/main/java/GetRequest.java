import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.*;

public class GetRequest {
    //Date must follow format year/month/day exactly
    String year;
    String month;
    String day;
    //Either breakfast, lunch, or dinner
    String meal;
    ArrayList<FoodItem> menuItems;

    public GetRequest(String year, String month, String day, String meal) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.meal = meal;

        HttpResponse<String> response = makeRequest();

        if (response.statusCode() == 200) {
            menuItems = parseMenu(response);
        } else {
            throw new RuntimeException("Network Error");
        }
    }

    private HttpResponse<String> makeRequest() {
        String date = this.year + "/" + this.month + "/" + this.day;

        String requestURI = "https://wisc-housingdining.api.nutrislice.com/menu/api/weeks/school/gordon-avenue-market/menu-type/"
                + this.meal + "/" + date + "/";
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
        JSONArray daysArray = new JSONArray(JSON.getJSONArray("days"));

        JSONObject day = getCorrectDay(date, daysArray);
        JSONArray menuItems = new JSONArray(day.getJSONArray("menu_items"));

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

                foundFood.add(item);
            }
        }
        return foundFood;
    }

    public ArrayList<FoodItem> getMenuItems() {
        return this.menuItems;
    }


    public static void main(String[] args) {

        GetRequest request = new GetRequest("2023", "11", "21", "lunch");
        ArrayList<FoodItem> temp = request.getMenuItems();
        for (int i = 0; i < temp.size(); i++) {
            System.out.println(Arrays.toString(temp.get(i).getNutrition()));
        }



        /**
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://wisc-housingdining.api.nutrislice.com/menu/api/weeks/school/gordon-avenue-market/menu-type/lunch/2023/11/05/"))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
               JSONObject json = new JSONObject(response.body());
               JSONArray days = new JSONArray(json.getJSONArray("days"));
               JSONObject dayZero = days.getJSONObject(0);
               JSONArray menuItems = new JSONArray(dayZero.getJSONArray("menu_items"));

               System.out.println(menuItems.getJSONObject(0).get("text"));
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        */
    }
}
