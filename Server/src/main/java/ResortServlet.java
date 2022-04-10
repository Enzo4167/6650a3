import com.google.gson.Gson;
import Helper.ResortRes;
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

@WebServlet(name = "ResortServlet")
public class ResortServlet extends HttpServlet {
    private final Gson gson = new Gson();

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(getReturnMessage(gson, "invalid url"));
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (isUrlValid(urlParts, "POST") == -1) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(getReturnMessage(gson, "invalid url"));
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (!validatePostJson(sb.toString())) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write(getReturnMessage(gson, "invalid json POST"));
                return;
            }
            res.setStatus(HttpServletResponse.SC_CREATED);
            // do any sophisticated processing with urlParts which contains all the url params
            res.getWriter().write("It works for POST!");
            res.getWriter().write(sb.toString());
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath != null && !urlPath.isEmpty()) {
            String[] urlParts = urlPath.split("/");
            int code = isUrlValid(urlParts, "GET");
            if (code == -1) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write(getReturnMessage(gson, "invalid url"));
                return;
            } else if (code == -2) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write(getReturnMessage(gson, "invalid resort id"));
                return;
            }
        }
        res.setStatus(HttpServletResponse.SC_OK);
        // do any sophisticated processing with urlParts which contains all the url params
        res.getWriter().write("It works for GET!");
        if (urlPath != null) {
            res.getWriter().write(urlPath);
        }
    }

    private boolean validatePostJson(String str) {
        ResortRes post = gson.fromJson(str, ResortRes.class);
        Map map = gson.fromJson(str, Map.class);
        if (map.size() != 1) return false;
        if (post.getYear() == null) return false;
        try {
            Integer year = parseInt(post.getYear());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private int isUrlValid(String[] urlPath, String source) {
        if (source.equals("GET")) {
            if (urlPath.length == 0 || (urlPath.length == 1 && urlPath[0].length() == 0)) {
                return 0;
            }
            if (urlPath.length == 3) {
                if (urlPath[0].length() != 0) return -1;
                Integer resortID = parseInt(urlPath[1]);
                if (resortID == null) return -1;
                if (!urlPath[2].equals("seasons")) return -1;
                return 0;
            }
            if (urlPath.length == 7) {
                if (urlPath[0].length() != 0) return -1;
                Integer resortID = parseInt(urlPath[1]);
                if (resortID == null) return -2;
                if (!urlPath[2].equals("seasons")) return -1;
                Integer seasonID = parseInt(urlPath[3]);
                if (seasonID == null) return -1;
                if (!urlPath[4].equals("days")) return -1;
                Integer daysID = parseInt(urlPath[5]);
                if (daysID == null) return -1;
                if (daysID < 1 || daysID > 366) return -1;
                if(!urlPath[6].equals("skiers")) return -1;
                return 0;
            }
        } else if (source.equals("POST")) {
            if (urlPath.length == 3) {
                if (urlPath[0].length() != 0) return -1;
                Integer resortID = parseInt(urlPath[1]);
                if (resortID == null) return -2;
                if (!urlPath[2].equals("seasons")) return -1;
                return 0;
            }
        }
        return -1;
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
