package software.amazon.datasync.locationfsxwindows;

import software.amazon.awssdk.services.datasync.model.CreateLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationFsxWindowsRequest translateToCreateRequest(final ResourceModel model) {
        return CreateLocationFsxWindowsRequest.builder()
                .domain(model.getDomain())
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .password(model.getPassword())
                .securityGroupArns(model.getSecurityGroupArns())
                .subdirectory(model.getSubdirectory())
                .tags(translateTags(model.getTags()))
                .user(model.getUser())
                .build();
    }

    public static DescribeLocationFsxWindowsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationFsxWindowsRequest.builder()
                .locationArn(locationArn)
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

    private static Set<TagListEntry> translateTags(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> TagListEntry.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }
}
