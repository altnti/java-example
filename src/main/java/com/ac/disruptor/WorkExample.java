package com.ac.disruptor;

import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.WorkerPool;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class WorkHandlerImpl implements WorkHandler<AddEvent>{
    public void onEvent(AddEvent addEvent) throws Exception {
        QueueConfig.count.addAndGet( addEvent.getSize() );
    }
}

public class WorkExample {
    public boolean test() throws InterruptedException {
        WorkerPool<AddEvent> pool = new WorkerPool<AddEvent>( new AddEventFactory(), new FatalExceptionHandler(),
                new WorkHandlerImpl(), new WorkHandlerImpl(), new WorkHandlerImpl() );
        RingBuffer<AddEvent> ringBuffer = pool.start(Executors.newCachedThreadPool());

        ExecutorService executorService = Executors.newFixedThreadPool(QueueConfig.CONSUMER_SIZE+QueueConfig.PUBLISHER_SIZE);
        for ( int i=0; i<QueueConfig.PUBLISHER_SIZE; ++i ){
            executorService.submit(new publisher(ringBuffer));
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
