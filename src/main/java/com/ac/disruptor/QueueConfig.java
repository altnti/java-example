package com.ac.disruptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2016/2/20.
 */
class QueueConfig {
    public static AtomicInteger publishNum  = new AtomicInteger(5);  //final == 0
    public static AtomicInteger count       = new AtomicInteger(0);  //final == totalSize

    public static final int STEP          = 1;
    public static final int TOTAL_SIZE    = 1000000;
    public static final int CONSUMER_SIZE = 3;
    public static final int QUEUE_SIZE    = 1024;
}
