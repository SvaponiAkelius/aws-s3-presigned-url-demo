package com.akelius.svaponi.demo.utils;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static java.util.stream.Collectors.joining;

/**
 * SDK v2 does not implement signed url feature. This is a workaround!
 * See https://github.com/aws/aws-sdk-java-v2/issues/203
 */
public class S3PresignUrlRequest implements ToCopyableBuilder<S3PresignUrlRequest.Builder, S3PresignUrlRequest> {

    public static URI presign(final S3PresignUrlRequest request) {
        final String encodedBucket;
        final String encodedKey;
        try {
            encodedBucket = URLEncoder.encode(request.bucket(), "UTF-8");

            encodedKey = Arrays.stream(request.key().split("/"))
                    .map(s -> {
                        try {
                            return URLEncoder.encode(s, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(joining("/"));
        } catch (final UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }

        final String regionPart = request.region() == Region.US_EAST_1
                ? ""
                : ("-" + request.region().id());
        final SdkHttpFullRequest.Builder httpRequestBuilder =
                SdkHttpFullRequest.builder()
                        .method(request.httpMethod())
                        .protocol("https")
                        .host("s3" + regionPart + ".amazonaws.com")
                        .encodedPath(encodedBucket + "/" + encodedKey);

        if (request.responseContentDisposition() != null) {
            httpRequestBuilder.appendRawQueryParameter(
                    "response-content-disposition",
                    request.responseContentDisposition());
        }

        if (request.responseContentType() != null) {
            httpRequestBuilder.appendRawQueryParameter(
                    "response-content-type",
                    request.responseContentType());
        }

        final SdkHttpFullRequest httpRequest = httpRequestBuilder.build();

        // IMPORTANT: expiration time is relative (do not use Instant.now().plus(...))
        final Instant expirationTime = request.signatureDuration() == null
                ? null
                : Instant.now().plus(request.signatureDuration());
//                : Instant.ofEpochSecond(request.signatureDuration().getSeconds());

        final Aws4PresignerParams presignRequest =
                Aws4PresignerParams.builder()
                        .expirationTime(expirationTime)
                        .awsCredentials(request
                                .credentialsProvider()
                                .resolveCredentials())
                        .signingName(software.amazon.awssdk.services.s3.S3Client.SERVICE_NAME)
                        .signingRegion(request.region())
                        .build();

        return AwsS3V4Signer.create()
                .presign(httpRequest, presignRequest)
                .getUri();
    }

    private final AwsCredentialsProvider credentialsProvider;
    private final SdkHttpMethod httpMethod;
    private final Region region;
    private final String bucket;
    private final String key;
    private final Duration signatureDuration;
    private final String responseContentType;
    private final String responseContentDisposition;

    private S3PresignUrlRequest(final Builder builder) {
        this.credentialsProvider = Validate.notNull(builder.credentialsProvider, "credentialsProvider");
        this.httpMethod = Validate.notNull(builder.httpMethod, "httpMethod");
        this.region = Validate.notNull(builder.region, "region");
        this.bucket = Validate.notNull(builder.bucket, "bucket");
        this.key = Validate.notNull(builder.key, "key");
        this.signatureDuration = builder.signatureDuration;
        this.responseContentType = builder.responseContentType;
        this.responseContentDisposition = builder.responseContentDisposition;
    }

    public static Builder builder() {
        return new Builder();
    }

    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    public SdkHttpMethod httpMethod() {
        return httpMethod;
    }

    public Region region() {
        return region;
    }

    public String bucket() {
        return bucket;
    }

    public String key() {
        return key;
    }

    public Duration signatureDuration() {
        return signatureDuration;
    }

    public String responseContentType() {
        return responseContentType;
    }

    public String responseContentDisposition() {
        return responseContentDisposition;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .bucket(bucket)
                .key(key)
                .signatureDuration(signatureDuration);
    }

    public static class Builder implements CopyableBuilder<Builder, S3PresignUrlRequest> {
        private AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        private SdkHttpMethod httpMethod = SdkHttpMethod.GET;
        private Region region;
        private String bucket;
        private String key;
        private Duration signatureDuration;
        private String responseContentType;
        private String responseContentDisposition;

        public Builder credentialsProvider(final AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder httpMethod(final SdkHttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder region(final Region region) {
            this.region = region;
            return this;
        }

        public Builder bucket(final String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder key(final String key) {
            this.key = key;
            return this;
        }

        /**
         * If null, then internal SDK default is applied.
         *
         * @see software.amazon.awssdk.auth.signer.params.Aws4PresignerParams.Builder#expirationTime(java.time.Instant)
         * @see software.amazon.awssdk.auth.signer.internal.SignerConstant#PRESIGN_URL_MAX_EXPIRATION_SECONDS
         */
        public Builder signatureDuration(final Duration signatureDuration) {
            this.signatureDuration = signatureDuration;
            return this;
        }

        public Builder responseContentType(final String responseContentType) {
            this.responseContentType = responseContentType;
            return this;
        }

        public Builder responseContentDisposition(final String responseContentDisposition) {
            this.responseContentDisposition = responseContentDisposition;
            return this;
        }

        @Override
        public S3PresignUrlRequest build() {
            return new S3PresignUrlRequest(this);
        }
    }

    public URI presign() {
        return S3PresignUrlRequest.presign(this);
    }
}
