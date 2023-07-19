package software.amazon.datasync.storagesystem;

import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemResponse;
import software.amazon.awssdk.services.datasync.model.DiscoveryServerConfiguration;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();
        final DescribeStorageSystemResponse describeStorageSystemResponse = buildDefaultResponse();
        final ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();
        final ResourceModel resourceModel = buildDefaultModel();

        doReturn(describeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        doReturn(listTagsForResourceResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(ListTagsForResourceRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isNotNull();
    }

    @Test
    public void handleRequest_FailureNotFoundRequest() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel resourceModel = buildDefaultModel();

        doThrow(InvalidRequestException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel resourceModel = buildDefaultModel();

        doThrow(InternalException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel resourceModel = buildDefaultModel();

        doThrow(DataSyncException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    private static DescribeStorageSystemResponse buildDefaultResponse() {
        final String storageSystemArn = "arn:aws:datasync:us-east-1:012345678901:system/storage-system-01234567-0123-b123-a123-da1234567890";
        final String secretManagerArn = "arn:aws:secretsmanager:us-east-1:012345678901:secret:testSecret-xyz";
        final String storageSystemName = "TestStorageSystem";
        final String cloudwatchLogGroupArn = "arn:aws:logs:us-east-1:012345678901:log-group:/aws/datasync/discovery:*";
        final String agentArn = "arn:aws:datasync:us-east-1:012345678901:agent/agent-0af44b1e18d579b82";
        final String systemType = "NetAppONTAP";
        final String connectivityStatus = "PASS";
        final DiscoveryServerConfiguration discoveryServerConfiguration =
                DiscoveryServerConfiguration.builder()
                        .serverHostname("012.01.01.01")
                        .serverPort(443)
                        .build();


        return DescribeStorageSystemResponse.builder()
                .storageSystemArn(storageSystemArn)
                .serverConfiguration(discoveryServerConfiguration)
                .secretsManagerArn(secretManagerArn)
                .systemType(systemType)
                .agentArns(Arrays.asList(agentArn))
                .name(storageSystemName)
                .connectivityStatus(connectivityStatus)
                .cloudWatchLogGroupArn(cloudwatchLogGroupArn)
                .build();
    }

    private static ResourceModel buildDefaultModel() {
        final String storageSystemArn = "arn:aws:datasync:us-east-1:012345678901:system/storage-system-01234567-0123-b123-a123-da1234567890";
        return ResourceModel.builder()
                .storageSystemArn(storageSystemArn)
                .build();
    }
}