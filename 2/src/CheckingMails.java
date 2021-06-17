/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.mail.BodyPart;
  
public class CheckingMails 
{ 
    public static  ArrayList<SimpleMessage> startEmails()
    {
        ArrayList<SimpleMessage> list = new ArrayList<SimpleMessage>();
        String host = "imap.gmail.com";
        String username = "alexandrinasobol11@gmail.com";
        String password = "Alexandrina12345";

        int count = 5;
        try {
            //create properties field
            Properties properties = new Properties();

            properties.setProperty("mail.imap.ssl.trust", "*");
            properties.setProperty("mail.imap.ssl.enable", "true");
            Session emailSession = Session.getInstance(properties);

            Store store = emailSession.getStore("imap");

            store.connect(host, username, password);

            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();
            int totalCount = messages.length;
            for (int i = messages.length - 1; i > totalCount - count - 1; i--) {
                SimpleMessage simpleMessage = new SimpleMessage();
                simpleMessage.subject = messages[i].getSubject().toString();
                simpleMessage.from = messages[i].getFrom()[0].toString();
                simpleMessage.dateSent = messages[i].getSentDate().toString();
                simpleMessage.content = getTextFromMessage(messages[i]);
                list.add(simpleMessage);
            }
            //close the store and folder objects
            emailFolder.close(false);
            store.close();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }
    
  /*  public static void startEmails() throws Exception {

          String host = "imap.gmail.com";
          String username = "alexandrinasobol11@gmail.com";
          String password = "Alexandrina12345";
          int count = 5;
          try {
            //create properties field
            Properties properties = new Properties();


            properties.setProperty("mail.imap.ssl.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            Store store = emailSession.getStore("imap");

            store.connect(host, username, password);

            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();
            System.out.println("messages.length---" + messages.length);

            for (int i = messages.length - 1, n = messages.length - count - 1; i > n; i--) {
               Message message = messages[i];
               System.out.println("---------------------------------");
               System.out.println("Email Number " + (messages.length + i));
               System.out.println("Subject: " + message.getSubject());
               System.out.println("From: " + message.getFrom()[0]);
               System.out.println("Date Sent: " + message.getSentDate());
               getContent(message);
            }

            //close the store and folder objects
            emailFolder.close(false);
            store.close();

          } catch (NoSuchProviderException e) {
             e.printStackTrace();
          } catch (MessagingException e) {
             e.printStackTrace();
          } catch (Exception e) {
             e.printStackTrace();
          }
  }*/
}
