package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.model.CreateLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.CreateLocationEfsResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationEfsResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

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
        final CreateHandler handler = new CreateHandler();

        final CreateLocationEfsResponse createLocationEfsResponse =
                CreateLocationEfsResponse.builder()
                        .build();

        final DescribeLocationEfsResponse describeLocationEfsResponse =
                DescribeLocationEfsResponse.builder()
                        .build();

        doReturn(createLocationEfsResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(CreateLocationEfsRequest.class),
                        any()
                );

        doReturn(describeLocationEfsResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(DescribeLocationEfsRequest.class),
                        any()
                );

        ResourceModel model = buildDefaultModel();

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
        final CreateHandler handler = new CreateHandler();

        doThrow(InvalidRequestException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateLocationEfsRequest.class), any());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final CreateHandler handler = new CreateHandler();

        doThrow(InternalException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateLocationEfsRequest.class), any());

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
        final CreateHandler handler = new CreateHandler();

        doThrow(DataSyncException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateLocationEfsRequest.class), any());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        } );
    }


    private static ResourceModel buildDefaultModel() {
        final String subnetArn = "arn:aws:ec2:us-east-1:123456789012:subnet/subnet-1234567890123456";
        final String securityGroupArn = "arn:aws:ec2:us-east-1:123456789012:security-group/sg-1234567890123456";
        final String efsFilesystemArn = "arn:aws:elasticfilesystem:us-east-1:123456789012:fs-01234567";
        EC2Config ec2Config = EC2Config.builder()
                .subnetArn(subnetArn)
                .securityGroupArns(Arrays.asList(securityGroupArn))
                .build();
        return ResourceModel.builder()
                .efsFilesystemArn(efsFilesystemArn)
                .eC2Config(ec2Config)
                .build();
    }
}
