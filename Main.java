import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {


  public static final String DEF_CHATSET = "UTF-8";
  public static final int DEF_CONN_TIMEOUT = 30000;
  public static final int DEF_READ_TIMEOUT = 30000;
  private static String rootPath = "";

  /**
   * 解析歌单
   *
   * @param playListId 歌单ID
   * @return
   */
  public Map<String, String> getList(String playListId) {
    Map<String, String> idAndName = new HashMap<>();
    String html = getBody("https://music.163.com/playlist?id=" + playListId);
    html = html.replace(" ", "");
    html = html.replace("\"", "");
    int index = html.indexOf("<ulclass=f-hide>");
    html = html.substring(index);
    int endIndex = html.indexOf("</ul>");
    html = html.substring(16, endIndex);
    String[] ss = html.split("</a></li>");
    for (int i = 0; i < ss.length; i++) {
      String s = ss[i].replace("<li><ahref=/song?id=", "");
      String[] kv = s.split(">");
      if (kv.length == 2) {
        idAndName.put(kv[0], kv[1]);
      }
    }
    return idAndName;
  }


  public static void main(String[] args) {
    Main main = new Main();
    // String s = main.getBody("https://music.163.com/playlist?id=2204388891");
    //int a = s.indexOf("song?id=");
    //System.out.println(a);
    Scanner scanner = new Scanner(System.in);
    System.out.println("请输入歌单保存地址：");
    rootPath = scanner.nextLine();
    System.out.println("请输入歌单ID：例如：2204388891");
    String id = scanner.nextLine();
    Map<String, String> map = main.getList(id);
    //Map<String, String> map = new HashMap<>();
    // map.put("516657051", "像风一样");
    System.out.println(" .... Total music " + map.size());
    System.out.println(" .... Start Download ");
    Map<String, String> failMap = main.down(map);
    System.out.println(" .... Fail music Total "+failMap.size());
    failMap.forEach((k,v)->{
      System.err.println(" .... Down Fail "+v);
    });
  }

  public String getBody(String strUrl) {
    HttpURLConnection conn = null;
    BufferedReader reader = null;
    String rs = null;
    try {
      StringBuffer sb = new StringBuffer();

      URL url = new URL(strUrl);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");

      conn.setRequestProperty("User-agent",
              "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36");
      conn.setUseCaches(false);
      conn.setConnectTimeout(DEF_CONN_TIMEOUT);
      conn.setReadTimeout(DEF_READ_TIMEOUT);
      conn.setInstanceFollowRedirects(false);
      conn.connect();
      InputStream is = conn.getInputStream();
      reader = new BufferedReader(new InputStreamReader(is, DEF_CHATSET));
      String strRead = null;
      while ((strRead = reader.readLine()) != null) {
        sb.append(strRead);
      }
      rs = sb.toString();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }

      }
      if (conn != null) {
        conn.disconnect();
      }
    }
    return rs;
  }

  /**
   * 下载
   * @param isAndName 需要下载的ID和name
   * @return 下载失败的列表
   */
  public Map<String, String> down(Map<String, String> isAndName) {
    Map<String, String> failMap = new HashMap<>();
    isAndName.forEach((k, v) -> {
      File file = new File(rootPath + "\\" + v + ".mp3");
      if (file.exists()) {
        return;
      }
      try {
        file.createNewFile();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      HttpURLConnection conn = null;

      HttpURLConnection conn2 = null;
      try {
        URL url = new URL("http://music.163.com/song/media/outer/url?id=" + k + ".mp3");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-agent",
                "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36");
        conn.setUseCaches(false);
        conn.setConnectTimeout(DEF_CONN_TIMEOUT);
        conn.setReadTimeout(DEF_READ_TIMEOUT);
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        if (302 == conn.getResponseCode()) {
          String httpurl = conn.getHeaderField("Location");
          conn.disconnect();

          url = new URL(httpurl);
          conn2 = (HttpURLConnection) url.openConnection();
          conn2.setRequestMethod("GET");
          conn2.setRequestProperty("User-agent",
                  "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36");
          conn2.setUseCaches(false);
          conn2.setConnectTimeout(DEF_CONN_TIMEOUT);
          conn2.setReadTimeout(DEF_READ_TIMEOUT);
          conn2.setInstanceFollowRedirects(false);
          conn2.connect();
          int read = 0;
          InputStream is = conn2.getInputStream();
          byte[] temp = new byte[1024 * 1024];
          while ((read = is.read(temp)) > 0) {
            byte[] bytes = new byte[read];
            System.arraycopy(temp, 0, bytes, 0, read);
            writer(file, bytes);
          }
          if(file.length()==0) {
            file.delete();
            System.err.println(" Down music " + v  + " fail!");
            failMap.put(k, v);
          }else {
            System.out.println(" Down music " + v  + " success!");
          }
        } else {
          file.delete();
          System.err.println(" Down music " + v  + " fail!");
          failMap.put(k, v);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (conn != null) {
          conn.disconnect();
        }
        if (conn2 != null) {
          conn2.disconnect();
        }
      }
    });
    return failMap;
  }

  /**
   * 写入数据
   * @param file  文件
   * @param bytes 需要写入的数据
   */
  private static void writer(File file, byte[] bytes) throws IOException {
    RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
    randomFile.seek(file.length());
    randomFile.write(bytes);
    randomFile.close();

  }
}
