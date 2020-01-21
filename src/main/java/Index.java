import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.log.StaticLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Index {


    static ObjectMapper objectMapper = new ObjectMapper();

    static String videoPath = "D:\\video";

    public static void main(String[] args) throws IOException {
        int maxPage = 10;
        int page = 0;
        FileWriter writer = new FileWriter("D:\\video\\test.txt");
        while (page <= maxPage) {
            StaticLog.info(DateUtil.now());
            String url = "https://www.52investing.com/mobile/showback/0_3_0_0_0_" + page + "_10";
            System.out.println(url);
            page++;
            List<String> list = getPageListBy(url);

            int threadCount = list.size();
            Thread tenMinTaskThread = new Thread(tenMinTask());
            tenMinTaskThread.start();

            CountDownLatch begin = new CountDownLatch(1);

            CountDownLatch end = new CountDownLatch(threadCount);

            begin.countDown();
            list.forEach(item -> {
                Runnable runnable = downTread(item, begin, end);
                new Thread(runnable).start();
            });
            //多个线程都执行结束
            try {
                end.await();
                tenMinTaskThread.interrupt();
                System.out.println("多个线程都执行结束，可以做自己的事情了");
                writer.write(url+System.getProperty("line.separator"));
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("多线程执行中出错了，凉凉了！！！");
            }
        }
        return;
    }

    public static List<String> getPageListBy(String url) throws IOException {

        Document document = Jsoup.connect(url).ignoreContentType(true).get();

        Elements items = document.getElementsByClass("perItem");

        List<String> pageInfos = new ArrayList<>();
        items.forEach(item -> {
            String pageHref = item.attr("href");
            String title = item.getElementsByClass("title").text().replaceAll("[/\\\\:*?|]", ".").replaceAll("[\"<>]", "'");
            pageInfos.add(ReUtil.getFirstNumber(pageHref).toString() + "_" + title);
        });

        return pageInfos;
    }

    public static void downM3u8VideoById(String str) throws IOException {
        String cid = str.split("_")[0];
        String vid = getVidByCid(cid);
        String token16 = getToken16ByVid(vid);
        String json = DecryptCdn.decryptCdn(vid, token16);
        Map map = objectMapper.readValue(json, Map.class);
        List<String> hlsLit = (List<String>) map.get("hls");
        String hlsUrl = hlsLit.get(hlsLit.size() - 1);
        Map videoMap = new HashMap();
        videoMap.put("title", str);
        videoMap.put("plvurl", hlsUrl);
        downVideoByPlvId(videoMap);
    }

    public static String getVidByCid(String cid) throws JsonProcessingException {
        String url = "https://www.52investing.com/52crm/app/index.php?method52=b.livepro.watchvod";
        Map map = new HashMap();
        map.put("url", "/52crm/app/index.php?method52=b.livepro.getblwtoken");
        map.put("id", cid);

        String result = HttpRequest.post(url).form(map).timeout(5000).execute().body();
        Map<String, Map<String, Object>> resultMap = objectMapper.readValue(result, Map.class);

        return resultMap.get("data").get("plvid").toString();
    }

    public static String getToken16ByVid(String vid) throws JsonProcessingException {
        String url = "https://player.polyv.net/secure/" + vid + ".json";
        String result = HttpRequest.get(url).execute().body();
        Map<String, String> resultMap = objectMapper.readValue(result, Map.class);
        return resultMap.get("body");
    }

    public static void downVideoByPlvId(Map map) {
        String path = videoPath + File.separator + map.get("title");
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        } else {
            return;
        }

        String cmd = "D:\\sofe\\ffmpeg\\bin\\ffmpeg.exe -i " + map.get("plvurl") + " -c copy " + path + File.separator + map.get("title") + ".mp4";
        StaticLog.info(cmd);
        RuntimeUtil.execForStr(cmd);
    }

    public static Runnable downTread(String string, CountDownLatch begin, CountDownLatch end) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    System.out.println("线程" + string + ":--------------------->开始工作");
                    begin.await();

                    downM3u8VideoById(string);

                    end.countDown();
                    System.out.println("线程" + string + ":--------------------->结束工作");
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }

            }
        };
        return runnable;
    }

    public static Runnable tenMinTask() {
        Runnable runnable = new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                try {
                    StaticLog.info("创建定时线程");
                    Thread.sleep(480000);

                    String cmd = "taskkill /F /im ffmpeg.exe";
                    RuntimeUtil.execForStr(cmd);
                } catch (InterruptedException e) {

                    System.out.println("在沉睡中被停止!进入catch!");

                    e.printStackTrace();
                }

            }
        };
        return runnable;
    }
}
