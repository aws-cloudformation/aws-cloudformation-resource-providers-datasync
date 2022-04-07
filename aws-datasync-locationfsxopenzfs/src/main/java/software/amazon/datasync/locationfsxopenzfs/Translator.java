package software.amazon.datasync.locationfsxopenzfs;

import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Map;

public class Translator {

    Translator() {}

    public static CreateLocationFsxOpenZfsRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return CreateLocationFsxOpenZfsRequest.builder()
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .securityGroupArns(model.getSecurityGroupArns())
                .protocol(translateToFsxProtocol(model.getProtocol()))
                .subdirectory(model.getSubdirectory())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .build();
    }

    public static DescribeLocationFsxOpenZfsRequest translateToReadRequest(final String locationArn) {
        return DescribeLocationFsxOpenZfsRequest.builder()
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

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }

    //Todo Need to build the FsxProtocol based on type NFS or SMB, when support for SMB is available
    public static FsxProtocol translateToFsxProtocol(Protocol protocol) {
        if(protocol == null)
            return null;
        return FsxProtocol.builder()
                .nfs(translateToFsxProtocolNfs(protocol.getNFS()))
                .build();
    }

    public static FsxProtocolNfs translateToFsxProtocolNfs(NFS nfs) {
        if(nfs == null)
            return null;
        return FsxProtocolNfs.builder()
                .mountOptions(translateToNfsMountOptions(nfs.getMountOptions()))
                .build();
    }

    public static NfsMountOptions translateToNfsMountOptions(MountOptions mountOptions) {
        if(mountOptions == null)
            return null;
        return NfsMountOptions.builder()
                .version(mountOptions.getVersion())
                .build();
    }

    //Todo Need to build the FsxProtocol based on type NFS or SMB, when support for SMB is available
    public static Protocol translateToResourceModelProtocol(FsxProtocol protocol) {
        if(protocol == null)
            return null;
        return Protocol.builder()
                .nFS(translateToResourceModelNFS(protocol.nfs()))
                .build();
    }

    public static NFS translateToResourceModelNFS(FsxProtocolNfs nfs) {
        if(nfs == null)
            return null;
        return NFS.builder()
                .mountOptions(translateToResourceModelMountOptions(nfs.mountOptions()))
                .build();
    }

    public static MountOptions translateToResourceModelMountOptions(NfsMountOptions mountOptions) {
        if(mountOptions == null)
            return null;
        return MountOptions.builder()
                .version(mountOptions.versionAsString())
                .build();
    }
}
