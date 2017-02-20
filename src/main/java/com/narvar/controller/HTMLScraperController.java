package com.narvar.controller;


import com.narvar.model.HtmlScraperRequest;
import com.narvar.services.HTMLScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/htmlscraper", produces = MediaType.APPLICATION_JSON_VALUE)
public class HTMLScraperController {

    @Autowired
    private HTMLScraperService service;

    public static final String ORDER_PAGE = "tracking";
    public static final String TRACKING_PAGE = "order";

    @RequestMapping(value = "/{html_origin}",method= RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public String scrape(@PathVariable("html_origin") String origin, @RequestBody HtmlScraperRequest request) {
        // TODO: email access and parsing.
        if (ORDER_PAGE.equals(origin) || TRACKING_PAGE.equals(origin)) {
            return service.extract(request.getConnectionUrl(), request.getSelector());
        }
        return null;
    }

    @RequestMapping(value = "/{name}",method= RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String helloscraper(@PathVariable("name") String name) {
        return "Hello " + name;
    }

}
