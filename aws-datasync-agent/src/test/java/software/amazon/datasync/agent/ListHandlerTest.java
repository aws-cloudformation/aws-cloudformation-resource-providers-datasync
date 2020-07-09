package software.amazon.datasync.agent;


import software.amazon.awssdk.services.datasync.model.AgentListEntry;
import software.amazon.awssdk.services.datasync.model.ListAgentsRequest;
import software.amazon.awssdk.services.datasync.model.ListAgentsResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

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
        final ListHandler handler = new ListHandler();

        final List<AgentListEntry> agents = buildDefaultList();

        ListAgentsResponse listAgentsResponse = ListAgentsResponse.builder()
                .agents(agents)
                .nextToken(null)
                .build();

        doReturn(listAgentsResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(ListAgentsRequest.class), any());

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

    private static List<AgentListEntry> buildDefaultList() {
        final String agentArn1 = "arn:aws:datasync:us-east-1:123456789012:agent/agent-01234567890123456";
        final String agentArn2 = "arn:aws:datasync:us-east-1:123456789012:agent/agent-abcdefabcdefabcde";
        AgentListEntry agent1 = AgentListEntry.builder().agentArn(agentArn1).build();
        AgentListEntry agent2 = AgentListEntry.builder().agentArn(agentArn2).build();
        return Arrays.asList(agent1, agent2);
    }

    private static ResourceModel buildDefaultModel1() {
        final String agentArn = "arn:aws:datasync:us-east-1:123456789012:agent/agent-01234567890123456";
        return ResourceModel.builder()
                .agentArn(agentArn)
                .build();
    }

    private static ResourceModel buildDefaultModel2() {
        final String agentArn = "arn:aws:datasync:us-east-1:123456789012:agent/agent-abcdefabcdefabcde";
        return ResourceModel.builder()
                .agentArn(agentArn)
                .build();
    }
}
