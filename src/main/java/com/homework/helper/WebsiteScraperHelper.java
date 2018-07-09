package com.homework.helper;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.logging.LogFactory;

import java.util.logging.Level;

public class WebsiteScraperHelper {

    public static final int TIMEOUT = 1000;

    public static String getWebsiteHtmlAsString(String url) throws Exception {
        //Suppress htmlUnit Warnings
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

        final WebClient webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setTimeout(TIMEOUT);
        final Page page = webClient.getPage(url);
        if (page.isHtmlPage()) {
            return ((HtmlPage) page).asText();
        } else {
            return ((TextPage) page).getContent();
        }
    }
}
