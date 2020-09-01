package com.akelius.svaponi.demo;

import com.akelius.svaponi.demo.utils.Rfc5987;
import com.akelius.svaponi.demo.utils.S3PresignUrlRequest;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class S3UploadAndDownloadWithSignedUrls {

    public static void main(final String[] args) throws IOException {

        // you need to be authorized to read/write in such bucket (see https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)
        final String bucket = System.getProperty("bucket", System.getenv("BUCKET"));

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Missing bucket");
        }

        final File file = new File("src/main/resources/test.jpg");

        if (!file.exists()) {
            throw new IllegalStateException("File not found");
        }

        final String key = "tmp/" + file.getName();

        System.out.println("Target file: " + bucket + "/" + key);

        final boolean exists = fileExists(bucket, key);

        final URI deleteSignedUri = S3PresignUrlRequest.builder()
                .httpMethod(SdkHttpMethod.DELETE)
                .region(DependencyFactory.getInstance().getRegion())
                .bucket(bucket)
                .key(key)
                .credentialsProvider(DependencyFactory.getInstance().getCredentialsProvider())
                .signatureDuration(Duration.ofMinutes(10))
                .build().presign();

        if (exists) {

            System.out.println("File already exists");

            System.out.println("Delete url: " + deleteSignedUri);

            System.out.println("Deleting...");

            final HttpDelete deleteRequest = new HttpDelete(deleteSignedUri);
            final int deleteStatusResponse = executeHttpRequest(deleteRequest);
            if (deleteStatusResponse != 204) {
                throw new IllegalStateException("Delete failed");
            }

        }

        final URI uploadSignedUri = S3PresignUrlRequest.builder()
                .httpMethod(SdkHttpMethod.PUT)
                .region(DependencyFactory.getInstance().getRegion())
                .bucket(bucket)
                .key(key)
                .credentialsProvider(DependencyFactory.getInstance().getCredentialsProvider())
                .signatureDuration(Duration.ofMinutes(10))
                .build().presign();

        System.out.println("Upload url: " + uploadSignedUri);

        System.out.println("Uploading...");

        final HttpPut uploadRequest = new HttpPut(uploadSignedUri);
        uploadRequest.setEntity(new FileEntity(file));
        uploadRequest.setHeader("Content-Type", "image/jpeg");
        // Content-Disposition notes:
        // - inline works only if content-type is "previewable"
        // - filename needs to be encoded, see https://tools.ietf.org/html/rfc5987
        uploadRequest.setHeader("Content-Disposition", "inline;filename*=UTF-8''" + Rfc5987.encodeUTF8(file.getName()));
        final int uploadStatusResponse = executeHttpRequest(uploadRequest);
        if (uploadStatusResponse != 200) {
            throw new IllegalStateException("Upload failed");
        }

        final URI downloadSignedUri = S3PresignUrlRequest.builder()
                .httpMethod(SdkHttpMethod.GET)
                .region(DependencyFactory.getInstance().getRegion())
                .bucket(bucket)
                .key(key)
                .credentialsProvider(DependencyFactory.getInstance().getCredentialsProvider())
                .responseContentDisposition("inline;filename*=UTF-8''" + Rfc5987.encodeUTF8("My Fåncy Nam∆.jpg"))
                .signatureDuration(Duration.ofMinutes(10))
                .build().presign();

        System.out.println("Download url: " + downloadSignedUri.toString());


        System.out.println();
        System.out.println("Try manually...");

        System.out.println("curl -f '" + downloadSignedUri + "' -o " + FilenameUtils.getBaseName(file.getName()) + ".copy.jpg");
        System.out.println("curl -f -XDELETE '" + deleteSignedUri + "'");
        System.out.println("curl -f -XPUT '" + uploadSignedUri + "' -H 'Content-Type: image/jpeg' --data-binary @" + file.getAbsolutePath());

    }

    private static int executeHttpRequest(final HttpUriRequest request) throws IOException {
        System.out.println("Request: " + request.getRequestLine());
        try (final CloseableHttpResponse response = DependencyFactory.getInstance().getHttpClient().execute(request)) {
            System.out.println("Response: " + response.getStatusLine());
            if (response.getEntity() != null && response.getEntity().getContentLength() > 0) {
                System.out.println();
                System.out.println(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                System.out.println();
            }
            return response.getStatusLine().getStatusCode();
        }
    }

    private static boolean fileExists(final String bucket, final String key) {
        try {
            DependencyFactory.getInstance().getS3Client().headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (final NoSuchKeyException e) {
            return false;
        }
    }
}
