package com.company;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import java.sql.*;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.text.Document;
import org.apache.commons.exec.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;  // WebDriver is Java interface
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Main {

    static MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
    static MongoDatabase mongoDatabase = mongoClient.getDatabase("DB1");
    static MongoCollection<org.bson.Document> collection = mongoDatabase.getCollection("data");
    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        System.setProperty("webdriver.chrome.driver", "E:\\chromedriver\\chromedriver_win32\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        String baseUrl = "http://www.amazon.com";

        driver.get(baseUrl);
        WebElement searchField = driver.findElement(By.xpath("//*[@id=\"twotabsearchtextbox\"]"));
        searchField.sendKeys("shoes");
        searchField.sendKeys(Keys.ENTER);
        WebElement Bigpage = driver.findElement(By.xpath("//*[@id=\"search\"]/div[1]/div[2]/div/span[7]/div/div/div/ul"));
        List<WebElement> pages = Bigpage.findElements(By.tagName("a"));
        pages.get(2).click();
        WebElement Bigdiv = driver.findElement(By.xpath("//*[@id=\"search\"]/div[1]/div[2]/div/span[3]/div[1]"));

        List<WebElement> images = Bigdiv.findElements(By.tagName("img"));
        Actions actions = new Actions(driver);

        String parentHandle = driver.getWindowHandle();
        for(int i = 46;i<images.size();i++) {
            System.out.println("i: " + i);
            String url = "";
            // get the url of the product image
            actions.keyDown(Keys.CONTROL).click(images.get(i)).keyUp(Keys.CONTROL).build().perform();
            for(String winHandle : driver.getWindowHandles()){
                driver.switchTo().window(winHandle);
            }
            try {
                url = images.get(i).getAttribute("src");
            } catch(org.openqa.selenium.StaleElementReferenceException e) {}

            // get the product name
            WebDriverWait wait = new WebDriverWait(driver, 15);
            WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"productTitle\"]")));
            String nameVal = name.getText().trim();
            // get product ASIN
            String ASIN = "";
            WebElement info = null;
            try {
                info = driver.findElement(By.xpath("//*[@id=\"detailBullets_feature_div\"]/ul"));
            } catch(org.openqa.selenium.NoSuchElementException e) {
                driver.close();
                driver.switchTo().window(parentHandle);

                continue;
            }
            List<WebElement> lists = info.findElements(By.tagName("li"));
            for(int j = 0; j < lists.size();j++) {
                List<WebElement> spans = lists.get(j).findElements(By.tagName("span"));
                for(int k = 0; k < spans.size();k++) {
                    if(spans.get(k).getText().equals("ASIN:")) {
                        ASIN = spans.get(k+1).getText();
                    }
                }
            }

            WebElement pictures = null;
            try {
                pictures = driver.findElement(By.xpath("//*[@id=\"variation_color_name\"]/ul"));
            } catch(org.openqa.selenium.NoSuchElementException e) {
                driver.close();
                driver.switchTo().window(parentHandle);
                continue;
            }
            List<WebElement> Allpictures = pictures.findElements(By.tagName("li"));

            HashMap<String, String> map1 = new HashMap<>();
            HashMap<String, List<String>> map2 = new HashMap<>();
            // get product price
            String price = "";
            try {
                price = driver.findElement(By.xpath("//*[@id=\"priceblock_ourprice_row\"]/td[2]/span")).getText();
            } catch(org.openqa.selenium.NoSuchElementException e) {
                driver.close();
                driver.switchTo().window(parentHandle);
                continue;
            }
            for(WebElement picture: Allpictures) {
                try {
                    picture.click();
                } catch(Exception e) {
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    continue;
                }
                driver.manage().timeouts().implicitlyWait(2,TimeUnit.SECONDS);
                String color = driver.findElement(By.xpath("//*[@id=\"variation_color_name\"]/div/span")).getText();

                try {
                    driver.findElement(By.xpath("//*[@id=\"dropdown_selected_size_name\"]/span")).click();
                } catch(Exception e) {
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    continue;
                }

                WebElement Allsize = driver.findElement(By.xpath("//*[@id=\"native_dropdown_selected_size_name\"]"));
                //get all the sizes of a color of shoe
                List<WebElement> sizes = Allsize.findElements(By.tagName("option"));
                List<String> listSizes = new ArrayList<>();
                for(int j = 1; j< sizes.size();j++) {
                    String valueOfsize = sizes.get(j).getText().trim();
                    String available = sizes.get(j).getAttribute("class");
                    if(available.equals("dropdownAvailable")) {
                        listSizes.add(valueOfsize);
                    }
                }
                map2.put(color, listSizes);
                map1.put(color,price);
            }

            WriteDatatoMongo(nameVal, url, map1, map2, ASIN);
            driver.close();
            driver.switchTo().window(parentHandle);
        }
        driver.close();
        driver.switchTo().window(parentHandle);
    }

    public static void WriteDatatoMysql() {

        // MySQL 8.0 以下版本 - JDBC 驱动名及数据库 URL
        final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3307/shoes?useSSL=false&serverTimezone=UTC";
        final String USER = "root";
        final String PASS = "@gewenhaoMYSQL2019";
        Connection conn = null;
        Statement stmt = null;
        try{
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);


            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);


            System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT id, name, url FROM websites";
            ResultSet rs = stmt.executeQuery(sql);


            while(rs.next()){

                int id  = rs.getInt("id");
                String name = rs.getString("name");
                String url = rs.getString("url");


                System.out.print("ID: " + id);
                System.out.print(", 站点名称: " + name);
                System.out.print(", 站点 URL: " + url);
                System.out.print("\n");
            }

            rs.close();
            stmt.close();
            conn.close();
        }catch(SQLException se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }catch(Exception e){
            // 处理 Class.forName 错误
            e.printStackTrace();
        }finally{

            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException se2){
            }
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
    }
    public static void WriteDatatoMongo(String name, String url, HashMap<String, String> map1, HashMap<String,List<String>> map2, String ASIN) {
        try{
            // 连接到 mongodb 服务
            //MongoClient mongoClient = new MongoClient( "localhost" , 27017 );

            //MongoDatabase mongoDatabase = mongoClient.getDatabase("DB1");
            //MongoCollection<org.bson.Document> collection = mongoDatabase.getCollection("data");
            org.bson.Document document = new org.bson.Document("product name", name).
                    append("product photo", url).
                    append("Color-price", map1).
                    append("color-sizes", map2).
                    append("ASIN", ASIN);
            List<org.bson.Document> documents = new ArrayList<org.bson.Document>();
            documents.add(document);
            collection.insertMany(documents);
            System.out.println("文档插入成功");
        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }
}
