import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebScraper {
    private final int AMAZON_COM = 0;
    private final int AMAZON_CO_UK = 1;
    private final int EBAY_COM = 2;

    private WebClient clientChrome;
    private WebClient clientFirefox;

    private String url_Amazon_com = "https://www.amazon.com/s?k=";
    private String url_Amazon_co_uk = "https://www.amazon.co.uk/s?k=";
    private String url_Ebay_com = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=";

    private String searchUrl_Amazon_com;
    private String searchUrl_Amazon_co_uk;
    private String searchUrl_Ebay_com;

    private ArrayList<Item> Amazon_com_List = new ArrayList<>();
    private ArrayList<Item> Amazon_co_uk_List = new ArrayList<>();
    private ArrayList<Item> Ebay_com_List = new ArrayList<>();

    private ArrayList<Item> itemList = new ArrayList<>();
    private ArrayList<Item> remove = new ArrayList<>();

    private int pages = 1;

    WebScraper() {
        clientChrome = new WebClient(BrowserVersion.CHROME);
        clientChrome.getOptions().setCssEnabled(false);
        clientChrome.getOptions().setJavaScriptEnabled(false);

        clientFirefox = new WebClient(BrowserVersion.FIREFOX_60);
        clientFirefox.getOptions().setCssEnabled(false);
        clientFirefox.getOptions().setJavaScriptEnabled(false);
    }

    public ArrayList<Item> getItemList() {
        return itemList;
    }



    public void setSearch(String searchTerm) {
        searchUrl_Amazon_com = url_Amazon_com + searchTerm + "&page=";
        searchUrl_Amazon_co_uk = url_Amazon_co_uk + searchTerm + "&page=";
        searchUrl_Ebay_com = url_Ebay_com + searchTerm + "&_pgn=";
    }

    public void sortByPrice() {
        itemList.addAll(Amazon_com_List);
        itemList.addAll(Amazon_co_uk_List);
        itemList.addAll(Ebay_com_List);

        itemList.sort((item1, item2) -> Float.compare(item1.getFullPriceConverted(), item2.getFullPriceConverted()));
    }

    public void mostRelevantItemSort() {
        itemList.clear();
        ArrayList<Item> tempList = new ArrayList<>();
        int maxSize = Amazon_com_List.size();
        if (Amazon_co_uk_List.size() > maxSize) maxSize = Amazon_co_uk_List.size();
        if (Ebay_com_List.size() > maxSize) maxSize = Ebay_com_List.size();

        for (int i = 0; i < maxSize; i++) {
            if (Amazon_com_List.size() >= i + 1) tempList.add(Amazon_com_List.get(i));
            if (Amazon_co_uk_List.size() >= i + 1) tempList.add(Amazon_co_uk_List.get(i));
            if (Ebay_com_List.size() >= i + 1) tempList.add(Ebay_com_List.get(i));

            tempList.sort((item1, item2) -> Float.compare(item1.getFullPriceConverted(), item2.getFullPriceConverted()));
            itemList.addAll(tempList);

            tempList.clear();
        }
        Amazon_com_List.clear();
        Amazon_co_uk_List.clear();
        Ebay_com_List.clear();
    }

    public void scrapeAmazon_com() {
        for (int i = 1; i <= pages; i++) {
            try {
                HtmlPage page = clientChrome.getPage(searchUrl_Amazon_com + i);
                List<HtmlElement> items = page.getByXPath("//div[@class='sg-col-4-of-12 sg-col-8-of-16 sg-col-16-of-24 sg-col-12-of-20 sg-col-24-of-32 sg-col sg-col-28-of-36 sg-col-20-of-28']");
                if (items.size() == 0) {
                    page = clientFirefox.getPage(searchUrl_Amazon_com + i);
                    items = page.getByXPath("//div[@class='sg-col-4-of-12 sg-col-8-of-16 sg-col-16-of-24 sg-col-12-of-20 sg-col-24-of-32 sg-col sg-col-28-of-36 sg-col-20-of-28']");
                }
                for (HtmlElement item : items) {
                    HtmlElement name = item.getFirstByXPath(".//span[@class='a-size-medium a-color-base a-text-normal']");
                    HtmlElement link = item.getFirstByXPath(".//a[@class='a-link-normal a-text-normal']");

                    if (name != null && link != null)
                        Amazon_com_List.add(new Item(name.asText(), AMAZON_COM, "https://www.amazon.com" + link.getAttribute("href")));
                }
                Thread.sleep(1000);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        checkItemsAmazon_com();
    }

    private void checkItemsAmazon_com() {
        for (Item item : Amazon_com_List) {
            try {
                HtmlPage page;
                HtmlElement items;
                page = clientChrome.getPage(item.getUrl());
                items = (HtmlElement) page.getElementById("price");
                if (items == null) {
                    page = clientFirefox.getPage(item.getUrl());
                    items = (HtmlElement) page.getElementById("price");
                }

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
                    remove.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Amazon_com_List.removeAll(remove);
        remove.clear();
    }

    public void scrapeAmazon_co_uk() {
        for (int i = 1; i <= pages; i++) {
            try {
                HtmlPage page = clientChrome.getPage(searchUrl_Amazon_co_uk + i);
                List<HtmlElement> items = page.getByXPath("//div[@class='sg-col-4-of-12 sg-col-8-of-16 sg-col-16-of-24 sg-col-12-of-20 sg-col-24-of-32 sg-col sg-col-28-of-36 sg-col-20-of-28']");
                if (items.size() == 0) {
                    page = clientFirefox.getPage(searchUrl_Amazon_co_uk + i);
                    items = page.getByXPath("//div[@class='sg-col-4-of-12 sg-col-8-of-16 sg-col-16-of-24 sg-col-12-of-20 sg-col-24-of-32 sg-col sg-col-28-of-36 sg-col-20-of-28']");
                }
                for (HtmlElement item : items) {
                    HtmlElement name = item.getFirstByXPath(".//span[@class='a-size-medium a-color-base a-text-normal']");
                    HtmlElement link = item.getFirstByXPath(".//a[@class='a-link-normal a-text-normal']");

                    if (name != null && link != null)
                        Amazon_co_uk_List.add(new Item(name.asText(), AMAZON_CO_UK, "https://www.amazon.co.uk" + link.getAttribute("href")));

                }
                Thread.sleep(1000);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        checkItemsAmazon_co_uk();
    }

    private  void checkItemsAmazon_co_uk() {
        for (Item item : Amazon_co_uk_List) {
            try {
                HtmlPage page = clientChrome.getPage(item.getUrl());
                HtmlElement items = (HtmlElement) page.getElementById("price");
                if (items == null) {
                    page = clientFirefox.getPage(item.getUrl());
                    items = (HtmlElement) page.getElementById("price");
                }
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
                    remove.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Amazon_co_uk_List.removeAll(remove);
        remove.clear();
    }

    public void scrapeEbay_com() {
        for (int i = 1; i <= pages; i++) {
            try {
                HtmlPage page = clientChrome.getPage(searchUrl_Ebay_com + i);
                List<HtmlElement> items = page.getByXPath("//div[@class='s-item__info clearfix']");
                if (items.size() == 0) {
                    page = clientFirefox.getPage(searchUrl_Ebay_com + i);
                    items = page.getByXPath("//div[@class='s-item__info clearfix']");
                }
                for (HtmlElement item : items) {
                    DomText name = item.getFirstByXPath(".//h3[@class='s-item__title']/text()");
                    if (name == null || name.asText().isEmpty())
                        name = item.getFirstByXPath(".//h3[@class='s-item__title s-item__title--has-tags']/text()");
                    HtmlElement link = item.getFirstByXPath(".//a[@class='s-item__link']");
                    if (name != null && link != null) {
                        HtmlElement details = item.getFirstByXPath(".//div[@class='s-item__details clearfix']");
                        List<HtmlElement> primaryDetails = details.getByXPath(".//div[@class='s-item__detail s-item__detail--primary']");
                        char currency = ' ';
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
                            Ebay_com_List.add(new Item(name.asText(), EBAY_COM, currency, price, shippingPrice, link.getAttribute("href")));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private float getPrice(String price) {
        StringBuilder modifiedPrice = new StringBuilder();
        boolean hadDecimal = false;
        int stopAt = 9999;
        for (int i = 0; i < price.length(); i++) {
            if (price.charAt(i) >= '0' && price.charAt(i) <= '9') {
                modifiedPrice.append(price.charAt(i));
            } else if (price.charAt(i) == '.' && !hadDecimal) {
                modifiedPrice.append(price.charAt(i));
                hadDecimal = true;
                stopAt = i + 2;
            }
            if (i == stopAt)
                break;
        }
        if (modifiedPrice.length() == 0 || modifiedPrice.charAt(0) == '.') return 0.0f;
        else return Float.parseFloat(modifiedPrice.toString());
    }
}
