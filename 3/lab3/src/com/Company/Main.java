package com.Company;

import com.Company.Models.Comment;
import com.Company.Models.Post;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    static ArrayList<Post> posts = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();

        boolean existConnection = CheckConnectivity();
        if (!existConnection) {
            System.out.println("Exista probleme cu conexiunea la server.");
            return;
        } else {
            System.out.println("Conexiunea a fost stabilita!\nMetodele suportate sunt: " + GetAvailableMethods());
        }

        try {
            while(true) {
                System.out.println("\n\nAi la dispozitie " + posts.size() + " postari.");
                if (posts.size() == 0) {
                    System.out.println("Doresti sa incarci postarile de pe website?(Da/Nu) ");
                    if (scanner.nextLine().equals("Da")) {
                        HTTPRequest getRequest = new HTTPRequest("https://jsonplaceholder.typicode.com/posts", RequestType.GET);
                        String response = sendRequest(getRequest);
                        posts = gson.fromJson(response, new TypeToken<ArrayList<Post>>(){}.getType());
                    } else {
                        break;
                    }
                } else {
                    System.out.println("1. Afiseaza lista\n2. Afiseaza detalii despre postare\n3. Afiseaza comentarii\n4. Adauga postare");
                    int method = scanner.nextInt();
                    scanner.nextLine();
                    int id;
                    switch (method) {
                        case 1:
                            posts.forEach(post -> {
                                System.out.println(post.id + ". " + post.title);
                            });
                            break;
                        case 2:
                            System.out.print("Introdu id la postare: ");
                            id = scanner.nextInt();
                            HTTPRequest getRequest = new HTTPRequest("https://jsonplaceholder.typicode.com/posts/" + id, RequestType.GET);
                            String response = sendRequest(getRequest);
                            Post post = gson.fromJson(response, Post.class);
                            if (post != null) {
                                System.out.println("---------------------");
                                System.out.println("\tId: " + post.id + "\n\tUser id: " + post.userId + "\n\tTitle: " + post.title + "\n\tBody: " + post.body);
                                System.out.println("---------------------");
                            }
                            break;
                        case 3:
                            System.out.print("Introdu id la postare: ");
                            id = scanner.nextInt();
                            getRequest = new HTTPRequest("https://jsonplaceholder.typicode.com/posts/" + id + "/comments", RequestType.GET);
                            response = sendRequest(getRequest);
                            Comment[] comments = gson.fromJson(response, Comment[].class);
                            if (comments.length > 0) {
                                int count = 1;
                                for(Comment comment : comments) {
                                    System.out.println(count++ + " comment ---------------");
                                    System.out.println("\tId: " + comment.id + "\n\tEmail: " + comment.email + "\n\tName: " + comment.name + "\n\tBody: " + comment.body + "\n");
                                }
                            }
                            break;
                        case 4:
                            System.out.print("Introduceti titlul: ");
                            String title = scanner.nextLine();
                            System.out.print("Introduceti continutul: ");
                            String body = scanner.nextLine();
                            HTTPRequest postRequest = new HTTPRequest("https://jsonplaceholder.typicode.com/posts", RequestType.POST)
                                    .addParameter("title", title)
                                    .addParameter("body", body)
                                    .addParameter("userId", "1");

                            response = sendRequest(postRequest);
                            post = gson.fromJson(response, Post.class);
                            if (post.title != null) {
                                posts.add(post);
                                System.out.println("Postarea a fost adaugata in lista cu success");
                            } else {
                                System.out.println("Ceva nu a mers cum trebuie");
                            }
                            break;
                        default:
                            return;
                    }
                }

                HTTPRequest authRequest = new HTTPRequest("http://httpbin.org/basic-auth", RequestType.GET)
                        .addParameter("user", "deniz")
                        .addParameter("passwd", "password");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sendRequest(HTTPRequest httpRequest) throws IOException {
        httpRequest.run();
        return httpRequest.getRequestBody();
    }

    private static boolean CheckConnectivity() {
        try {
            HTTPRequest headRequest = new HTTPRequest("https://jsonplaceholder.typicode.com", RequestType.HEAD);
            if (headRequest.run().contains("200 OK")) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String GetAvailableMethods() {
        try {
            HTTPRequest optionsRequest = new HTTPRequest("https://jsonplaceholder.typicode.com", RequestType.OPTIONS);
            String response = optionsRequest.run();

            String patternString = "Access-Control-Allow-Methods: (.+)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
