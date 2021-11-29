package software.amazon.datasync.locationsmb;

import software.amazon.awssdk.services.datasync.model.CreateLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.UpdateLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.SmbMountOptions;
import software.amazon.awssdk.services.datasync.model.TagListEntry;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateLocationSmbRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationSmbRequest.builder()
                .agentArns(model.getAgentArns())
                .domain(model.getDomain())
                .mountOptions(translateToDataSyncMountOptions(model.getMountOptions()))
                .password(model.getPassword())
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
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

    public static UpdateLocationSmbRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateLocationSmbRequest.builder()
                .locationArn(model.getLocationArn())
                .subdirectory(model.getSubdirectory())
                .user(model.getUser())
                .domain(model.getDomain())
                .password(model.getPassword())
                .mountOptions(translateToDataSyncMountOptions(model.getMountOptions()))
                .agentArns(model.getAgentArns())
                .build();
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

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }
}
