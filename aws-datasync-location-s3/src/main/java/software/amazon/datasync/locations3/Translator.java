package software.amazon.datasync.locations3;

import software.amazon.awssdk.services.datasync.model.CreateLocationS3Request;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Request;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.S3Config;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationS3Request translateToCreateRequest(final ResourceModel model) {
        return CreateLocationS3Request.builder()
                .s3BucketArn(model.getS3BucketArn())
                .s3Config(translateToDataSyncS3Config(model.getS3Config()))
                .s3StorageClass(model.getS3StorageClass())
                .subdirectory(model.getSubdirectory())
                .tags(translateTags(model.getTags()))
                .build();
    }

    public static DeleteLocationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLocationRequest.builder()
                .locationArn(model.getLocationArn())
                .build();
    }

    public static ListLocationsRequest translateToListRequest(final String nextToken) {
        return ListLocationsRequest.builder()
                .maxResults(100)
                .nextToken(nextToken)
                .build();
    }

    public static DescribeLocationS3Request translateToReadRequest(final String locationArn) {
        return DescribeLocationS3Request.builder()
                .locationArn(locationArn)
                .build();
    }

    // Convert S3Config object to the correct type for DataSync API
    private static S3Config translateToDataSyncS3Config(software.amazon.datasync.locations3.S3Config s3Config) {
        return S3Config.builder()
                .bucketAccessRoleArn(s3Config.getBucketAccessRoleArn())
                .build();
    }

    // Convert Tag object to TagListEntry object
    public static Set<TagListEntry> translateTags(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> TagListEntry.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }
}
