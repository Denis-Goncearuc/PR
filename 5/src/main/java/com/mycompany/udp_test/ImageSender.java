package com.mycompany.udp_test;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.sun.image.codec.jpeg.ImageFormatException;

public class ImageSender {

    /* Flags and sizes */
    public static int HEADER_SIZE = 8;
    public static int MAX_PACKETS = 255;
    public static int SESSION_START = 128;
    public static int SESSION_END = 64;
    public static int DATAGRAM_MAX_SIZE = 65507 - HEADER_SIZE;
    public static int MAX_SESSION_NUMBER = 255;

    /*
    * The absolute maximum datagram packet size is 65507, The maximum IP packet
    * size of 65535 minus 20 bytes for the IP header and 8 bytes for the UDP
    * header.
     */
    public static String OUTPUT_FORMAT = "jpg";

    public static int COLOUR_OUTPUT = BufferedImage.TYPE_INT_RGB;

    /* Default parameters */
    public static double SCALING = 0.5;
    public static int SLEEP_MILLIS = 250;
    public static String IP_ADDRESS = "225.4.5.6";
    public static int PORT = 4444;
    public static boolean SHOW_MOUSEPOINTER = true;

    public static BufferedImage getScreenshot() throws AWTException,
            ImageFormatException, IOException {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        Rectangle screenRect = new Rectangle(screenSize);

        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(screenRect);

        return image;
    }

    /**
     * Converts BufferedImage to byte array
     */
    public static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    /**
     * Scales a bufferd image
     */
    public static BufferedImage scale(BufferedImage source, int w, int h) {
        Image image = source
                .getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
        BufferedImage result = new BufferedImage(w, h, COLOUR_OUTPUT);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * Shrinks a BufferedImage
     */
    public static BufferedImage shrink(BufferedImage source, double factor) {
        int w = (int) (source.getWidth() * factor);
        int h = (int) (source.getHeight() * factor);
        return scale(source, w, h);
    }

    /**
     * Sends a byte array via multicast Multicast addresses are IP addresses in
     * the range of 224.0.0.0 to 239.255.255.255.
     *
     * @param imageData Byte array
     * @param multicastAddress IP multicast address
     * @param port Port
     * @return <code>true</code> on success otherwise <code>false</code>
     */
    private boolean sendImage(byte[] imageData, String multicastAddress,
            int port) {
        InetAddress ia;

        boolean ret = false;
        int ttl = 2;

        try {
            ia = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return ret;
        }

        MulticastSocket ms = null;

        try {
            ms = new MulticastSocket();
            ms.setTimeToLive(ttl);
            DatagramPacket dp = new DatagramPacket(imageData, imageData.length,
                    ia, port);
            ms.send(dp);
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (ms != null) {
                ms.close();
            }
        }

        return ret;
    }

    public static void main(String[] args) {
        ImageSender sender = new ImageSender();
        int sessionNumber = 0;

        // Create Frame
        JFrame frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel();
        label.setText("Sharing screen...");
        
        frame.getContentPane().add(label);
        frame.setVisible(true);


        frame.pack();

        try {
            /* Continuously send images */
            while (true) {
                BufferedImage image;

                /* Get screenshot */
                image = getScreenshot();

                /* Draw mousepointer into image */
                if (SHOW_MOUSEPOINTER) {
                    PointerInfo p = MouseInfo.getPointerInfo();
                    int mouseX = p.getLocation().x;
                    int mouseY = p.getLocation().y;

                    Graphics2D g2d = image.createGraphics();
                    g2d.setColor(Color.red);
                    Polygon polygon1 = new Polygon(new int[]{mouseX, mouseX + 10, mouseX, mouseX},
                            new int[]{mouseY, mouseY + 10, mouseY + 15, mouseY},
                             4);

                    Polygon polygon2 = new Polygon(new int[]{mouseX + 1, mouseX + 10 + 1, mouseX + 1, mouseX + 1},
                            new int[]{mouseY + 1, mouseY + 10 + 1, mouseY + 15 + 1, mouseY + 1},
                             4);
                    g2d.setColor(Color.black);
                    g2d.fill(polygon1);

                    g2d.setColor(Color.red);
                    g2d.fill(polygon2);
                    g2d.dispose();
                }
                
                /* Scale image */
                image = shrink(image, SCALING);
                byte[] imageByteArray = bufferedImageToByteArray(image, OUTPUT_FORMAT);
                int packets = (int) Math.ceil(imageByteArray.length / (float) DATAGRAM_MAX_SIZE);

                /* If image has more than MAX_PACKETS slices -> error */
                if (packets > MAX_PACKETS) {
                    System.out.println("Image is too large to be transmitted!");
                    continue;
                }

                /* Loop through slices */
                for (int i = 0; i <= packets; i++) {
                    int flags = 0;
                    flags = i == 0 ? flags | SESSION_START : flags;
                    flags = (i + 1) * DATAGRAM_MAX_SIZE > imageByteArray.length ? flags | SESSION_END : flags;

                    int size = (flags & SESSION_END) != SESSION_END ? DATAGRAM_MAX_SIZE : imageByteArray.length - i * DATAGRAM_MAX_SIZE;

                    /* Set additional header */
                    byte[] data = new byte[HEADER_SIZE + size];
                    data[0] = (byte) flags;
                    data[1] = (byte) sessionNumber;
                    data[2] = (byte) packets;
                    data[3] = (byte) (DATAGRAM_MAX_SIZE >> 8);
                    data[4] = (byte) DATAGRAM_MAX_SIZE;
                    data[5] = (byte) i;
                    data[6] = (byte) (size >> 8);
                    data[7] = (byte) size;

                    /* Copy current slice to byte array */
                    System.arraycopy(imageByteArray, i * DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size);
                    /* Send multicast packet */
                    sender.sendImage(data, IP_ADDRESS, PORT);

                    /* Leave loop if last slice has been sent */
                    if ((flags & SESSION_END) == SESSION_END) {
                        break;
                    }
                }
                /* Sleep */
                Thread.sleep(SLEEP_MILLIS);

                /* Increase session number */
                sessionNumber = sessionNumber < MAX_SESSION_NUMBER ? ++sessionNumber : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
