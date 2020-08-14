package software.amazon.datasync.locationobjectstorage;

import software.amazon.awssdk.services.datasync.model.CreateLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    public static CreateLocationObjectStorageRequest translateToCreateRequest(ResourceModel model) {
        Integer serverPort = model.getServerPort() == null ? null : model.getServerPort().intValue();
        return CreateLocationObjectStorageRequest.builder()
                .accessKey(model.getAccessKey())
                .agentArns(model.getAgentArns())
                .bucketName(model.getBucketName())
                .secretKey(model.getSecretKey())
                .serverHostname(model.getServerHostname())
                .serverPort(serverPort)
                .serverProtocol(model.getServerProtocol())
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
                .nextToken(nextToken)
                .build();
    }

    public static DescribeLocationObjectStorageRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationObjectStorageRequest.builder()
                .locationArn(locationArn)
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
