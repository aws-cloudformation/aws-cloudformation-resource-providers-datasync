package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.AgentListEntry;
import software.amazon.awssdk.services.datasync.model.ListAgentsRequest;
import software.amazon.awssdk.services.datasync.model.ListAgentsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final String nextToken = request.getNextToken();
        final DataSyncClient client = ClientBuilder.getClient();

        final ListAgentsRequest listAgentsRequest = Translator.translateToListRequest(nextToken);
        final ListAgentsResponse response = proxy.injectCredentialsAndInvokeV2(listAgentsRequest, client::listAgents);

        List<ResourceModel> models = new ArrayList<>();
        for (AgentListEntry a : response.agents()) {
            ResourceModel model = ResourceModel.builder()
                    .agentArn(a.agentArn())
                    .agentName(a.name())
                    .build();
            models.add(model);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(response.nextToken())
                .build();
    }

}
