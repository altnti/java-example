package com.ac.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;

class publisher implements Runnable {
    RingBuffer<AddEvent> ringBuffer;

    publisher( RingBuffer<AddEvent> ringBuffer){
        this.ringBuffer = ringBuffer;
    }

    static final EventTranslatorOneArg<AddEvent, Integer> TRANSLATOR = new EventTranslatorOneArg<AddEvent, Integer>(){
        public void translateTo(AddEvent event, long sequence, Integer size) {
            event.setSize( size );
        }
    };

    public void run() {
        int count = QueueConfig.TOTAL_SIZE;

        while ( --count >= 0 ){
            ringBuffer.publishEvent(TRANSLATOR, QueueConfig.STEP);
        }

        System.out.println("thread end, id: " + QueueConfig.publishNum.decrementAndGet());
    }
}
