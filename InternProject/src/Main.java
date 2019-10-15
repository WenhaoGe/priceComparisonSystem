import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import netscape.javascript.JSObject;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.swing.text.Document;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.mongodb.client.model.Filters.eq;


public class Main {

    static MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
    static MongoDatabase database = mongoClient.getDatabase("DB1");
    static MongoCollection<org.bson.Document> collection = database.getCollection("customer");

    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver", "E:\\chromedriver\\chromedriver_win32\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        String baseUrl = "http://www.amazon.com";

        driver.get(baseUrl);
        WebElement searchField = driver.findElement(By.xpath("//*[@id=\"twotabsearchtextbox\"]"));
        searchField.sendKeys("shoes");
        searchField.sendKeys(Keys.ENTER);
        WebElement Bigpage = driver.findElement(By.xpath("//*[@id=\"search\"]/div[1]/div[2]/div/span[7]/div/div/div/ul"));
        List<WebElement> pages = Bigpage.findElements(By.tagName("a"));
        String parentHandle = null;
        //for(int z = 2; z>=0; z--) {
            pages.get(2).click();
            WebElement Bigdiv = driver.findElement(By.xpath("//*[@id=\"search\"]/div[1]/div[2]/div/span[3]/div[1]"));

            List<WebElement> images = Bigdiv.findElements(By.tagName("img"));
            Actions actions = new Actions(driver);
            parentHandle = driver.getWindowHandle();
            for (int i = 0; i < images.size(); i++) {
            System.out.println("i: " + i);
            String url = "";

            if(images.get(i).getAttribute("src").equals("https://m.media-amazon.com/images/I/91tcoLK-VcL._AC_UL436_.jpg")) {continue;}
            // get the url of the product image
            try {
                url = images.get(i).getAttribute("src");
            } catch(org.openqa.selenium.StaleElementReferenceException e) {}
                actions.keyDown(Keys.CONTROL).click(images.get(i)).keyUp(Keys.CONTROL).build().perform();
                for (String winHandle : driver.getWindowHandles()) {
                    driver.switchTo().window(winHandle);
                }
            WebDriverWait wait = new WebDriverWait(driver, 15);
            WebElement name = null;
            String nameVal = "";
            String ASIN = "";
            WebElement info = null;
            List<WebElement> lists = null;
            WebElement pictures = null;
            List<WebElement> Allpictures = null;
            HashMap<String, String> map1 = new HashMap<>();
            HashMap<String, List<String>> map2 = new HashMap<>();
            String price = "";
            List<WebElement> sizes = null;
            List<String> listSizes = null;
            String color = null;

            try {
                info = driver.findElement(By.xpath("//*[@id=\"detailBullets_feature_div\"]/ul"));
                lists = info.findElements(By.tagName("li"));
                for (int j = 0; j < lists.size(); j++) {
                    List<WebElement> spans = lists.get(j).findElements(By.tagName("span"));
                    for (int k = 0; k < spans.size(); k++) {
                        if (spans.get(k).getText().equals("ASIN:")) {
                            ASIN = spans.get(k + 1).getText();
                        }
                    }
                }
                name = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"productTitle\"]")));
                nameVal = name.getText().trim();

                pictures = driver.findElement(By.xpath("//*[@id=\"variation_color_name\"]/ul"));
                Allpictures = pictures.findElements(By.tagName("li"));
                // get product price
                price = driver.findElement(By.xpath("//*[@id=\"priceblock_ourprice_row\"]/td[2]/span")).getText();
                for (WebElement picture : Allpictures) {
                    picture.click();

                    driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
                    color = driver.findElement(By.xpath("//*[@id=\"variation_color_name\"]/div/span")).getText();

                    driver.findElement(By.xpath("//*[@id=\"dropdown_selected_size_name\"]/span")).click();

                    WebElement Allsize = driver.findElement(By.xpath("//*[@id=\"native_dropdown_selected_size_name\"]"));
                    //get all the sizes of a color of shoe
                    sizes = Allsize.findElements(By.tagName("option"));
                    listSizes = new ArrayList<>();
                    for (int j = 1; j < sizes.size(); j++) {
                        String valueOfsize = sizes.get(j).getText().trim();
                        String available = sizes.get(j).getAttribute("class");
                        if (available.equals("dropdownAvailable")) {
                            listSizes.add(valueOfsize);
                        }
                    }
                    map2.put(color, listSizes);
                    map1.put(color, price);
                }
            } catch (Exception e) {
                driver.close();
                driver.switchTo().window(parentHandle);
                continue;
            }
                WriteDatatoMongo(nameVal, price, url, map1, map2, ASIN);
                driver.close();
                driver.switchTo().window(parentHandle);
            }
        //}
        driver.close();
        driver.switchTo().window(parentHandle);

    }
    private static boolean samePriceOrNot(String asin, String price) throws JSONException{
        FindIterable<org.bson.Document> lists = collection.find(eq("asin", asin));
        for(org.bson.Document list: lists) {
            JSONObject object = null;

            object = new JSONObject(list.toJson());


            if(object.get("price").toString().equals(price)) {
                return true;
            }
            else {
                return false;
            }
        }
        return true;
    }
    private static void Mongo() throws JSONException {
        FindIterable<org.bson.Document> lists = collection.find(eq("asin","B07H86YWKF"));
        //FindIterable<org.bson.Document> doc = collection.find();
        for(org.bson.Document d: lists) {
            JSONObject object = new JSONObject(d.toJson());
            String firstname = object.get("firstname").toString();
            collection.updateMany(
                    Filters.eq("firstname", "hao"),
                    Updates.combine(
                            Updates.set("firstname", "W"),
                            Updates.set("firstname", "P")
                    ));
        }

       /*for(org.bson.Document d: lists) {
            JSONObject object = new JSONObject(d.toJson());
            object.get("asin")
            //System.out.println(object.getJSONObject("colorPrice").names().get(1));

            //Object colorPrice = object.get("colorPrice");
            //JSONObject newObject = new JSONObject(colorPrice);
            //System.out.println(newObject);

            //System.out.println(colorPrice.getString("8008all Black"));

        }*/
    }

    private static void WriteDatatoMongo(String name, String price, String url, HashMap<String, String> map1, HashMap<String,List<String>> map2, String ASIN) {

        Calendar cal = Calendar.getInstance();
        Date date=cal.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
        String formattedDate=dateFormat.format(date);

        try{
            // 连接到 mongodb 服务
            org.bson.Document document = null;
            document = new org.bson.Document("name", name).
                    append("url", url).
                    append("price", price).
                    append("colorSizes", map2).
                    append("colorPrice", map1).
                    append("asin", ASIN).
                    append("Date", formattedDate);

            collection.insertOne(document);
            System.out.println("文档插入成功");
        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }
}

