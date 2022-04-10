import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import Helper.PayLoad;
import Helper.SkierRes;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet(name = "SkierServlet",
        value = {"/SkierServlet"})
public class SkierServlet extends HttpServlet {
    private Gson gson = new Gson();
    private final static String QUEUE_NAME_1 = "skier_service_queue";
    private final static String QUEUE_NAME_2 = "resort_service_queue";
    private final BlockingQueue<Channel> channelPool = new ArrayBlockingQueue<Channel>(200);
    private final EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(2000, 2, TimeUnit.SECONDS, 1500);
    private final AtomicInteger exp = new AtomicInteger(1);


    public SkierServlet() {
        super();
    }

    public void init(ServletConfig config) {
        // use hard coded private IP address here for the RabbitMQ address
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.31.16.156");
        try {
            Connection conn = factory.newConnection();
            for (int i = 0; i < 200; i++) {
                Channel ch = conn.createChannel();
                channelPool.offer(ch);
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }

    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(getReturnMessage(gson,"invalid url"));
            return;
        }
        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts, "POST")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(getReturnMessage(gson,"invalid url"));
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (!verifyJson(sb.toString())) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write(getReturnMessage(gson,"invalid json POST"));
                return;
            }
            res.setStatus(HttpServletResponse.SC_CREATED);
            res.getWriter().write("It works for POST!");
            Integer resortID = parseInt(urlParts[1]);
            Integer seasonID = parseInt(urlParts[3]);
            Integer daysID = parseInt(urlParts[5]);
            Integer skierID = parseInt(urlParts[7]);
            SkierRes post = gson.fromJson(sb.toString(), SkierRes.class);
            PayLoad ride = new PayLoad(resortID, seasonID, daysID, skierID, post.getLiftID(), post.getTime(), post.getWaitTime());
            String message = gson.toJson(ride);
            while (true) {
                if (breaker.incrementAndCheckState()) {
                    Channel channel = null;
                    try {
                        channel = channelPool.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //send message to both queues
                    channel.queueDeclare(QUEUE_NAME_1, true, false, false, null);
                    channel.basicPublish("", QUEUE_NAME_1, null, message.getBytes(StandardCharsets.UTF_8));
                    channel.queueDeclare(QUEUE_NAME_2, true, false, false, null);
                    channel.basicPublish("", QUEUE_NAME_2, null, message.getBytes(StandardCharsets.UTF_8));
                    channelPool.offer(channel);
                    break;
                } else {
                    int uExp = Math.min(14, exp.getAndIncrement());
                    int maxSleepTime = (int)Math.pow(2.0, (double)uExp);
                    int sleepTime = (int)(Math.random() * maxSleepTime);
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(getReturnMessage(gson,"invalid url"));
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts, "GET")) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(getReturnMessage(gson,"invalid url"));
        } else {
            if (urlParts.length == 3) {
                String resortStr = req.getParameter("resort");
                String skierIDStr = req.getParameter("skierID");
                String seasonIDStr = req.getParameter("season");
                Integer resortID = parseInt(resortStr);
                Integer skierID = parseInt(skierIDStr);
                Integer seasonID = parseInt(skierIDStr);
                if (resortID == null || skierID == null || (seasonIDStr != null && seasonID == null)) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().write(getReturnMessage(gson,"invalid parameters"));
                    return;
                }
            }
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            res.getWriter().write("It works for GET!");
            res.getWriter().write(urlPath);
        }
    }

    private boolean isUrlValid(String[] urlPath, String source){
        if (urlPath.length == 8){
            if (urlPath[0].length() != 0) return false;
            Integer resortID = parseInt(urlPath[1]);
            if (resortID == null) return false;
            if (!urlPath[2].equals("seasons")) return false;
            Integer seasonID = parseInt(urlPath[3]);
            if (seasonID == null) return false;
            if (!urlPath[4].equals("days")) return false;
            Integer daysID = parseInt(urlPath[5]);
            if (daysID == null) return false;
            if (daysID < 1 || daysID > 366) return false;
            if (!urlPath[6].equals("skiers")) return false;
            Integer skierID = parseInt(urlPath[7]);
            return skierID != null;
        } else if (source.equals("GET") && urlPath.length == 3) {
            if (urlPath[0].length() != 0) return false;
            Integer skierID = parseInt(urlPath[1]);
            if (skierID == null) return false;
            return urlPath[2].equals("vertical");
        }
        return false;
    }

    private boolean verifyJson(String str) {
        SkierRes post = gson.fromJson(str, SkierRes.class);
        Map map = gson.fromJson(str, Map.class);
        if (map.size() != 3) return false;
        return post.getLiftID() != null && post.getTime() != null && post.getWaitTime() != null;
    }

    private static String getReturnMessage(Gson gson, String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return gson.toJson(map);
    }

    private static Integer parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return null;
        }
    }

}
