package com.narvar.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

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
     * @param htmlString
     * @param selector
     * @return Value present in the element mentioned by selector
     */
    public String extractText(Document htmlString, String selector) {
        Elements elements = htmlString.select(selector);

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
        if (!StringUtils.hasLength(connectionUrl) && !StringUtils.hasLength(selector)) {
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
}
