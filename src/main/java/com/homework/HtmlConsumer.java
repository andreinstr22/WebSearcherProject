package com.homework;


import com.homework.model.WebsiteData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.homework.Searcher.MAX_REQUEST_THREADS;
import static com.homework.Searcher.POISON_PILL;

public class HtmlConsumer extends Thread {

    private LinkedBlockingQueue<WebsiteData> htmlQeueue;
    private List<Pattern> patterns;
    private BufferedWriter bufferedWriter;
    private AtomicInteger producersFinished;
    private AtomicInteger urlsProcessed;

    public HtmlConsumer(LinkedBlockingQueue<WebsiteData> htmlQueue, List<Pattern> patterns, BufferedWriter bufferedWriter, AtomicInteger producersFinished, AtomicInteger urlsProcessed) {
        this.htmlQeueue = htmlQueue;
        this.patterns = patterns;
        this.bufferedWriter = bufferedWriter;
        this.producersFinished = producersFinished;
        this.urlsProcessed = urlsProcessed;
    }

    @Override
    public void run() {
        try {
            while (producersFinished.get() < MAX_REQUEST_THREADS || !htmlQeueue.isEmpty()) {
                WebsiteData websiteData = htmlQeueue.take();
                if (websiteData.getHtml().equals(POISON_PILL)) {
                    producersFinished.incrementAndGet();
                } else {
                    consume(websiteData);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void consume(WebsiteData websiteData) {
        StringBuilder result = new StringBuilder();
        result.append("Url : ");
        result.append(websiteData.getUrl());
        result.append(System.getProperty("line.separator"));
        for (Pattern pattern : patterns) {
            result.append("Pattern: ");
            result.append(pattern.toString());
            result.append(" : ");
            result.append(pattern.matcher(websiteData.getHtml()).find());
            result.append(System.getProperty("line.separator"));
        }
        result.append("-------------------------------------------------");
        result.append(System.getProperty("line.separator"));

        synchronized (bufferedWriter) {
            try {
                bufferedWriter.write(result.toString());
                bufferedWriter.flush();
                urlsProcessed.incrementAndGet();
            } catch (IOException e) {
                System.out.println(" Unable to write result due to " + e.getMessage());
            }
        }
    }
}
