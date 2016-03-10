package com.ac.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AddEventHandler implements EventHandler<AddEvent> {
    public void onEvent(AddEvent event, long sequence, boolean endOfBatch) throws Exception {
        QueueConfig.count.addAndGet( event.getSize() );
    }
}

public class DisruptorExample {
    public DisruptorExample(){
        QueueConfig.reset();
    }

    public boolean test() throws InterruptedException {
        Disruptor<AddEvent> disruptor = new Disruptor<AddEvent>( new AddEventFactory(),
                4096, Executors.newFixedThreadPool(QueueConfig.CONSUMER_SIZE) );
        disruptor.handleEventsWith( new AddEventHandler() );
        disruptor.start();
        RingBuffer<AddEvent> ringBuffer = disruptor.getRingBuffer();

        ExecutorService es = Executors.newFixedThreadPool(QueueConfig.PUBLISHER_SIZE+QueueConfig.CONSUMER_SIZE);
        for ( int i=0; i<QueueConfig.PUBLISHER_SIZE; ++i ){
            es.submit(new publisher(ringBuffer));
        }

        int tick = 0;
        while (tick<121&&QueueConfig.count.get()!=QueueConfig.PUBLISHER_SIZE*QueueConfig.TOTAL_SIZE){
            ++tick;
            Thread.sleep(500);
        }

        System.out.println("count:" + QueueConfig.count.get());
        return QueueConfig.count.get() == QueueConfig.PUBLISHER_SIZE*QueueConfig.TOTAL_SIZE;
    }
}
