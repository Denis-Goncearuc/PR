import javax.net.SocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpClient
{
    static Pattern pattern = Pattern.compile("\"\\S+\\.(jpg|png)\"");

    public static void main(String[] args) throws InterruptedException, IOException {
        List<String> imagesList = null;
        String hostname = "";
        Scanner keyboard = new Scanner(System.in);

        System.out.println("Alegeti site:\n[1] me.utm.md\n[2] UTM.md");
        int siteId = keyboard.nextInt();
        if (siteId == 1) {
            imagesList = useNonSslSocket();
            hostname = "http://me.utm.md";
        } else if (siteId == 2) {
            imagesList = useSslSocket();
        } else {
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (String name : imagesList) {
            pool.submit(new DownloadTask(hostname + name, siteId));
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        System.out.println("\nProcess finished!");
    }

    private static List<String> useNonSslSocket() {
        List<String> imagesList = new ArrayList<>();

        try {
            Socket socket = new Socket("me.utm.md", 80);

            PrintStream output = new PrintStream(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            output.println("GET / HTTP/1.1");
            output.println("Host: me.utm.md");
            output.println();

            String line = reader.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    var image = matcher.group(0).replace("\"", "");
                    if (!image.contains("http")) {
                        imagesList.add(image);
                    }
                }
                line = reader.readLine();
            }
            reader.close();
            output.close();
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
        return imagesList;
    }

    private static List<String> useSslSocket() throws IOException {
        List<String> imagesList = new ArrayList<>();

        final SocketFactory socketFactory = SSLSocketFactory.getDefault();
        try (final Socket socket = socketFactory.createSocket("utm.md", 443)) {
            final String request = "GET / HTTP/1.1\r\nConnection: close\r\nHost:utm.md\r\n\r\n";

            final OutputStream outputStream = socket.getOutputStream();
            outputStream.write(request.getBytes(StandardCharsets.US_ASCII));

            final InputStream inputStream = socket.getInputStream();

            final String response = readAsString(inputStream);
            Matcher matcher = pattern.matcher(response);
            while(matcher.find()) {
                imagesList.add(matcher.group(0).replace("\"", ""));
            }
            return imagesList;
        }
    }

    private static String readAsString(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}