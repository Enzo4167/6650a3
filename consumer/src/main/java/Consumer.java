import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.JedisPool;

import javax.xml.bind.*;
import java.io.IOException;
import java.util.concurrent.*;

public class Consumer {
    private final static String IP_MQ = "172.31.16.156";
    private final static String IP_REDIS = "172.31.5.213";

    public static void main(String[] args) throws JAXBException, IOException, TimeoutException {
        int threadNum = 0;
        String type = null;

        if (args.length == 2){
            threadNum = Integer.parseInt(args[0]);
            type = args[1];
        } else {
            System.err.println("wrong number of arguments");
        }
        if (threadNum <= 0) {
            System.err.println("threads number not allowed");
            return;
        }
        JedisPool pool = new JedisPool(IP_REDIS, 6379);
        int numThreads = threadNum;
        String QUEUE_NAME = type + "_queue";
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_MQ);
        factory.setUsername("guest");
        factory.setPassword("guest");
        //factory.setVirtualHost("/");
        factory.setPort(5672);
        Connection conn = factory.newConnection();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            Channel channel = conn.createChannel();
            channel.basicQos(1);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            executor.execute(new ConsumerThread(pool, channel, QUEUE_NAME, type));
        }
        executor.shutdown();
    }



}
