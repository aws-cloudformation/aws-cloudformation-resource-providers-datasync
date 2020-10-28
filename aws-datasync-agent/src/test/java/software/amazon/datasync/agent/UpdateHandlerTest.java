package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.datasync.model.TagResourceRequest;
import software.amazon.awssdk.services.datasync.model.UpdateAgentRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        final DescribeAgentResponse describeAgentResponse = buildDefaultResponse();
        final ListTagsForResourceResponse listTagsForResourceResponse = buildDefaultTagsResponse();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(
                invocation -> {
                    Class type = invocation.getArgument(0).getClass();
                    if (ListTagsForResourceRequest.class.equals(type)) {
                        return listTagsForResourceResponse;
                    }
                    return describeAgentResponse;
                }
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
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_TagUpdateSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(
                new Answer() {
                    final DescribeAgentResponse describeAgentResponse = buildDefaultResponse();
                    ListTagsForResourceResponse listTagsForResourceResponse = buildDefaultTagsResponse();

                    public Object answer(InvocationOnMock invocation) {
                        Class type = invocation.getArgument(0).getClass();
                        if (ListTagsForResourceRequest.class.equals(type)) {
                            return listTagsForResourceResponse;
                        } else if (TagResourceRequest.class.equals(type)) {
                            // Very low-key mocking here, but we would like to verify that at least
                            // one tag-updating call is made or this test doesn't really test anything
                            listTagsForResourceResponse = buildUpdatedTagsResponse();
                        }
                        return describeAgentResponse;
                    }
                }
        );

        final ResourceModel model = buildDefaultModel();
        final ResourceModel updatedModel = buildUpdatedModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(model)
                .previousResourceTags(Translator.translateTagsToMap(defaultTags))
                .desiredResourceState(updatedModel)
                .desiredResourceTags(Translator.translateTagsToMap(updatedTags))
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureNotFoundRequest() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        doThrow(InvalidRequestException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateAgentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        doThrow(InternalException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateAgentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );

    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final UpdateHandler handler = new UpdateHandler();

        final ResourceModel model = buildDefaultModel();

        doThrow(DataSyncException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(UpdateAgentRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );
    }

    final static Set<Tag> defaultTags = new HashSet<Tag>(Arrays.asList(
            Tag.builder().key("Constant").value("Should remain").build(),
            Tag.builder().key("Update").value("Should be updated").build(),
            Tag.builder().key("Delete").value("Should be deleted").build()
    ));

    final static Set<Tag> updatedTags = new HashSet<Tag>(Arrays.asList(
            Tag.builder().key("Constant").value("Should remain").build(),
            Tag.builder().key("Update").value("Has been updated").build(),
            Tag.builder().key("Add").value("Should be added").build()
    ));

    private static ResourceModel buildDefaultModel() {
        final String agentArn = "arn:aws:datasync:us-east-1:123456789012:agent/agent-01234567890123456";
        return ResourceModel.builder()
                .agentName("MyUpdatedAgent")
                .agentArn(agentArn)
                .tags(defaultTags)
                .build();
    }

    private static ResourceModel buildUpdatedModel() {
        final String agentArn = "arn:aws:datasync:us-east-1:123456789012:agent/agent-01234567890123456";
        return ResourceModel.builder()
                .agentName("MyUpdatedAgent")
                .agentArn(agentArn)
                .tags(updatedTags)
                .build();
    }

    private static DescribeAgentResponse buildDefaultResponse() {
        final String agentArn = "arn:aws:datasync:us-east-1:123456789012:agent/agent-01234567890123456";
        return DescribeAgentResponse.builder()
                .name("MyUpdatedAgent")
                .agentArn(agentArn)
                .build();
    }

    private static ListTagsForResourceResponse buildDefaultTagsResponse() {
        return ListTagsForResourceResponse.builder()
                .tags(Translator.translateTags(defaultTags))
                .build();
    }

    private static ListTagsForResourceResponse buildUpdatedTagsResponse() {
        return ListTagsForResourceResponse.builder()
                .tags(Translator.translateTags(updatedTags))
                .build();
    }

}
