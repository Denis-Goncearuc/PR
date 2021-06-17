import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;

public class DownloadTask implements Runnable {
    private String path;
    boolean ssl = false;

    public DownloadTask(String path, int siteId) {
        this.path = path;
        ssl = siteId != 1;
    }

    @Override
    public void run() {
        DownloadImage(path, ssl);
    }

    static void DownloadImage(String link, boolean ssl)
    {
        Socket s = null;
        InputStream s_in = null;
        PrintWriter s_out = null;

        try {
            if (!ssl) {
                s = new Socket(getDomainName(link), 80);
            } else {
                final SocketFactory socketFactory = SSLSocketFactory.getDefault();
                s = socketFactory.createSocket(getDomainName(link), 443);
            }
            s_out = new PrintWriter(s.getOutputStream(), true);
            s_in = s.getInputStream();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Message to server
        String message = "GET " + link + " HTTP/1.0\r\n\r\n";
        s_out.println(message);

        try {
            var fullName = new File(link).getName();
            System.out.println("[Thread id: " + Thread.currentThread().getId() + "] " + fullName);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Header end flag.
            boolean headerEnded = false;

            byte[] bytes = new byte[2048];
            int length;
            while ((length = s_in.read(bytes)) != -1) {
                if (headerEnded)
                    out.write(bytes, 0, length);
                else {
                    for (int i = 0; i < 2043; i++) {
                        if (bytes[i] == 13 && bytes[i + 1] == 10 && bytes[i + 2] == 13 && bytes[i + 3] == 10) {
                            headerEnded = true;
                            out.write(bytes, i + 4 , 2048 - i - 4);
                            break;
                        }
                    }
                }
            }
            out.close();
            s_in.close();
            byte[] response = out.toByteArray();

            FileOutputStream fos = new FileOutputStream("C://Images//" + fullName);
            fos.write(response);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDomainName(String url) throws MalformedURLException{
        if(!url.startsWith("http") && !url.startsWith("https")){
            url = "http://" + url;
        }
        URL netUrl = new URL(url);
        String host = netUrl.getHost();
        if(host.startsWith("www")){
            host = host.substring("www".length()+1);
        }
        return host;
    }
}