# aws-s3-presigned-url-demo

This project contains a demo to upload and download a file using S3 presigned urls (generated with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2)).


## Prerequisites
- Java 11
- Apache Maven (optional)


## How to

If you have Maven installed
```
mvn test -Dbucket=YOUR_BUCKET
```

If you **do not have** Maven installed
```
./mvnw test -Dbucket=YOUR_BUCKET
```

#### Notes
- You need to be authorized to read/write in `YOUR_BUCKET` (see [docs](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)).
- File will be uploaded to `s3://YOUR_BUCKET/tmp/test.jpg`
