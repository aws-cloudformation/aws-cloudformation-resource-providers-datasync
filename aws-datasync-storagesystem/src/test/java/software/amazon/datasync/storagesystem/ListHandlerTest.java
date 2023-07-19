package software.amazon.datasync.storagesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListStorageSystemsRequest;
import software.amazon.awssdk.services.datasync.model.ListStorageSystemsResponse;
import software.amazon.awssdk.services.datasync.model.StorageSystemListEntry;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private static final String storageSystemArn = "arn:aws:datasync:us-east-1:012345678901:system/storage-system-01234567-0123-b123-a123-da1234567890";
    private static final String storageSystemName = "TestStorageSystem";
    private static final String storageSystemArn1 = "arn:aws:datasync:us-east-1:012345678901:system/storage-system-a1234567-b123-c123-d123-ea1234567890";
    private static final String storageSystemName1 = "TestStorageSystem1";

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();
        final List<StorageSystemListEntry> storageSystemEntryList = buildDefaultList();

        ListStorageSystemsResponse listStorageSystemsResponse =
                ListStorageSystemsResponse.builder()
                        .storageSystems(storageSystemEntryList)
                        .nextToken(null)
                        .build();

        doReturn(listStorageSystemsResponse).when(proxy)
                .injectCredentialsAndInvokeV2(any(ListStorageSystemsRequest.class), any());

        final ResourceModel model1 = buildDefaultModel1();
        final ResourceModel model2 = buildDefaultModel2();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).containsAll(Arrays.asList(model1, model2));
        assertThat(response.getNextToken()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureInvalidRequest() {
        final ListHandler listHandler = new ListHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        doThrow(InvalidRequestException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(ListStorageSystemsRequest.class), any());

        assertThrows(CfnInvalidRequestException.class, () -> {
            listHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final ListHandler listHandler = new ListHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        doThrow(InternalException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(ListStorageSystemsRequest.class), any());

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            listHandler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final ListHandler listHandler = new ListHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

        doThrow(DataSyncException.class).when(proxy)
                .injectCredentialsAndInvokeV2(any(ListStorageSystemsRequest.class), any());

        assertThrows(CfnGeneralServiceException.class, () -> {
            listHandler.handleRequest(proxy, request, null, logger);
        });
    }

    private static List<StorageSystemListEntry> buildDefaultList() {
        final StorageSystemListEntry entry =
                StorageSystemListEntry.builder()
                        .storageSystemArn(storageSystemArn)
                        .name(storageSystemName)
                        .build();
        final StorageSystemListEntry entry1 =
                StorageSystemListEntry.builder()
                        .storageSystemArn(storageSystemArn1)
                        .name(storageSystemName1)
                        .build();
        return Arrays.asList(entry, entry1);
    }

    private static ResourceModel buildDefaultModel1() {
        return ResourceModel.builder()
                .storageSystemArn(storageSystemArn)
                .name(storageSystemName)
                .build();
    }

    private static ResourceModel buildDefaultModel2() {
        return ResourceModel.builder()
                .storageSystemArn(storageSystemArn1)
                .name(storageSystemName1)
                .build();
    }
}