package software.amazon.datasync.storagesystem;

import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.RemoveStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.RemoveStorageSystemResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

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
        final DeleteHandler deleteHandler = new DeleteHandler();
        final RemoveStorageSystemResponse removeStorageSystemResponse =
                RemoveStorageSystemResponse.builder().build();
        final DescribeStorageSystemResponse describeStorageSystemResponse =
                DescribeStorageSystemResponse.builder().build();
        final ResourceModel resourceModel = buildDefaultModel();

        doReturn(removeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(RemoveStorageSystemRequest.class), any());
        doReturn(describeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                deleteHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureNotFoundRequest() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel resourceModel = buildDefaultModel();

        doThrow(InvalidRequestException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnNotFoundException.class, () -> {
            deleteHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureDescribeCallInternalExceptionRequest() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel resourceModel = buildDefaultModel();

        doThrow(InternalException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            deleteHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureDescribeCallDataSyncExceptionRequest() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel resourceModel = buildDefaultModel();

        doThrow(DataSyncException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            deleteHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_ResourceInUseRequest() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel resourceModel = buildDefaultModel();
        final DescribeStorageSystemResponse describeStorageSystemResponse =
                DescribeStorageSystemResponse.builder().build();

        final InvalidRequestException invalidRequestException =
                InvalidRequestException.builder()
                        .message("Request references a resource that is in use")
                        .build();

        doReturn(describeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        doThrow(invalidRequestException).when(proxy)
                .injectCredentialsAndInvokeV2(any(RemoveStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnInvalidRequestException.class, () -> {
            deleteHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel resourceModel = buildDefaultModel();
        final DescribeStorageSystemResponse describeStorageSystemResponse =
                DescribeStorageSystemResponse.builder().build();

        doReturn(describeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        doThrow(InternalException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(RemoveStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            deleteHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_SlsInternalException() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final RemoveStorageSystemResponse removeStorageSystemResponse =
                RemoveStorageSystemResponse.builder().build();
        final ResourceModel resourceModel = buildDefaultModel();
        final DescribeStorageSystemResponse describeStorageSystemResponse =
                DescribeStorageSystemResponse.builder().build();

        doReturn(removeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(RemoveStorageSystemRequest.class), any());

        final InvalidRequestException slsException =
                InvalidRequestException.builder()
                        .message("Unable to describe secret associated with StorageSystem")
                        .build();

        doThrow(slsException).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                deleteHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel resourceModel = buildDefaultModel();
        final DescribeStorageSystemResponse describeStorageSystemResponse =
                DescribeStorageSystemResponse.builder().build();

        doReturn(describeStorageSystemResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeStorageSystemRequest.class), any());

        doThrow(DataSyncException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(RemoveStorageSystemRequest.class), any());

        final ResourceHandlerRequest<ResourceModel> request =
                ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(resourceModel).build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            deleteHandler.handleRequest(proxy, request, null, logger);
        });
    }

    private static ResourceModel buildDefaultModel() {
        final String storageSystemArn = "arn:aws:datasync:us-east-1:012345678901:system/storage-system-01234567-0123-b123-a123-da1234567890";
        return ResourceModel.builder()
                .storageSystemArn(storageSystemArn)
                .build();
    }
}