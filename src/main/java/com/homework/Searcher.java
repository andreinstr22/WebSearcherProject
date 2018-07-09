package com.homework;

import com.homework.helper.WebsiteScraperHelper;
import com.homework.model.WebsiteData;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


/**
 * Execute by running java -jar WebSearcher.jar
 * Has 2 optional parameters
 * -Durls="url for target urls file."
 * -Dpattern="some pattern" provide any pattern to search for instead of defaults.
 */
public class Searcher {

    public static final int MAX_REQUEST_THREADS = 20;
    private static final int MAX_CONSUMER_THREADS = 20;

    public static final String POISON_PILL = "poisonPill";
    private static final String DEFAULT_URL_LOCATION = "https://s3.amazonaws.com/fieldlens-public/urls.txt";
    public static final String URL = "\"URL\"";


    public static void main(String[] args) {
        LinkedBlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<WebsiteData> htmlQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<String> notScraped = new LinkedBlockingQueue<>();


        String urlFileLocation = System.getProperty("urls");
        populateUrlQueue(urlFileLocation, urlQueue);
        int numberOfUrls = urlQueue.size();


        List<Pattern> patterns = new ArrayList<>();
        String pattern = System.getProperty("pattern");
        populatePatterns(patterns, pattern);

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("results.txt"));
        } catch (IOException e) {
            System.out.println("Unable to create results file due to : " + e.getMessage());
            return;
        }

        for (int i = 0; i < MAX_REQUEST_THREADS; i++) {
            Thread producer = new HtmlProducer(urlQueue, htmlQueue, notScraped);
            producer.start();
        }


        AtomicInteger producersFinished = new AtomicInteger(0);
        AtomicInteger urlsProcessed = new AtomicInteger(0);
        for (int i = 0; i < MAX_CONSUMER_THREADS; i++) {
            Thread consumer = new HtmlConsumer(htmlQueue, patterns, writer, producersFinished, urlsProcessed);
            consumer.start();
        }


        while (urlsProcessed.get() < numberOfUrls - notScraped.size()) {
            try {
                Thread.currentThread().sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Done");
        try {
            writer.write("Not able to scrape :");
            writer.write(System.getProperty("line.separator"));
            while (!notScraped.isEmpty()) {
                writer.write(notScraped.poll());
                writer.write(System.getProperty("line.separator"));
                writer.flush();
            }

        } catch (IOException e) {
            System.out.println("Unable to finish result dump due to :" + e.getMessage());
        }
        System.exit(0);
    }

    private static void populatePatterns(List<Pattern> patterns, String pattern) {
        if (pattern != null) {
            patterns.add(Pattern.compile("(?)" + pattern));
            return;
        }
        patterns.add(Pattern.compile("(?)We.orks"));
        patterns.add(Pattern.compile("(?)G.ogle"));

    }

    private static void populateUrlQueue(String urlFileLocation, LinkedBlockingQueue<String> urlQueue) {
        if (urlFileLocation == null) {
            urlFileLocation = DEFAULT_URL_LOCATION;
        }

        try {
            parseUrls(urlFileLocation, urlQueue);
        } catch (Exception e) {
            System.out.println("Unable to parse urls due to " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void parseUrls(String urlFileLocation, LinkedBlockingQueue<String> urlQueue) throws Exception {
        String urls = WebsiteScraperHelper.getWebsiteHtmlAsString(urlFileLocation);
        BufferedReader reader = new BufferedReader(new StringReader(urls));
        List<String> headers = new ArrayList<>(Arrays.asList(reader.readLine().split(",")));
        int urlIndex = headers.indexOf(URL);
        String line;
        while ((line = reader.readLine()) != null) {
            String url = line.split(",")[urlIndex];
            url = url.substring(1, url.length() - 1); // Remove ""
            urlQueue.put(url);
        }
    }

}

