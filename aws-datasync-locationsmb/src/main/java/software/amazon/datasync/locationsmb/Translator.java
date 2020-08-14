package software.amazon.datasync.locationsmb;

import software.amazon.awssdk.services.datasync.model.CreateLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.SmbMountOptions;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationSmbRequest translateToCreateRequest(final ResourceModel model) {
        return CreateLocationSmbRequest.builder()
                .agentArns(model.getAgentArns())
                .domain(model.getDomain())
                .mountOptions(translateToDataSyncMountOptions(model.getMountOptions()))
                .password(model.getPassword())
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(translateTags(model.getTags()))
                .user(model.getUser())
                .build();
    }

    public static DescribeLocationSmbRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationSmbRequest.builder()
                .locationArn(locationArn)
                .build();
    }

    public static ListLocationsRequest translateToListRequest(final String nextToken) {
        return ListLocationsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    public static DeleteLocationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLocationRequest.builder()
                .locationArn(model.getLocationArn())
                .build();
    }

    private static Set<TagListEntry> translateTags(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> TagListEntry.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    private static SmbMountOptions translateToDataSyncMountOptions(MountOptions mountOptions) {
        if (mountOptions == null)
            return null;
        return SmbMountOptions.builder()
                .version(mountOptions.getVersion())
                .build();

    }

    public static MountOptions translateToResourceModelMountOptions(SmbMountOptions mountOptions) {
        if (mountOptions == null)
            return null;
        return MountOptions.builder()
                .version(mountOptions.versionAsString())
                .build();
    }
}
