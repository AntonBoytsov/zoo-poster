package com.example.vkposter.poster;

import com.example.vkposter.auth.Authorizer;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.*;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.wall.responses.PostResponse;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PosterService implements Runnable {
    @Autowired
    private ZooGroupsService zooGroupsService;

    @Autowired
    private Authorizer authorizer;

    private final VkApiClient vk = new VkApiClient(new HttpTransportClient());

    @Override
    public void run() {
        UserActor actor = authorizer.getUserActor(vk);

        List<String> postsToSend = zooGroupsService.getPostsToSend();

        for (String p : postsToSend) {
            PostData postData = preparePostData(actor, p);
            postToGroups(actor, postData, zooGroupsService.getGroupsToUse());
        }
    }

    private PostData preparePostData(UserActor userActor, String postName) {
        PostDataProcessor reader = new PostDataProcessor("./config/posts/" + postName + "/");

        String message = reader.getMessage();

        List<String> images = reader.getImageFiles();

        if (Strings.isEmpty(message)) {
            throw new RuntimeException("Post " + postName + " message should not be empty");
        }

        if (images.isEmpty()) {
            throw new RuntimeException("Post " + postName + " images should not be empty");
        }

        Stream<String> imageStream = images.stream().map(i -> processPhoto(userActor, i));
        Stream<String> videoStream = reader.getVideoFiles().stream();

        String attachment = Stream.concat(imageStream, videoStream).collect(Collectors.joining(","));

        return new PostData(postName, message, attachment);
    }

    private String processPhoto(UserActor userActor, String photoPath) {
        File photoFile = new File(photoPath);

        try {
            var serverResponse = vk.photos().getWallUploadServer(userActor).execute();
            var uploadResponse = vk.upload().photoWall(serverResponse.getUploadUrl().toString(), photoFile).execute();
            var photoList = vk.photos().saveWallPhoto(userActor, uploadResponse.getPhoto())
                    .server(uploadResponse.getServer())
                    .hash(uploadResponse.getHash())
                    .execute();

            var photo = photoList.get(0);

            return "photo" + photo.getOwnerId() + "_" + photo.getId();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException("Error while preparing photos", e);
        }
    }

    private void postToGroups(UserActor userActor, PostData post, List<String> groups) {
        final int MAX_RETRIES = 3;

        System.out.println("Starting sending post " + post.postName() + " to all groups...");

        List<String> skippedGroups = new ArrayList<>();
        int currentIndex = -1;

        try {
            for (String group : groups) {
                currentIndex++;

                int retries = 0;
                boolean ok = false;

                while (!ok && retries < MAX_RETRIES) {
                    try {
                        doPost(userActor, post, Integer.parseInt(group));
                        ok = true;
                    } catch (ApiAuthException | ApiAccessException | ApiPermissionException | ApiCaptchaException e) {
                        System.out.println("Access denied error while sending post to group " + group + ": " + e.getDescription());
                        userActor = authorizer.getUserActor(vk);

                        retries++;
                    } catch (ApiWallTooManyRecipientsException e) {
                        System.out.println("Too many requests error while sending post to group " + group + ": " + e.getDescription());
                        System.out.println("Sleeping for 10 minutes...");

                        try {
                            TimeUnit.MINUTES.sleep(10);
                        } catch (InterruptedException ie) {
                            throw new RuntimeException(ie);
                        }

                        retries++;

                        if (retries == MAX_RETRIES) {
                            throw new RuntimeException("Too many recipients error repeating several times, stopping the processing.");
                        }
                    } catch (ApiException e) {
                        System.out.println("API exception while sending post to group " + group + ": " + e.getDescription());

                        retries++;
                    } catch (ClientException e) {
                        System.out.println("Client exception while sending post to group " + group + ": " + e.getLocalizedMessage());

                        retries++;
                    } catch (NumberFormatException e) {
                        System.out.println("Incorrect group number: " + group);

                        break;
                    }

                    try {
                        TimeUnit.SECONDS.sleep(20);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (!ok) {
                    System.out.println("Cannot send post to group " + group + ", skipping.");
                    skippedGroups.add(group);
                }
            }

            currentIndex++;

            System.out.println("Finished sending post " + post.postName() + " to all groups.");

            if (!skippedGroups.isEmpty()) {
                System.out.println("Skipped groups: " + String.join(",", skippedGroups));
            }
        } finally {
            if (!skippedGroups.isEmpty()) {
                zooGroupsService.writeSkippedGroups(skippedGroups);
            }

            if (currentIndex < groups.size()) {
                List<String> groupsLeft = groups.subList(currentIndex, groups.size());
                zooGroupsService.writeGroupsLeft(groupsLeft);
            }
        }
    }

    private void doPost(UserActor userActor, PostData post, int groupId) throws ApiException, ClientException {
        PostResponse postResponse = vk.wall().post(userActor)
                .ownerId(groupId)
                .message(post.message())
                .attachments(post.attachments())
                .execute();

        System.out.println("OK: " + groupId + ": " + postResponse.toPrettyString());
    }
}
