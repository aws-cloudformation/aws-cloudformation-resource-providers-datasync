package software.amazon.datasync.storagesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import software.amazon.awssdk.services.datasync.model.UpdateStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemResponse;
import software.amazon.awssdk.services.datasync.model.DiscoveryServerConfiguration;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.datasync.model.TagResourceRequest;
import software.amazon.awssdk.services.datasync.model.UpdateStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.UpdateStorageSystemResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    private static final String storageSystemArn = "arn:aws:datasync:us-east-1:012345678901:system/storage-system-01234567-0123-b123-a123-da1234567890";
    private static final String secretManagerArn = "arn:aws:secretsmanager:us-east-1:012345678901:secret:testSecret-xyz";
    private static final String storageSystemName = "TestStorageSystem";
    private static final String cloudwatchLogGroupArn = "arn:aws:logs:us-east-1:012345678901:log-group:/aws/datasync/discovery:*";
    private static final String agentArn = "arn:aws:datasync:us-east-1:012345678901:agent/agent-0af44b1e18d579b82";
    private static final String systemType = "NetAppONTAP";
    private static final String connectivityStatus = "PASS";
    private static final DiscoveryServerConfiguration discoveryServerConfiguration =
            DiscoveryServerConfiguration.builder()
                    .serverHostname("012.01.01.01")
                    .serverPort(443)
                    .build();
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
        final UpdateHandler handler = new UpdateHandler();
        final DescribeStorageSystemResponse describeStorageSystemResponse = buildDefaultReadResponse();
        final ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();

        doReturn(describeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());
        doReturn(listTagsForResourceResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(ListTagsForResourceRequest.class), any());
        doReturn(UpdateStorageSystemResponse.builder().build()).when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateStorageSystemRequest.class), any());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureInvalidRequest() {
        final UpdateHandler updateHandler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .build();

        doThrow(InvalidRequestException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateStorageSystemRequest.class), any());

        assertThrows(CfnInvalidRequestException.class, () -> {
            updateHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureNotFound() {
        final UpdateHandler updateHandler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();
        final InvalidRequestException invalidRequestException =
                InvalidRequestException.builder()
                        .message("Request references a resource which does not exist")
                        .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .build();

        doThrow(invalidRequestException).when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateStorageSystemRequest.class), any());

        assertThrows(CfnNotFoundException.class, () -> {
            updateHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final UpdateHandler updateHandler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .build();

        doThrow(InternalException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateStorageSystemRequest.class), any());

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            updateHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final UpdateHandler updateHandler = new UpdateHandler();
        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .build();

        doThrow(DataSyncException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateStorageSystemRequest.class), any());

        assertThrows(CfnGeneralServiceException.class, () -> {
            updateHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_StorageSystemAndTagsUpdateSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(
                new Answer() {
                    final DescribeStorageSystemResponse describeStorageSystemResponse = buildDefaultReadResponse();
                    ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();

                    public Object answer(InvocationOnMock invocation) {
                        Class type = invocation.getArgument(0).getClass();
                        if (ListTagsForResourceRequest.class.equals(type)) {
                            return listTagsForResourceResponse;
                        } else if (TagResourceRequest.class.equals(type)) {
                            // Very low-key mocking here, but we would like to verify that at least
                            // one tag-updating call is made or this test doesn't really test anything
                            listTagsForResourceResponse = TagTestResources.buildUpdatedTagsResponse();
                        }
                        return describeStorageSystemResponse;
                    }
                });
        final ResourceModel model = buildDefaultModel();
        final ResourceModel updatedModel = buildUpdatedModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(model)
                .previousResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .desiredResourceState(updatedModel)
                .desiredResourceTags(TagTranslator.translateTagsToMap(TagTestResources.updatedTags))
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getResourceModel().getTags()).isEqualTo(TagTestResources.updatedTags);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddSystemTagForImportedResource() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();
        final ResourceModel updatedModel = buildDefaultModel();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(
                new Answer() {
                    final DescribeStorageSystemResponse describeStorageSystemResponse = buildDefaultReadResponse();
                    ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();

                    public Object answer(InvocationOnMock invocation) {
                        Class type = invocation.getArgument(0).getClass();
                        if (ListTagsForResourceRequest.class.equals(type)) {
                            return listTagsForResourceResponse;
                        } else if (TagResourceRequest.class.equals(type)) {
                            listTagsForResourceResponse = TagTestResources.buildTagsWithSystemTagResponse();
                        }
                        return describeStorageSystemResponse;
                    }
                }
        );

        Map<String, String> mockSystemTag = new HashMap<String, String>() {{
            put("aws:cloudformation:stackid", "123");
        }};

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(model)
                .desiredResourceState(updatedModel)
                .previousResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .systemTags(mockSystemTag)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getTags()).isEqualTo(TagTestResources.defaultTags);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SystemTagInvalidAddRequest() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();
        final ResourceModel updatedModel = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(model)
                .previousResourceTags(TagTranslator.translateTagsToMap(TagTestResources.defaultTags))
                .desiredResourceState(updatedModel)
                .desiredResourceTags(TagTranslator.translateTagsToMap(TagTestResources.TagsWithSystemTag))
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    private static DescribeStorageSystemResponse buildDefaultReadResponse() {
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
        return ResourceModel.builder()
                .storageSystemArn(storageSystemArn)
                .serverConfiguration(Translator.translateToResourceModelServerConfiguration(discoveryServerConfiguration))
                .serverCredentials(buildDefaultServerCredentials())
                .systemType(systemType)
                .agentArns(Collections.singletonList(agentArn))
                .name(storageSystemName)
                .connectivityStatus(connectivityStatus)
                .cloudWatchLogGroupArn(cloudwatchLogGroupArn)
                .build();
    }

    private static ResourceModel buildUpdatedModel() {
        return ResourceModel.builder()
                .storageSystemArn(storageSystemArn)
                .serverConfiguration(Translator.translateToResourceModelServerConfiguration(discoveryServerConfiguration))
                .serverCredentials(buildDefaultServerCredentials())
                .systemType(systemType)
                .agentArns(Collections.singletonList(agentArn))
                .name("NewName")
                .connectivityStatus(connectivityStatus)
                .cloudWatchLogGroupArn(cloudwatchLogGroupArn)
                .build();
    }

    private static ServerCredentials buildDefaultServerCredentials() {
        return ServerCredentials.builder()
                .username("username")
                .password("password")
                .build();
    }
}