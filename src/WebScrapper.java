import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class WebScrapper {
    private final int AMAZON_COM = 0;
    private final int AMAZON_CO_UK = 1;
    private final int EBAY_COM = 2;

    private String searchTerm = "laser+printer";

    private String url_Amazon_com = "https://www.amazon.com/s?k=";
    private String url_Amazon_co_uk = "https://www.amazon.co.uk/s?k=";
    //https://www.ebay.com/sch/i.html?_from=R40&_nkw=xbox+controller&_pgn=1
    private String url_Ebay_com = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=";

    private String searchUrl_Amazon_com = url_Amazon_com + searchTerm + "&page=";
    private String searchUrl_Amazon_co_uk = url_Amazon_co_uk + searchTerm + "&page=";
    private String searchUrl_Ebay_com = url_Ebay_com + searchTerm + "&_pgn=";

    private ArrayList<Item> itemList = new ArrayList<>();
    private ArrayList<Item> remove = new ArrayList<>();

    private int pages = 1;

    WebScrapper() {
        WebClient client = new WebClient(BrowserVersion.BEST_SUPPORTED);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        // AMAZON.COM
        for (int i = 1; i <= pages; i++) {
            try {
                HtmlPage page = client.getPage(searchUrl_Amazon_com + i);
                System.out.println(page.getUrl());
                List<HtmlElement> items = page.getByXPath("//div[@class='sg-col-4-of-12 sg-col-8-of-16 sg-col-16-of-24 sg-col-12-of-20 sg-col-24-of-32 sg-col sg-col-28-of-36 sg-col-20-of-28']");
                for (HtmlElement item : items) {
                    HtmlElement name = item.getFirstByXPath(".//span[@class='a-size-medium a-color-base a-text-normal']");
                    HtmlElement link = item.getFirstByXPath(".//a[@class='a-link-normal a-text-normal']");

                    if (name != null && link != null)
                        itemList.add(new Item(name.asText(), AMAZON_COM, "https://www.amazon.com" + link.getAttribute("href")));
                }
                Thread.sleep(1000);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        // AMAZON.CO.UK
        for (int i = 1; i <= pages; i++) {
            try {
                HtmlPage page = client.getPage(searchUrl_Amazon_co_uk + i);
                System.out.println(page.getUrl());
                List<HtmlElement> items = page.getByXPath("//div[@class='sg-col-4-of-12 sg-col-8-of-16 sg-col-16-of-24 sg-col-12-of-20 sg-col-24-of-32 sg-col sg-col-28-of-36 sg-col-20-of-28']");
                for (HtmlElement item : items) {
                    HtmlElement name = item.getFirstByXPath(".//span[@class='a-size-medium a-color-base a-text-normal']");
                    HtmlElement link = item.getFirstByXPath(".//a[@class='a-link-normal a-text-normal']");

                    if (name != null && link != null)
                        itemList.add(new Item(name.asText(), AMAZON_CO_UK, "https://www.amazon.co.uk" + link.getAttribute("href")));
                }
                Thread.sleep(1000);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        // EBAY.COM
        for (int i = 1; i <= pages; i++) {
            try {
                HtmlPage page = client.getPage(searchUrl_Ebay_com + i);
                System.out.println(page.getUrl());
                List<HtmlElement> items = page.getByXPath("//div[@class='s-item__info clearfix']");
                for (HtmlElement item : items) {
                    DomText name = item.getFirstByXPath(".//h3[@class='s-item__title']/text()");
                    if (name == null || name.asText().isEmpty())
                        name = item.getFirstByXPath(".//h3[@class='s-item__title s-item__title--has-tags']/text()");
                    HtmlElement link = item.getFirstByXPath(".//a[@class='s-item__link']");
                    if (name != null && link != null) {
                        HtmlElement details = item.getFirstByXPath(".//div[@class='s-item__details clearfix']");
                        List<HtmlElement> primaryDetails = details.getByXPath(".//div[@class='s-item__detail s-item__detail--primary']");
                        char currency = 'd';
                        float price = 0.0f;
                        float shippingPrice = 0.0f;
                        boolean skip = false;
                        for (int j = 0; j < primaryDetails.size(); j++) {
                            if (primaryDetails.get(j) != null) {
                                if (j == 0) {
                                    currency = primaryDetails.get(j).asText().charAt(0);
                                    price = getPrice(primaryDetails.get(j).asText());
                                }
                                else if (primaryDetails.get(j).asText().contains("shipping"))
                                    shippingPrice = getPrice(primaryDetails.get(j).asText());
                                else if (primaryDetails.get(j).asText().contains("bids"))
                                    skip = true;
                            }
                        }
                        if (!skip)
                            itemList.add(new Item(name.asText(), EBAY_COM, currency, price, shippingPrice, link.getAttribute("href")));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Item item : itemList) {
            if (item.getSite() == AMAZON_COM || item.getSite() == AMAZON_CO_UK) {
                System.out.println(item.getSite());
                try {
                    HtmlPage page = client.getPage(item.getUrl());
                    HtmlElement items = (HtmlElement) page.getElementById("price");
                    if (items != null) {
                        HtmlElement price = items.getFirstByXPath(".//span[@class='a-size-medium a-color-price priceBlockSalePriceString']");
                        if (price == null || price.asText().isEmpty())
                            price = items.getFirstByXPath(".//span[@class='a-size-medium a-color-price priceBlockBuyingPriceString']");
                        else {
                            item.setPrice(getPrice(price.asText()));
                            item.setCurrency(price.asText().charAt(0));
                        }
                        if (price == null || price.asText().isEmpty()) {
                            List<HtmlElement> list = items.getByXPath(".//span[@class='a-size-small price-info-superscript']");
                            String sPrice = "";
                            if (list.size() > 0) {
                                HtmlElement currency = list.get(0);
                                HtmlElement dollars = items.getFirstByXPath(".//span[@class='price-large']");
                                HtmlElement cents = list.get(1);
                                if (currency != null) sPrice += currency.asText();
                                if (dollars != null) sPrice += dollars.asText();
                                if (cents != null) sPrice += "." + cents.asText();
                            } else {
                                System.out.println("REMOVE");
                                remove.add(item);
                            }
                            item.setPrice(getPrice(sPrice));
                            if (sPrice.length() > 0)
                                item.setCurrency(sPrice.charAt(0));
                        } else {
                            item.setPrice(getPrice(price.asText()));
                            item.setCurrency(price.asText().charAt(0));
                        }

                        HtmlElement shipPrice = items.getFirstByXPath(".//span[@class='a-size-base a-color-secondary']");
                        if (shipPrice != null) {

                            item.setShippingPrice(getPrice(shipPrice.asText()));
                        }
                    } else {
                        System.out.println("ITEMS: REMOVE");
                       // System.out.println(page.asText());
                        remove.add(item);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (item.getSite() == EBAY_COM)
                continue;
        }

        itemList.removeAll(remove);
        remove.clear();


        client.close();

        itemList.sort((it1, it2) -> Float.compare(it1.getFullPriceConverted(), it2.getFullPriceConverted()));

        try {
            PrintWriter pw = new PrintWriter("the-file-name.html", "UTF-8");
            for (Item item : itemList) {
                //System.out.println(item.getName() + " " + item.getPrice() + " " + item.getShippingPrice() + " " + item.getFullPriceConverted() + " Eur");
                //System.out.println(item.getUrl());
                String site = "";
                switch (item.getSite()) {
                    case AMAZON_COM:
                        site = "Amazon.com";
                        break;
                    case AMAZON_CO_UK:
                        site = "Amazon.co.uk";
                        break;
                    case EBAY_COM:
                        site = "Ebay.com";
                        break;
                }
                String s = site + " <a href=\"" + item.getUrl() + "\">" + item.getName() + "</a>" + " " + item.getFullPriceConverted() + " Eur<br>";
                pw.println(s);
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void scrapeAmazon_com() {

    }

    public void scrapeAmazon_co_uk() {

    }

    public void scrapeEbay_com() {

    }

    private float getPrice(String price) {
        String pr = "";
        boolean hadDecimal = false;
        int stopAt = 9999;
        for (int i = 0; i < price.length(); i++) {
            if (price.charAt(i) >= '0' && price.charAt(i) <= '9') {
                pr += price.charAt(i);
            } else if (price.charAt(i) == '.' && !hadDecimal) {
                pr += price.charAt(i);
                hadDecimal = true;
                stopAt = i + 2;
            }
            if (i == stopAt)
                break;
        }
        if (pr.isEmpty()) return 0.0f;
        else return Float.parseFloat(pr);
    }


    public static void main(String[] args) {
        new WebScrapper();
    }
}
