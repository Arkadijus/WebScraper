import java.text.DecimalFormat;

public class Item {
    private float dollarToEur = 0.91f;
    private float poundToEur = 1.19f;

    private final String name;
    private final int site;
    private char currency;
    private float price;
    private float shippingPrice;
    private final String url;

    public Item(String name, int site, String url) {
        this.name = name;
        this.site = site;
        this.url = url;
    }

    public Item(String name, int site, char currency, float price, float shippingPrice, String url) {
        this.name = name;
        this.site = site;
        this.currency = currency;
        this.price = price;
        this.shippingPrice = shippingPrice;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public int getSite() { return site; }

    public float getPrice() {
        return price;
    }

    public String getUrl() {
        return url;
    }

    public void setCurrency(char currency) {
        this.currency = currency;
    }

    public char getCurrency() {
        return currency;
    }

    public void setShippingPrice(float shippingPrice) {
        this.shippingPrice = shippingPrice;
    }

    public float getShippingPrice() {
        return shippingPrice;
    }

    public float getFullPrice() {
        float fullPrice = (price + shippingPrice);
        DecimalFormat df = new DecimalFormat("0.00");
        return Float.parseFloat(df.format(fullPrice));
    }

    public float getFullPriceConverted() {
        float fullPrice = 0.0f;
        if (currency == '$')
            fullPrice = (price + shippingPrice) * dollarToEur;
        else if (currency == '£')
            fullPrice = (price + shippingPrice) * poundToEur;
        DecimalFormat df = new DecimalFormat("0.00");
        return Float.parseFloat(df.format(fullPrice));
    }
}
