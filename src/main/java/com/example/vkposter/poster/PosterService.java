package com.example.vkposter.poster;

import com.example.vkposter.auth.Authorizer;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
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

@Component
public class PosterService implements Runnable {
    @Autowired
    private ZooConfig zooConfig;

    @Autowired
    private Authorizer authorizer;

    private final VkApiClient vk = new VkApiClient(new HttpTransportClient());

    @Override
    public void run() {
        UserActor actor = authorizer.getUserActor(vk);

        List<String> postsToSend = zooConfig.getPostsToSend();

        for (String p : postsToSend) {
            PostData postData = preparePostData(actor, p);
            postToGroups(actor, postData, zooConfig.getGroupsToUse());
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

        String attachment = images.stream().map(i -> processPhoto(userActor, i)).collect(Collectors.joining(","));
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
        System.out.println("Starting sending post " + post.postName() + " to all groups...");

        List<String> skippedGroups = new ArrayList<>();

        for (String group : groups) {

            int retries = 0;
            boolean ok = false;

            while (!ok && retries < 3) {
                try {
                    doPost(userActor, post, Integer.parseInt(group));
                    ok = true;
                } catch (ApiException e) {
                    if (e.getCode() == 401 || e.getCode() == 403) {
                        userActor = authorizer.getUserActor(vk);
                    }
                    System.out.println("Error while sending post to group " + group + ": " + e.getMessage());

                    retries++;
                } catch (ClientException e) {
                    System.out.println("Error while sending post to group " + group + ": " + e.getMessage());

                    retries++;
                }
            }

            if (!ok) {
                System.out.println("Cannot send post to group " + group + "after " + retries + " retries, skipping.");
                skippedGroups.add(group);
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Finished sending post " + post.postName() + " to all groups.");

        if (!skippedGroups.isEmpty()) {
            System.out.println("Skipped groups: " + String.join(",", skippedGroups));
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
