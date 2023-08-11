package com.example.vkposter.poster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostDataProcessor {

    private final String postDir;

    public PostDataProcessor(String postDir) {
        this.postDir = postDir;
    }

    public String getMessage() {
        String filePath = postDir + "/message.txt";
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            throw new RuntimeException("Error while reading " + filePath + ": " + e.getMessage(), e);
        }
    }

    public List<String> getImageFiles() {
        try {
            try (Stream<Path> paths = Files.walk(Paths.get(postDir))) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(Path::toString)
                        .filter(f -> f.endsWith(".jpg") || f.endsWith(".png"))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while scanning " + postDir + " for images: " + e.getMessage(), e);
        }
    }

    public List<String> getVideoFiles() {
        String filePath = postDir + "/videos.txt";
        try {
            return Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Error while scanning " + postDir + " for videos: " + e.getMessage(), e);
        }
    }

}
