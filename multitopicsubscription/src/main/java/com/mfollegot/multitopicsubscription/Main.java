package com.mfollegot.multitopicsubscription;

import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceConsumer;
import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceMultiConsumer;
import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceProducer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static String prefix = "persistent://ssa/ingress/123/";
    private static List<String> topics = Arrays.asList(prefix+"topic1", prefix+"topic2");
    private static List<String> topics2 = Arrays.asList(prefix+"topic3", prefix+"topic4");
    private static List<String> lateTopic = Arrays.asList(prefix + "topic5");

    public static void main(String[] args) {
        pulsarMultiTopicConsumer();
//        pulsarMultiTopicProducer();

    }

    private static void pulsarMultiTopicConsumer() {
        pulsarMultiTopicConsumer(1,1);
    }

    private static void pulsarMultiTopicConsumer(int topic1Ratio, int topic2Ratio) {
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin

        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer orderProducer2 = new PulsarSequenceProducer(topics, msgCounts);
        orderProducer.produce(400, Duration.ofMillis(0));

        PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer(topics);
        executor.submit(() -> consumer.consume(300));

//        waitFor(2000);
        executor.submit(() -> {
                    PulsarSequenceProducer lateOrderProducer = new PulsarSequenceProducer(lateTopic, msgCounts);
                    lateOrderProducer.produce(100, Duration.ofMillis(0));
                });

        waitFor(70000);
        executor.submit(() -> {
                    orderProducer2.produce(100, Duration.ofMillis(10));
        });

        executor.shutdown();
    }

    private static void pulsarMultiTopicProducer() {
        pulsarMultiTopicProducer(1,1);
    }

    private static void pulsarMultiTopicProducer(int topic1Ratio, int topic2Ratio) {
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin

        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer orderProducer2 = new PulsarSequenceProducer(topics2, msgCounts);
        executor.submit(() -> orderProducer.produce(8000, Duration.ofMillis(0)));
        executor.submit(() -> orderProducer2.produce(8000, Duration.ofMillis(0)));

        System.out.print("Topics: " + topics.toString());
        System.out.println(" and finally, " + lateTopic.toString());

        waitFor(2000);
        executor.submit(() -> {
                    PulsarSequenceProducer lateOrderProducer = new PulsarSequenceProducer(lateTopic, msgCounts);
                    lateOrderProducer.produce(600, Duration.ofMillis(20));
                });

        executor.shutdown();
    }

    private static void waitFor(int milliseconds) {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }
}
