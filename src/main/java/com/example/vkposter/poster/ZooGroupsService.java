package com.example.vkposter.poster;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Component
public class ZooGroupsService {
    public List<String> getGroupsToUse() {
        String filePath = "./config/groups_to_use.txt";

        try {
            List<String> content = Files.readAllLines(Paths.get(filePath));
            System.out.println("Read a list of groups to use from file " + filePath + ": " + content);

            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getPostsToSend() {
        String filePath = "./config/posts_to_send.txt";

        try {
            List<String> content = Files.readAllLines(Paths.get(filePath));
            System.out.println("Read a list of posts to send from file " + filePath + ": " + content);

            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void writeSkippedGroups(List<String> skippedGroups) {
        String filePath = "./config/skipped_groups_" + Instant.now().toString() + ".txt";

        try {
            Files.write(Paths.get(filePath), skippedGroups);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeGroupsLeft(List<String> groupsLeft) {
        String filePath = "./config/groups_to_use.txt";

        try {
            Files.write(Paths.get(filePath), groupsLeft);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
