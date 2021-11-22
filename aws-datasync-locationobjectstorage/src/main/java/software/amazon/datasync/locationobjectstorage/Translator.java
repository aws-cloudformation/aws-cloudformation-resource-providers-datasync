package software.amazon.datasync.locationobjectstorage;

import software.amazon.awssdk.services.datasync.model.CreateLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteLocationRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.UpdateLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    public static CreateLocationObjectStorageRequest translateToCreateRequest(ResourceModel model, Map<String, String> tags) {
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
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .build();
    }

    public static DeleteLocationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteLocationRequest.builder()
                .locationArn(model.getLocationArn())
                .build();
    }

    public static UpdateLocationObjectStorageRequest translateToUpdateRequest(final ResourceModel model) {
        Integer serverPort = model.getServerPort() == null ? null : model.getServerPort().intValue();
        return UpdateLocationObjectStorageRequest.builder()
                .accessKey(model.getAccessKey())
                .agentArns(model.getAgentArns())
                .locationArn(model.getLocationArn())
                .secretKey(model.getSecretKey())
                .serverPort(serverPort)
                .serverProtocol(model.getServerProtocol())
                .subdirectory(model.getSubdirectory())
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

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }
}
