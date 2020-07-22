package software.amazon.datasync.task;

import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeTaskResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.UpdateTaskRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

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

        doReturn(describeTaskResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(),
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
        } );
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
        } );

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
        } );
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
                .build();
    }
}
