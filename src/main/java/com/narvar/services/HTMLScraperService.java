package com.narvar.services;

import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import us.codecraft.xsoup.Xsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an utility service intended to scrape HTML documents.
 * The class can be modified to have multiple scraper methods returning different objects
 * e.g. Order Info, Tracking Info, Order Info from emails
 */
@Service
public class HTMLScraperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTMLScraperService.class);

    /**
     * Basic method to extract the text present at the mentioned selector.
     * e.g. Here is a sample selector:
     *          body > center > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td.width292 > table:nth-child(8) > tbody > tr:nth-child(1) > td > div > div > span
     *  This method retrieves the value in this span element
     * @param document
     * @param selector
     * @return Value present in the element mentioned by selector
     */
    public String extractText(Document document, String selector) {
        Elements elements = document.select(selector);

        if (elements != null && elements.size() > 0) {
            return elements.get(0).text();
        }

        return null;
    }

    /**
     * When the source of html is either an order page url or a tracking page,
     * the HTML document is obtained first and passed to extractText method along with selector
     * @param connectionUrl
     * @param selector
     * @return Value present in the element mentioned by selector
     */
    public String extract(String connectionUrl, String selector) {
        if (StringUtils.hasLength(connectionUrl) && StringUtils.hasLength(selector)) {
            try {
                Document doc = Jsoup.connect(connectionUrl).timeout(5000).get();
                return extractText(doc, selector);
            } catch (IOException e) {
                LOGGER.info("Failed to parse the html for given selector.");
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Extract individual element from an HTML document using xPath
     * @param htmlString
     * @param xpath
     * @return value contained withing element identified by xpath
     */
    public String extractTextWithXPath(String htmlString, String xpath) {
            Document document = Jsoup.parse(htmlString);
            return Xsoup.compile(xpath).evaluate(document).get();
    }

    /**
     * Extract list of elements from an HTML document using xPath
     * @param htmlString
     * @param xpath
     * @return
     */
    public List<String> extractListWithXPath(String htmlString, String xpath) {
        Document document = Jsoup.parse(htmlString);
        return Xsoup.compile(xpath).evaluate(document).list();
    }

    /**
     * Retrieve the details using xpath entries in the map
     * @param connectionUrl Url to the HTML
     * @param pathMap Map containing attribute name and xpath to the attribute
     * @return JSONObject constructed using keys and extracted values
     */
    public JSONObject extractJson(String connectionUrl, Map<String, String> pathMap) {
        if (StringUtils.hasLength(connectionUrl)) {
            try {
                Document doc = Jsoup.connect(connectionUrl).get();
                JSONObject trackingObject = new JSONObject();
                for (Map.Entry<String, String> entry : pathMap.entrySet()) {
                    String val = Xsoup.compile(entry.getValue()).evaluate(doc).get();
                    trackingObject.put(entry.getKey(), val);
                }
                return trackingObject;
            } catch (IOException e) {
                LOGGER.info("Failed to parse the html for given selector.");
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        HTMLScraperService service = new HTMLScraperService();
        String html = "<html><div><a href='https://github.com'>github.com</a></div>" +
                "<table><tr><td>a</td><td>b</td></tr></table></html>";
        String xpath = "//a/@href";
        System.out.println(service.extractTextWithXPath(html, xpath));

        xpath = "//tr/td/text()";
        List<String> list = service.extractListWithXPath(html, xpath);
        for (String item : list) {
            System.out.println(item);
        }

        Map<String, String> trackingMap = new HashMap<>();
        trackingMap.put("status", "//*[@id=\"wrap\"]/div[3]/div/div[1]/div[2]/div[2]/h2");
        JSONObject jsonObject = service.extractJson("http://webtrack.dhlglobalmail.com/?trackingnumber=92748901859227111139186495", trackingMap);
        System.out.println(jsonObject.toString());

    }
}
