package software.amazon.datasync.locationnfs;

import software.amazon.awssdk.services.datasync.model.CreateLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.NfsMountOptions;
import software.amazon.awssdk.services.datasync.model.OnPremConfig;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationNfsRequest translateToCreateRequest(final ResourceModel model) {
        return CreateLocationNfsRequest.builder()
                .mountOptions(translateToDataSyncMountOptions(model.getMountOptions()))
                .onPremConfig(translateToDataSyncOnPremConfig(model.getOnPremConfig()))
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(translateTags(model.getTags()))
                .build();
    }

    public static DeleteLocationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLocationRequest.builder()
                .locationArn(model.getLocationArn())
                .build();
    }

    public static DescribeLocationNfsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationNfsRequest.builder()
                .locationArn(locationArn)
                .build();
    }

    public static ListLocationsRequest translateToListRequest(final String nextToken) {
        return ListLocationsRequest.builder()
                .maxResults(100)
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

    public static NfsMountOptions translateToDataSyncMountOptions(MountOptions mountOptions) {
        if (mountOptions == null)
            return NfsMountOptions.builder()
                    .version("AUTOMATIC")
                    .build();
        return NfsMountOptions.builder()
                .version(mountOptions.getVersion())
                .build();
    }

    public static MountOptions translateToResourceModelMountOptions(NfsMountOptions mountOptions) {
        if (mountOptions == null)
            return MountOptions.builder()
                    .version("AUTOMATIC")
                    .build();
        return MountOptions.builder()
                .version(mountOptions.versionAsString())
                .build();
    }

    public static OnPremConfig translateToDataSyncOnPremConfig(
            software.amazon.datasync.locationnfs.OnPremConfig onPremConfig) {
        if (onPremConfig == null)
            return OnPremConfig.builder().build();
        return OnPremConfig.builder()
                .agentArns(onPremConfig.getAgentArns())
                .build();
    }

    public static software.amazon.datasync.locationnfs.OnPremConfig translateToResourceModelOnPremConfig(
            OnPremConfig onPremConfig) {
        if (onPremConfig == null)
            return software.amazon.datasync.locationnfs.OnPremConfig.builder().build();
        return software.amazon.datasync.locationnfs.OnPremConfig.builder()
                .agentArns(onPremConfig.agentArns())
                .build();
    }
}
