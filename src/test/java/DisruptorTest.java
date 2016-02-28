import com.ac.disruptor.DisruptorExample;
import com.ac.disruptor.PollerExample;
import com.ac.disruptor.WorkExample;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

public class DisruptorTest {
    @org.junit.Test
    public void testDisruptor() throws ExecutionException, InterruptedException {
        assertTrue( new DisruptorExample().test() );
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
