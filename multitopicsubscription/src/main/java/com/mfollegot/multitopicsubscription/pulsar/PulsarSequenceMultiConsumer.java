package com.mfollegot.multitopicsubscription.pulsar;

import org.apache.pulsar.client.api.*;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class PulsarSequenceMultiConsumer {
    private boolean isCancelled;
    private boolean isCompleted;
    private Consumer<String> consumer;

    PulsarClient client;
    List<String> consumerIds;
    List<Consumer> consumers;
    List<String> topics;
    Message[] messages;

    public PulsarSequenceMultiConsumer(List<String> topics) {
        this.topics = topics;
        consumerIds = new ArrayList<>();
        consumers = new ArrayList<>();
        messages = new Message[topics.size()];
        try {
            client = PulsarClient.builder()
                    .serviceUrl(PulsarConnection.ServiceUrl)
                    .build();

            Pattern allTopicsInNamespace = Pattern.compile("persistent://ssa/ingress/123/.*");

            this.consumer = client.newConsumer(Schema.STRING)
                    .topicsPattern(allTopicsInNamespace)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .subscriptionName("dsp-input-raw-events-subscription1")
                    .subscriptionType(SubscriptionType.Shared)
                    .subscribe();
        }
        catch(PulsarClientException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Failed starting client and producers", e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void consume() {
        while (true) {
            try {
                Message message = consumer.receive(2, TimeUnit.SECONDS);
                if (message != null) {
                    //String dspEvent = new String(message.getData(), StandardCharsets.UTF_8);
                    //String dspEvent = new String((byte[])message.getValue(), StandardCharsets.UTF_8);
                    System.out.print("[" + message.getTopicName() + "] ");
                    //InputEvent inputEvent = mapper.readValue(dspEvent, InputEvent.class);
                    //System.out.println(dspEvent);
                    //System.out.println(inputEvent);
                    consumer.acknowledge(message);
                    System.out.println("Acknowledged message " + message.getMessageId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //public void consume() {
        //        registerShutdownHook();
//
//        int lastGlobalCounter = 0;
//        Map<String,Integer> lastTopicCounterMap = new HashMap<>();
//
//        while (!isCancelled) {
//            if(!fillMessages())
//                break; // needs proper error handling rather than breaking out of loop
//
//            NextMessage nextMsg = getNextMessage();
//            if(nextMsg != null) {
//                lastGlobalCounter = processMessage(nextMsg, lastTopicCounterMap, lastGlobalCounter);
//            }
//        }
    //}

    private boolean fillMessages() {
        try {
            for (int i = 0; i < topics.size(); i++) {
                if (messages[i] == null) {
                    Message message = consumers.get(i).receive();
                    if (message != null)
                        messages[i] = message;
                }
            }

            return true;
        }
        catch(PulsarClientException e) {
            e.printStackTrace();
            return false;
        }
    }

    private NextMessage getNextMessage() {
        long minTimestamp = Long.MAX_VALUE;
        int targetIndex = -1;
        for(int i=0; i<topics.size(); i++) {
            Message msg = messages[i];
            if(msg != null) {
                long timestamp = msg.getPublishTime();
                if (timestamp < minTimestamp) {
                    minTimestamp = timestamp;
                    targetIndex = i;
                }
            }
        }

        return new NextMessage(messages[targetIndex], targetIndex);
    }

    private int processMessage(NextMessage nextMsg,
                               Map<String,Integer> lastTopicCounterMap,
                               int lastGlobalCounter) {
        Message msg = nextMsg.getMessage();
        Consumer consumer = consumers.get(nextMsg.getIndex());
        String consumerId = consumerIds.get(nextMsg.getIndex());

        int globalCounter = 0;
        try {
            String payload = new String(msg.getData());

            String seqNoKey = msg.getTopicName();
            globalCounter = Integer.valueOf(payload.split(",")[0]);
            int topicCounter = Integer.valueOf(payload.split(",")[1]);

            int lastTopicCounter = 0;
            if (lastTopicCounterMap.containsKey(seqNoKey))
                lastTopicCounter = lastTopicCounterMap.get(seqNoKey);

            String topicOrderLabel = getOrderingText(lastTopicCounter, topicCounter);
            String globalOrderLabel = getOrderingText(lastGlobalCounter, globalCounter);

            print(consumerId, MessageFormat.format("Topic: {0}, TC: {1} {2}, GC: {3} {4}",
                    msg.getTopicName().substring(msg.getTopicName().lastIndexOf("/")+1, msg.getTopicName().indexOf("_")),
                    topicCounter,
                    topicOrderLabel,
                    globalCounter,
                    globalOrderLabel));

            lastTopicCounterMap.put(seqNoKey, topicCounter);

            // Acknowledge the message so that it can be deleted by the message broker
            consumer.acknowledge(msg);
            messages[nextMsg.getIndex()] = null;
        } catch (Exception e) {
            e.printStackTrace();
            // Message failed to process, redeliver later
            consumer.negativeAcknowledge(msg);
        }

        return globalCounter;
    }

    private String getOrderingText(int last, int current) {
        if(last + 1 < current)
            return "JUMP FORWARD " + (current - (last + 1));
        else if(last + 1 > current)
            return "JUMP BACKWARDS " + (last - current);
        else
            return "OK";
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                isCancelled = true;

                while (!isCompleted)
                    waitFor(100);
            }
        });
    }

    private void waitFor(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void print(String conId, String text) {
        System.out.println(Instant.now() + " : Consumer " + conId + " : " + text);
    }

    private void tryClose() {
        try {
            for(Consumer consumer : consumers)
                consumer.close();
            client.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
