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
                    .brokerUri("amqp://guest:123456@10.12.6.19:5672/10.12.6.19")
                    .backendUri("rpc://guest:123456@10.12.6.19:5672/10.12.6.19")
                    .maxPriority(p)
                    .build();

            for(int i=0 ;i <100 ; i++){
               Celery.AsyncResult a = client.submit("app_tasks.add", i % 10, new Object[]{i % 10, 0});
               while(!a.isDone()){
                   Thread.sleep(100);
               }


               System.out.println(a.getTaskId() + "~~~~ result is :"+ a.get());
            }

        }catch (Exception ex){

        }

    }
}
