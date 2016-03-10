package com.ac.disruptor;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class PollCall implements Runnable {
    EventPoller<AddEvent> eventPoller;
    final EventPoller.Handler<AddEvent> handler = new  EventPoller.Handler<AddEvent>() {
        public boolean onEvent(AddEvent event, long sequence, boolean endOfBatch) throws Exception {
            QueueConfig.count.addAndGet(event.getSize());
            return true;
        }
    };

    public PollCall(EventPoller<AddEvent> eventPoller) {
        this.eventPoller = eventPoller;
    }

    public void run() {
        while (QueueConfig.count.get() / QueueConfig.CONSUMER_SIZE != QueueConfig.PUBLISHER_SIZE * QueueConfig.TOTAL_SIZE) {
            try {
                EventPoller.PollState state = eventPoller.poll( handler );

                if (state == EventPoller.PollState.IDLE) {
                    //Thread.sleep(9);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * publisher:n -> event -> all of consumer
 *
 * use flag(example AtomicBoolean) to implement event process by one of consumer.
 * Note: publisher must reset flag.
 *
 */
public class PollerExample {
    public PollerExample() {
        QueueConfig.reset();
    }

    public boolean test() throws ExecutionException, InterruptedException {
        RingBuffer<AddEvent> ringBuffer = RingBuffer.createMultiProducer(new AddEventFactory(), QueueConfig.QUEUE_SIZE);
        ExecutorService executorService = Executors.newFixedThreadPool(QueueConfig.PUBLISHER_SIZE+QueueConfig.CONSUMER_SIZE);
        List<Future> list = new ArrayList<Future>();
        /*
        create consumers
        Note: every event process by all consumers!!!
         */
        for (int i = 0; i < QueueConfig.CONSUMER_SIZE; ++i) {
            EventPoller<AddEvent> poller = ringBuffer.newPoller();
            ringBuffer.addGatingSequences(poller.getSequence());
            list.add(executorService.submit(new PollCall(poller)));
        }
        //create producers/publishers
        for (int i = 0; i < QueueConfig.PUBLISHER_SIZE; ++i) {
            list.add(executorService.submit(new publisher(ringBuffer)));
        }
        //wait the all threads exit.
        for (Future f : list) {
            f.get();
        }

        System.out.println("count:" + QueueConfig.count.get());
        return QueueConfig.count.get()/QueueConfig.CONSUMER_SIZE == QueueConfig.TOTAL_SIZE * QueueConfig.PUBLISHER_SIZE;
    }
}
