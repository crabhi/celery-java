import com.geneea.celery.Celery;
import org.junit.Test;

import java.util.Optional;

/**
 * Created by stone on 18-3-7.
 */
public class TestCases {

    @Test
    public void testPriority(){
        try{
            Optional<Integer> p = Optional.of(10);

            Celery client = Celery.builder()
                    .queue("test_idata1")
                    .brokerUri("amqp://localhost/%2F")
                    .backendUri("rpc://localhost/%2F")
                    .maxPriority(p)
                    .build();

            for(int i=0 ;i <100 ; i++){
                client.submit("app_tasks.add", i % 10, new Object[]{i % 10, 0});
            }

        }catch (Exception ex){

        }

    }
}
