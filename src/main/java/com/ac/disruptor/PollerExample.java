package com.ac.disruptor;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class PollCall implements Runnable{
    EventPoller<AddEvent> eventPoller;
    public PollCall( EventPoller<AddEvent> eventPoller ){
        this.eventPoller = eventPoller;
    }

    public void run() {
        while (QueueConfig.publishNum.get() != 0) try {
            EventPoller.PollState state = eventPoller.poll(new EventPoller.Handler<AddEvent>() {
                public boolean onEvent(AddEvent event, long sequence, boolean endOfBatch) throws Exception {
                    QueueConfig.count.addAndGet( event.getSize() );
                    return true;
                }
            });

            if ( state == EventPoller.PollState.IDLE ){
                Thread.sleep(9);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class PollerExample {
    public void test() throws ExecutionException, InterruptedException {
        RingBuffer<AddEvent> ringBuffer = RingBuffer.createMultiProducer(new AddEventFactory(), QueueConfig.QUEUE_SIZE);
        ExecutorService es = Executors.newFixedThreadPool(QueueConfig.publishNum.get() + QueueConfig.CONSUMER_SIZE);
        //创建消费者
        for ( int i=0; i<QueueConfig.CONSUMER_SIZE; ++i ){
            EventPoller<AddEvent> poller = ringBuffer.newPoller();
            ringBuffer.addGatingSequences(poller.getSequence());
            es.submit( new PollCall(poller) );
        }
        //创建生产者
        List<Future> list   = new ArrayList<Future>();
        final int threadNum = QueueConfig.publishNum.get();
        for ( int i=0; i<threadNum; ++i ){
            list.add( es.submit(new publisher(ringBuffer)) );
        }
        //等待生产者线程退出
        for ( Future f :list ){
            f.get();
        }
    }
}
