package com.homework;

import com.homework.helper.WebsiteScraperHelper;
import com.homework.model.WebsiteData;

import java.util.concurrent.LinkedBlockingQueue;

import static com.homework.Searcher.POISON_PILL;

public class HtmlProducer extends Thread {

    private LinkedBlockingQueue<String> urlQueue;
    private LinkedBlockingQueue<WebsiteData> htmlQueue;
    private LinkedBlockingQueue<String> notScraped;

    public HtmlProducer(LinkedBlockingQueue urlQueue, LinkedBlockingQueue htmlQueue, LinkedBlockingQueue notScraped) {
        this.urlQueue = urlQueue;
        this.htmlQueue = htmlQueue;
        this.notScraped = notScraped;
    }

    @Override
    public void run() {
        while (!urlQueue.isEmpty()) {
            String url = urlQueue.poll();
            if (url == null) {
                addPoisonPill();
                return;
            }
            scrapeUrl(url);
        }
        addPoisonPill();
    }

    private void addPoisonPill() {
        WebsiteData poisonPill = new WebsiteData();
        poisonPill.setHtml(POISON_PILL);
        htmlQueue.add(poisonPill);
    }

    private void scrapeUrl(String url) {
        try {
            WebsiteData websiteData = new WebsiteData();
            websiteData.setUrl(url);
            websiteData.setHtml(WebsiteScraperHelper.getWebsiteHtmlAsString("http://" + url));
            htmlQueue.add(websiteData);
        } catch (Exception e) {
            notScraped.add(url);
            System.out.println("Unable to scrape url " + url);
        }
    }
}
