package com.example.vkposter.poster;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Component
public class ZooConfig {
    public List<String> getGroupsToUse() {
        String filePath = "./config/groups_to_use.txt";  // replace with the path to your text file

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("Read a list of groups to use from file " + filePath + ": " + content);

            return Arrays.asList(content.split(","));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getPostsToSend() {
        String filePath = "./config/posts_to_send.txt";  // replace with the path to your text file

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("Read a list of posts to send from file " + filePath + ": " + content);

            return Arrays.asList(content.split(","));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
