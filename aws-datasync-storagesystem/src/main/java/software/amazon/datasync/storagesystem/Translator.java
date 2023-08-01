package software.amazon.datasync.storagesystem;

import java.util.Map;

import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

public class Translator {

    Translator() {}

    public static AddStorageSystemRequest translateToCreateRequest(final ResourceModel model, Map<String, String> tags) {
        return AddStorageSystemRequest.builder()
                .serverConfiguration(translateToDiscoveryServerConfiguration(model.getServerConfiguration()))
                .credentials(translateToCredentials(model.getServerCredentials()))
                .systemType(model.getSystemType())
                .agentArns(model.getAgentArns())
                .cloudWatchLogGroupArn(model.getCloudWatchLogGroupArn())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .name(model.getName())
                .build();
    }

    public static DescribeStorageSystemRequest translateToReadRequest(final String storageSystemArn) {
        return DescribeStorageSystemRequest.builder()
                .storageSystemArn(storageSystemArn)
                .build();
    }

    public static ListStorageSystemsRequest translateToListRequest(final String nextToken) {
        return ListStorageSystemsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    public static RemoveStorageSystemRequest translateToDeleteRequest(final String storageSystemArn) {
        return RemoveStorageSystemRequest.builder()
                .storageSystemArn(storageSystemArn)
                .build();
    }

    public static UpdateStorageSystemRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateStorageSystemRequest.builder()
                .serverConfiguration(translateToDiscoveryServerConfiguration(model.getServerConfiguration()))
                .credentials(model.getServerCredentials() != null ?
                        translateToCredentials(model.getServerCredentials()) : null)
                .storageSystemArn(model.getStorageSystemArn())
                .agentArns(model.getAgentArns())
                .cloudWatchLogGroupArn(model.getCloudWatchLogGroupArn())
                .name(model.getName())
                .build();
    }

    private static DiscoveryServerConfiguration translateToDiscoveryServerConfiguration(final ServerConfiguration serverConfiguration) {
        return DiscoveryServerConfiguration.builder()
                .serverHostname(serverConfiguration.getServerHostname())
                .serverPort(serverConfiguration.getServerPort())
                .build();
    }

    private static Credentials translateToCredentials(final ServerCredentials credentials) {
        return Credentials.builder()
                .username(credentials.getUsername())
                .password(credentials.getPassword())
                .build();
    }

    public static ServerConfiguration translateToResourceModelServerConfiguration(final DiscoveryServerConfiguration discoveryServerConfiguration) {
        return ServerConfiguration.builder()
                .serverHostname(discoveryServerConfiguration.serverHostname())
                .serverPort(discoveryServerConfiguration.serverPort())
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