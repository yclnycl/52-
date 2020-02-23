import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.StaticLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class LoginDown {

    static String videoPath = "D:\\video\\";

    private String baseUrl = "https://www.52investing.com/";

    ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        String path = "C:\\Users\\yclny\\Desktop\\"+"小姑夫视频.txt";
        FileReader fileReader = new FileReader(path);
        List<String> list1 = new ArrayList<>();
        fileReader.readLines(new LineHandler() {
            @SneakyThrows
            @Override
            public void handle(String s) {
                list1.add(s);

            }
        });

        for (String s:
                list1) {

            StaticLog.info(DateUtil.now());
            List<String> list = getPageListBy(s);

            int threadCount = list.size();
            Thread tenMinTaskThread = new Thread(Index.tenMinTask());
            tenMinTaskThread.start();

            CountDownLatch begin = new CountDownLatch(1);

            CountDownLatch end = new CountDownLatch(threadCount);

            begin.countDown();
            list.forEach(item -> {
                Runnable runnable = Index.downTread(item, begin, end);
                new Thread(runnable).start();
            });
            //多个线程都执行结束
            try {
                end.await();
                tenMinTaskThread.interrupt();
                System.out.println("多个线程都执行结束，可以做自己的事情了");
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("多线程执行中出错了，凉凉了！！！");
            }
        }
        return;


    }

    public static List<String> getPageListBy(String url) throws IOException {

        List<String> pageInfos = new ArrayList<>();
        Document document = Jsoup.connect(url).ignoreContentType(true).get();
        Elements elements = document.select(".muliList>.perItem");

        elements.forEach(item -> {
            String id = item.attr("id");
            String title = item.getElementsByClass("name").text().replaceAll("[/\\\\:*?|]", ".").replaceAll("[\"<>]", "'");
            pageInfos.add(id + "_" + title);
        });

        return pageInfos;
    }

}


