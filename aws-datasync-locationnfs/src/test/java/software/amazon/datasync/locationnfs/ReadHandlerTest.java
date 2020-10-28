package software.amazon.datasync.locationnfs;

import software.amazon.awssdk.services.datasync.model.*;
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

        final DescribeLocationNfsResponse describeLocationNfsResponse = buildDefaultResponse();

        final ListTagsForResourceResponse listTagsForResourceResponse = TagTestResources.buildDefaultTagsResponse();

        doReturn(describeLocationNfsResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(DescribeLocationNfsRequest.class),
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
        assertThat(response.getResourceModel()).isEqualToComparingOnlyGivenFields(describeLocationNfsResponse,
                "locationArn", "locationUri");
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureNotFoundRequest() {
        final ReadHandler handler = new ReadHandler();

        doThrow(InvalidRequestException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeLocationNfsRequest.class), any());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final ReadHandler handler = new ReadHandler();

        doThrow(InternalException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeLocationNfsRequest.class), any());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );

    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final ReadHandler handler = new ReadHandler();

        doThrow(DataSyncException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(DescribeLocationNfsRequest.class), any());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );
    }

    private static DescribeLocationNfsResponse buildDefaultResponse() {
        final String locationArn = "arn:aws:datasync:us-west-2:123456789012:location/loc-1234567890123456";
        final String locationUri = "nfs://10.0.1.0/";
        return DescribeLocationNfsResponse.builder()
                .locationArn(locationArn)
                .locationUri(locationUri)
                .build();
    }

    private static ResourceModel buildDefaultModel() {
        final String locationArn = "arn:aws:datasync:us-west-2:123456789012:location/loc-1234567890123456";
        return ResourceModel.builder()
                .locationArn(locationArn)
                .build();
    }
}
