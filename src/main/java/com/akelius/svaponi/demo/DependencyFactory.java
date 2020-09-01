package com.akelius.svaponi.demo;

import lombok.Data;
import lombok.Getter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Data
public class DependencyFactory {

    @Getter
    private static final DependencyFactory instance = new DependencyFactory();

    private final AwsCredentialsProvider credentialsProvider;
    private final S3Client s3Client;
    private final CloseableHttpClient httpClient;
    private final Region region = Region.EU_WEST_1;

    private DependencyFactory() {
        credentialsProvider = DefaultCredentialsProvider.create();
        s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.EU_WEST_1)
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
        httpClient = HttpClients.createDefault();
    }
}
