import com.ac.disruptor.PollerExample;
import com.ac.disruptor.WorkExample;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

class ConfigInfo {
    public static AtomicInteger thredNum  = new AtomicInteger(1000);
    public static AtomicInteger queueSize = new AtomicInteger(0);
    public static AtomicInteger totalSize = new AtomicInteger(0);
    public static final int count = 1000000;
}

public class QueueTest {
    static class Consumer implements Runnable {
        private final BlockingQueue<LogEvent> spanQueue;

        public Consumer(BlockingQueue<LogEvent> spanQueue){
            this.spanQueue = spanQueue;
        }

        public void run() {
            while ( ConfigInfo.thredNum.get()!=0 ) {
                try {
                    ConfigInfo.totalSize.addAndGet(spanQueue.poll(5, TimeUnit.SECONDS).getSize());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println( "cosumer exit." );
        }
    }

    static class Producter implements Runnable{
        private final BlockingQueue<LogEvent> spanQueue;

        public Producter(BlockingQueue<LogEvent> spanQueue){
            this.spanQueue = spanQueue;
        }

        public void run() {
            int cout = ConfigInfo.count;
            while ( --cout > 0 ){
                LogEvent logEvent = new LogEvent();
                logEvent.setSize(1);
                spanQueue.offer(logEvent);
            }
            int num = ConfigInfo.thredNum.decrementAndGet();
            //System.out.println("thread end, id: " + num);
        }
    }

    @org.junit.Test
    public void testBlockQueue() throws InterruptedException, ExecutionException {
        BlockingQueue<LogEvent> spanQueue = new ArrayBlockingQueue<LogEvent>(5000);
        ExecutorService es = Executors.newFixedThreadPool(ConfigInfo.thredNum.get()+3);
        List<Future> list = new ArrayList<Future>();
        list.add(es.submit(new Consumer(spanQueue)));
        list.add(es.submit(new Consumer(spanQueue)));
        list.add(es.submit(new Consumer(spanQueue)));
        int threadNum = ConfigInfo.thredNum.get();
        for ( int i=0; i<threadNum; ++i ){
            list.add(es.submit(new Producter(spanQueue)));
        }

        for ( Future f :list ){
            f.get();
        }
        System.out.println( ConfigInfo.totalSize.get() );
        es.shutdown();
    }

    static class NoConsumer implements Runnable {
        private final ConcurrentLinkedQueue<LogEvent> queue;

        public NoConsumer(ConcurrentLinkedQueue<LogEvent> queue){
            this.queue = queue;
        }

        public void run() {
            while ( ConfigInfo.thredNum.get() != 0 ) {
                if ( !queue.isEmpty() ) {
                    queue.poll();
                    //ConfigInfo.totalSize.addAndGet(queue.poll().getSize());
                    ConfigInfo.queueSize.getAndDecrement();
                }
            }
            System.out.println("cosumer exit.");
        }
    }

    static class NoProducter implements Runnable{
        private final ConcurrentLinkedQueue<LogEvent> queue;

        public NoProducter(ConcurrentLinkedQueue<LogEvent> queue){
            this.queue = queue;
        }

        public void run() {
            int count = ConfigInfo.count;
            while ( --count > 0 ){
                if ( ConfigInfo.queueSize.get() < 5000 ) {
                    ConfigInfo.queueSize.getAndIncrement();
                    LogEvent logEvent = new LogEvent();
                    logEvent.setSize(1);
                    queue.offer(logEvent);
                }
            }
            int num = ConfigInfo.thredNum.decrementAndGet();
            //System.out.println("thread end, id: " + num);
        }
    }

    @org.junit.Test
    public void testNoBlockQueue() throws InterruptedException, ExecutionException {
        ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<LogEvent>();
        ExecutorService es = Executors.newFixedThreadPool(ConfigInfo.thredNum.get()+3);
        List<Future> list = new ArrayList<Future>();
        list.add(es.submit(new NoConsumer(queue)));
        list.add(es.submit(new NoConsumer(queue)));
        list.add(es.submit(new NoConsumer(queue)));
        int threadNum = ConfigInfo.thredNum.get();
        for ( int i=0; i<threadNum; ++i ){
            list.add( es.submit(new NoProducter(queue)) );
        }

        for ( Future f :list ){
            if ( f != null ) {
                f.get();
            }
        }
        es.shutdown();

        System.out.println(ConfigInfo.totalSize.get());
    }

    static class LogEvent{
        public void setSize(int size){
            this.size = size;
        }
        public int getSize(){
            return size;
        }

        private int size;
    }

    static class LogEventFactory implements EventFactory<LogEvent>{

        public LogEvent newInstance() {
            return new LogEvent();
        }
    }

    static class LogEventProducerWithTranslator implements Runnable {
        RingBuffer<LogEvent> ringBuffer;

        LogEventProducerWithTranslator( RingBuffer<LogEvent> ringBuffer){
            this.ringBuffer = ringBuffer;
        }

        static final EventTranslatorOneArg<LogEvent, Integer> TRANSLATOR = new EventTranslatorOneArg<LogEvent, Integer>(){
            public void translateTo(LogEvent event, long sequence, Integer arg0) {
                event.setSize( arg0 );
            }
        };

        public void run() {
            int count = ConfigInfo.count;
            while ( --count > 0 ){
                if ( ringBuffer.remainingCapacity() > 0 ) {
                    ringBuffer.publishEvent(TRANSLATOR, 1);
                }
            }
            int num = ConfigInfo.thredNum.decrementAndGet();
            System.out.println("thread end, id: " + num);
        }
    }

    static class LogEventHandler implements WorkHandler<LogEvent>, EventHandler<LogEvent>{
        private int id;
        public LogEventHandler(int id){
            this.id = id;
        }

        public void onEvent(LogEvent event, long sequence, boolean endOfBatch) throws Exception {
            ConfigInfo.totalSize.addAndGet( event.getSize() );
        }

        public void onEvent(LogEvent event) throws Exception {
            ConfigInfo.totalSize.addAndGet(event.getSize());
            //System.out.println( id + ":" + event.getSize() );
        }
    }

    @org.junit.Test
    public void testDisruptor() throws ExecutionException, InterruptedException {
        Disruptor<LogEvent> disruptor = new Disruptor<LogEvent>( new LogEventFactory(),
                4096, Executors.newFixedThreadPool(3), ProducerType.MULTI, new YieldingWaitStrategy() );
        disruptor.handleEventsWith( new LogEventHandler(0) );
        disruptor.start();
        RingBuffer<LogEvent> ringBuffer = disruptor.getRingBuffer();

        ExecutorService es = Executors.newFixedThreadPool(ConfigInfo.thredNum.get()+3);
        List<Future> list = new ArrayList<Future>();
        int threadNum = ConfigInfo.thredNum.get();
        for ( int i=0; i<threadNum; ++i ){
            list.add( es.submit(new LogEventProducerWithTranslator(ringBuffer)) );
        }
        for ( Future f :list ){
            f.get();
        }

        System.out.println( ConfigInfo.totalSize.get() );
    }

    @org.junit.Test
    public void testWorkPool() throws ExecutionException, InterruptedException {
        assertTrue( new WorkExample().test() );
    }

    @org.junit.Test
    public void testPoller() throws ExecutionException, InterruptedException {
        assertTrue( new PollerExample().test() );
    }
}
