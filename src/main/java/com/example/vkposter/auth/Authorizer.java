package com.example.vkposter.auth;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.UserAuthResponse;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class Authorizer {

    @Value("${poster.vk.appId}")
    private int appId;

    @Value("${poster.vk.clientSecret}")
    private String clientSecret;

    @Value("${poster.vk.redirectUrl}")
    private String redirectUrl;

    @Value("${poster.vk.permissions}")
    private String permissions;

    public String getVkAuthCode() throws IOException {
        String authUrl = "https://oauth.vk.com/authorize?client_id=" + appId + "&display=page&redirect_uri=" + redirectUrl
                + "&scope=" + permissions + "&response_type=code&v=5.131";

        System.setProperty("webdriver.chrome.driver", "./config/chromedriver/windows/chromedriver.exe");

        WebDriver driver = new ChromeDriver();

        driver.get(authUrl);

        System.out.println("Please authorize the application in your browser, then enter the authorization code here:");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        return reader.readLine();
    }

    public UserActor getUserActor(VkApiClient vk) {
        String code;
        try {
            code = getVkAuthCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        UserAuthResponse authResponse;
        try {
            authResponse = vk.oAuth()
                    .userAuthorizationCodeFlow(appId, clientSecret, redirectUrl, code)
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }

        return new UserActor(authResponse.getUserId(), authResponse.getAccessToken());
    }

}
