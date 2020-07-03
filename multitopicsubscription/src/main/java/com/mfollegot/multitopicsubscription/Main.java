package com.mfollegot.multitopicsubscription;

import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceConsumer;
import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceMultiConsumer;
import com.mfollegot.multitopicsubscription.pulsar.PulsarSequenceProducer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
//        pulsarTailingTestWithZeroProc(1, 1); // Pulsar Case 1
//        pulsarTailingTestWithInterval(1, 1); // Pulsar Case 2
//        pulsarTailingTestWithJitter(1, 1); // Pulsar Case 3
//        pulsarTailingTestFastProducer(1,1); // Pulsar Case 4
//        pulsarTailingTestWithZeroProc(10, 1); // Pulsar Case 5
//        pulsarTailingTestWithInterval(10, 1); // Pulsar Case 6
//        pulsarTailingTestWithJitter(10, 1); // Pulsar Case 7
//        pulsarTailingTestFastProducer(10,1); // Pulsar Case 8
//        pulsarTimeTravelTest(1, 1); // Pulsar Case 9
//        pulsarTimeTravelTest(10, 1); // Pulsar Case 10
        pulsarTimeTravelMultiConsumer(1,1);

    }

    private static void pulsarTailingTestWithZeroProc(int topic1Ratio, int topic2Ratio) {
        String prefix = "persistent://ssa/ingress/";
        String suffix = getSuffix();
        List<String> topics = Arrays.asList(prefix+"topic1"+suffix, prefix+"topic2"+suffix);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        PulsarSequenceConsumer consumer = new PulsarSequenceConsumer(1, topics, false);
        executor.submit(() -> consumer.consume(Duration.ZERO, Duration.ZERO, false));

        waitFor(100);


        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin or other ratio
        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        executor.submit(() -> orderProducer.produce(100, Duration.ZERO));

        executor.shutdown();
    }

    private static void pulsarTailingTestWithInterval(int topic1Ratio, int topic2Ratio) {
        String prefix = "persistent://ssa/ingress/";
        String suffix = getSuffix();
        List<String> topics = Arrays.asList(prefix+"topic1"+suffix, prefix+"topic2"+suffix);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        PulsarSequenceConsumer consumer = new PulsarSequenceConsumer(1, topics, false);
        executor.submit(() -> consumer.consume(Duration.ofMillis(10), Duration.ofMillis(10), false));

        waitFor(100);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin or other ratio
        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        executor.submit(() -> orderProducer.produce(100, Duration.ofMillis(10)));

        executor.shutdown();
    }

    private static void pulsarTailingTestWithJitter(int topic1Ratio, int topic2Ratio) {
        String prefix = "persistent://ssa/ingress/";
        String suffix = getSuffix();
        List<String> topics = Arrays.asList(prefix+"topic1"+suffix, prefix+"topic2"+suffix);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        PulsarSequenceConsumer consumer = new PulsarSequenceConsumer(1, topics, false);
        executor.submit(() -> consumer.consume(Duration.ofMillis(1), Duration.ofMillis(50), true));

        waitFor(100);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin or other ratio
        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        executor.submit(() -> orderProducer.produce(100, Duration.ofMillis(25)));

        executor.shutdown();
    }

    private static void pulsarTailingTestFastProducer(int topic1Ratio, int topic2Ratio) {
        String prefix = "persistent://ssa/ingress/";
        String suffix = getSuffix();
        List<String> topics = Arrays.asList(prefix+"topic1"+suffix, prefix+"topic2"+suffix);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // set receiveQueueSize to ten order to better test a fast producer
        PulsarSequenceConsumer consumer = new PulsarSequenceConsumer(1, topics, false, 10);
        executor.submit(() -> consumer.consume(Duration.ofMillis(100), Duration.ofMillis(100), false));

        waitFor(5000);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin or other ratio
        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        executor.submit(() -> orderProducer.produce(100, Duration.ofMillis(1)));

        executor.shutdown();
    }

    private static void pulsarTimeTravelTest(int topic1Ratio, int topic2Ratio) {
        String prefix = "persistent://ssa/ingress/";
        String suffix = getSuffix();
        List<String> topics = Arrays.asList(prefix+"topic1"+suffix, prefix+"topic2"+suffix);

        ExecutorService executor = Executors.newFixedThreadPool(5);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio);
        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        orderProducer.produce(100, Duration.ofMillis(1));

        PulsarSequenceConsumer consumer = new PulsarSequenceConsumer(1, topics, true);
        executor.submit(() -> consumer.consume(Duration.ofMillis(0), Duration.ofMillis(0), false));

        executor.shutdown();
    }

    private static void pulsarTimeTravelMultiConsumer(int topic1Ratio, int topic2Ratio) {
        String prefix = "persistent://ssa/ingress/123/";
        String suffix = getSuffix();
        List<String> topics = Arrays.asList(prefix+"topic1"+suffix, prefix+"topic2"+suffix);
        List<String> topic = Arrays.asList(prefix + "topic7" + suffix);

        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<Integer> msgCounts = Arrays.asList(topic1Ratio, topic2Ratio); // controls if it is round-robin or other ratio
        PulsarSequenceProducer orderProducer = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer orderProducer2 = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer orderProducer3 = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer orderProducer4 = new PulsarSequenceProducer(topics, msgCounts);
        PulsarSequenceProducer lateOrderProducer = new PulsarSequenceProducer(topic, msgCounts);
//        executor.submit(() -> orderProducer.produce(200, Duration.ofMillis(300)));
        orderProducer.produce(30, Duration.ofMillis(300));
//        executor.submit(() -> orderProducer2.produce(200, Duration.ofMillis(300)));
        orderProducer2.produce(30, Duration.ofMillis(300));
        executor.submit(() -> orderProducer3.produce(1200, Duration.ofMillis(300)));
        executor.submit(() -> orderProducer4.produce(1200, Duration.ofMillis(300)));

        System.out.print("Topics: " + topics.toString());
        System.out.println("and finalement, [" + topics.toString() + "]");
        PulsarSequenceMultiConsumer consumer = new PulsarSequenceMultiConsumer(topics);
        PulsarSequenceMultiConsumer consumer2 = new PulsarSequenceMultiConsumer(topics);
//        consumer.initialize(true);
        executor.submit(() -> consumer.consume());
        //executor.submit(() -> consumer2.consume());
        executor.submit(() -> {
                    lateOrderProducer.produce(500, Duration.ofMillis(1000));
                }
        );

        //executor.shutdown();
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

    private static String getSuffix() {
        Random rand = new Random();
        return "_" + String.valueOf(rand.nextInt(9999));
    }
}