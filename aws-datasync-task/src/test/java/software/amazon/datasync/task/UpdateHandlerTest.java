package software.amazon.datasync.task;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

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

        final DescribeTaskResponse describeTaskResponse = buildDefaultResponse();
        final ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();

        doReturn(describeTaskResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(),
                        any()
                );

        doReturn(listTagsForResourceResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(ListTagsForResourceRequest.class),
                        any()
                );

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
    public void handleRequest_TagUpdateSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(
                new Answer() {
                    final DescribeTaskResponse describeTaskResponse = buildDefaultResponse();
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
                        return describeTaskResponse;
                    }
                }
        );

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
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getTags()).isEqualTo(request.getDesiredResourceState().getTags());
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
                    final DescribeTaskResponse describeTaskResponse = buildDefaultResponse();
                    ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();

                    public Object answer(InvocationOnMock invocation) {
                        Class type = invocation.getArgument(0).getClass();
                        if (ListTagsForResourceRequest.class.equals(type)) {
                            return listTagsForResourceResponse;
                        } else if (TagResourceRequest.class.equals(type)) {
                            listTagsForResourceResponse = TagTestResources.buildTagsWithSystemTagResponse();
                        }
                        return describeTaskResponse;
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

    @Test
    public void handleRequest_FailureNotFoundRequest() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        doThrow(InvalidRequestException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateTaskRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        doThrow(InternalException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateTaskRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });

    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        doThrow(DataSyncException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateTaskRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    private static DescribeTaskResponse buildDefaultResponse() {
        final String taskArn = "arn:aws:datasync:us-east-2:123456789012:task/task-01234567890123456";
        final String destinationLocationArn =
                "arn:aws:datasync:us-east-1:123456789012:location/loc-12345678901234567";
        final String sourceLocationArn =
                "arn:aws:datasync:us-east-1:123456789012:location/loc-12345678901234567";
        return DescribeTaskResponse.builder()
                .taskArn(taskArn)
                .destinationLocationArn(destinationLocationArn)
                .sourceLocationArn(sourceLocationArn)
                .name("MyUpdatedTask")
                .build();
    }

    private static ResourceModel buildDefaultModel() {
        final String taskArn = "arn:aws:datasync:us-east-2:123456789012:task/task-01234567890123456";
        final String destinationLocationArn =
                "arn:aws:datasync:us-east-1:123456789012:location/loc-12345678901234567";
        final String sourceLocationArn =
                "arn:aws:datasync:us-east-1:123456789012:location/loc-12345678901234567";
        return ResourceModel.builder()
                .name("MyUpdatedTask")
                .destinationLocationArn(destinationLocationArn)
                .sourceLocationArn(sourceLocationArn)
                .taskArn(taskArn)
                .tags(TagTestResources.defaultTags)
                .build();
    }

    private static ResourceModel buildUpdatedModel() {
        final String taskArn = "arn:aws:datasync:us-east-2:123456789012:task/task-01234567890123456";
        final String destinationLocationArn =
                "arn:aws:datasync:us-east-1:123456789012:location/loc-12345678901234567";
        final String sourceLocationArn =
                "arn:aws:datasync:us-east-1:123456789012:location/loc-12345678901234567";
        return ResourceModel.builder()
                .name("MyUpdatedTask")
                .destinationLocationArn(destinationLocationArn)
                .sourceLocationArn(sourceLocationArn)
                .taskArn(taskArn)
                .tags(TagTestResources.updatedTags)
                .build();
    }
}
