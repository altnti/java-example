package com.ac.disruptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * about test information.
 * call reset before start test!!!
 */
class QueueConfig {
    public static final int STEP           = 1;
    public static final int TOTAL_SIZE     = 1000000;
    public static final int PUBLISHER_SIZE = 5;
    public static final int CONSUMER_SIZE  = 2;
    public static final int QUEUE_SIZE     = 1024;

    public static AtomicInteger publishNum  = new AtomicInteger(PUBLISHER_SIZE);  //final == 0
    public static AtomicInteger count       = new AtomicInteger(0);  //final == totalSize

    public static void reset(){
        publishNum.set(PUBLISHER_SIZE);
        count.set(0);
    }
}
