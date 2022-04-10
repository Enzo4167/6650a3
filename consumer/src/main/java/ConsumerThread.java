import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import Helper.PayLoad;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsumerThread implements Runnable{
    private final JedisPool jedisPool;
    private final Channel channel;
    private final Gson gson = new Gson();
    private final String queueName;
    private final String type;

    public ConsumerThread(JedisPool jedisPool, Channel channel, String queueName, String type) {
        this.jedisPool = jedisPool;
        this.channel = channel;
        this.queueName = queueName;
        this.type = type;
    }

    @Override
    public void run() {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            PayLoad ride = gson.fromJson(message, PayLoad.class);
            Integer userId = ride.getSkierId();
            Integer seasonId = ride.getSeasonId();
            Integer daysId = ride.getDaysId();
            Integer liftId = ride.getLiftID();
            Integer resortId = ride.getResortId();
            Integer time = ride.getTime();
            try (Jedis jedis = jedisPool.getResource()) {
                //System.out.println("Greedy is good");
                if (type.equals("skier_service")) {
                    jedis.sadd("1" + "User" + userId + ":Season" + seasonId + ":Days", "" + daysId);
                    jedis.sadd("1" + "User" + userId + ":Season" + seasonId + ":Days" + daysId + ":Lifts", "" + liftId);
                    String verticalKey = "1" + "User" + userId + ":Season" + seasonId + ":Days" + daysId + ":vertical";
                    int vertical = 10 * liftId;
                    jedis.incrBy(verticalKey, (long)vertical);
                } else if (type.equals("resort_service")) {
                    jedis.sadd("2" + "Resort" + resortId + ":Season" + seasonId + ":Days" + daysId + ":Users", "" + userId);
                    jedis.incr("2" + "Lift" + liftId + ":Season" + seasonId + ":Days" + daysId + ":Frequency");
                    int hour = (time - 1) / 60;
                    jedis.incr("2" + "Season" + seasonId + ":Days" + daysId + ":Hours" + hour + "frequency");
                }
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        try {
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
            System.out.println("unseccesful");
            Logger.getLogger(ConsumerThread.class.getName()).log(Level.SEVERE, null, e);
        }

    }


}
