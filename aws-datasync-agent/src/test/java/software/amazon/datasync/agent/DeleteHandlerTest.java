package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteAgentRequest;
import software.amazon.awssdk.services.datasync.model.DeleteAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock private ProxyClient<DataSyncClient> proxyClient;

    @Mock DataSyncClient dataSyncClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        dataSyncClient = mock(DataSyncClient.class);
        proxyClient = MOCK_PROXY(proxy, dataSyncClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final DeleteHandler handler = new DeleteHandler();

        final DeleteAgentResponse deleteAgentResponse = DeleteAgentResponse.builder().build();

        final ResourceModel model = buildDefaultModel();

        when(proxyClient.client().deleteAgent(any(DeleteAgentRequest.class)))
                .thenReturn(deleteAgentResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

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
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().deleteAgent(any(DeleteAgentRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ResourceModel model = buildDefaultModel();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        } );
    }

    @Test
    public void handleRequest_FailureInternalException() {
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().deleteAgent(any(DeleteAgentRequest.class)))
                .thenThrow(InternalException.builder().build());


        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        } );

    }

    @Test
    public void handleRequest_FailureDataSyncException() {
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().deleteAgent(any(DeleteAgentRequest.class)))
                .thenThrow(DataSyncException.builder().build());

        final ResourceModel model = buildDefaultModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        } );
    }

    private static ResourceModel buildDefaultModel() {
        final String agentArn = "arn:aws:datasync:us-east-1:123456789012:agent/agent-01234567890123456"; // dummy agent arn
        return ResourceModel.builder()
                .agentArn(agentArn)
                .build();
    }
}
