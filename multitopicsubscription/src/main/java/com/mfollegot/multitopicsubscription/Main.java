package com.mfollegot.multitopicsubscription;

import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceConsumer;
import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceMultiConsumer;
import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceProducer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Main {

    private static String prefix = "persistent://ssa/ingress/123/";
    private static List<String> topics = Arrays.asList(prefix+"topic1", prefix+"topic2");
    private static List<String> topics2 = Arrays.asList(prefix+"topic3", prefix+"topic4");
    private static List<String> lateTopic = Arrays.asList(prefix + "topic100");

    public static void main(String[] args) {
//        pulsarMultiTopicProducer();
//        pulsarMultiTopicConsumer();
//        pulsar1kTopicConsumer(1,1);
//        consumeTrainerResolverEvents();
        drainQueue();
    }

    private static void pulsarMultiTopicConsumer() {
        pulsarMultiTopicConsumer(1,1);
    }

    private static void pulsarMultiTopicConsumer(int topic1Ratio, int topic2Ratio) {
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin

        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer orderProducer2 = new PulsarSequenceProducer(topics, msgCounts);
        orderProducer.produce(1000, Duration.ofMillis(0));
//        orderProducer2.produce(1000, Duration.ofMillis(0));

        PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer();
        executor.submit(() -> consumer.consume(0));

        waitFor(2000);
        executor.submit(() -> {
                    PulsarSequenceProducer lateOrderProducer = new PulsarSequenceProducer(lateTopic, msgCounts);
                    lateOrderProducer.produce(100, Duration.ofMillis(0));
                });

//        waitFor(70000);
        executor.submit(() -> {
                    orderProducer2.produce(100, Duration.ofMillis(10));
        });

        executor.shutdown();
    }

    private static void drainQueue() {
        PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer();
        consumer.consume(0);
    }

    private static void pulsar1kTopicConsumer(int topic1Ratio, int topic2Ratio) {
        ExecutorService executor = Executors.newFixedThreadPool(502);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin

        PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer();
        executor.submit(() -> consumer.consume(500));
        PulsarSequenceMultiConsumer consumer2 = new PulsarSequenceMultiConsumer();
        executor.submit(() -> consumer2.consume(500));

        int i = 1;
        while (i <= 600) {
            List<String> dualTopics = Arrays.asList(prefix+"topic" + i++, prefix+"topic" + i);
            PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(dualTopics, msgCounts);
            executor.submit(() -> orderProducer.produce(100, Duration.ofMillis(500)));
            i++;
//            if (i==500 || i==501) {
//                PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer();
//                executor.submit(() -> consumer.consume(0));
//            }
        }
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

    private static void consumeTrainerResolverEvents() {
        ExecutorService executor = Executors.newFixedThreadPool(6);

        PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer(
          Pattern.compile("persistent://ssa/internal/.*"));
        executor.submit(() -> consumer.consume(0));
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
