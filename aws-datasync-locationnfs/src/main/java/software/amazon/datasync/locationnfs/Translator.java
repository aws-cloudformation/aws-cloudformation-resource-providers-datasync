package software.amazon.datasync.locationnfs;

import software.amazon.awssdk.services.datasync.model.CreateLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.UpdateLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.NfsMountOptions;
import software.amazon.awssdk.services.datasync.model.OnPremConfig;
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

    public static CreateLocationNfsRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationNfsRequest.builder()
                .mountOptions(translateToDataSyncMountOptions(model.getMountOptions()))
                .onPremConfig(translateToDataSyncOnPremConfig(model.getOnPremConfig()))
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
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

    public static UpdateLocationNfsRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateLocationNfsRequest.builder()
                .locationArn(model.getLocationArn())
                .mountOptions(translateToDataSyncMountOptions(model.getMountOptions()))
                .onPremConfig(translateToDataSyncOnPremConfig(model.getOnPremConfig()))
                .subdirectory(model.getSubdirectory())
                .build();
    }

    public static ListLocationsRequest translateToListRequest(final String nextToken) {
        return ListLocationsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    public static NfsMountOptions translateToDataSyncMountOptions(MountOptions mountOptions) {
        if (mountOptions == null)
            return null;
        return NfsMountOptions.builder()
                .version(mountOptions.getVersion())
                .build();
    }

    public static MountOptions translateToResourceModelMountOptions(NfsMountOptions mountOptions) {
        if (mountOptions == null)
            return null;
        return MountOptions.builder()
                .version(mountOptions.versionAsString())
                .build();
    }

    public static OnPremConfig translateToDataSyncOnPremConfig(
            software.amazon.datasync.locationnfs.OnPremConfig onPremConfig) {
        // Prevents Null Pointer Exception:
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

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }
}
