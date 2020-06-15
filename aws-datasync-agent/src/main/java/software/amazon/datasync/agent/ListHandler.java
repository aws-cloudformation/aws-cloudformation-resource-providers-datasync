package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
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

        final List<ResourceModel> models = listAgents(nextToken, proxy, client);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private List<ResourceModel> listAgents(final String nextToken, final AmazonWebServicesClientProxy proxy,
                                           DataSyncClient client) {

        final ListAgentsRequest request = Translator.translateToListRequest(nextToken);

        ListAgentsResponse response = proxy.injectCredentialsAndInvokeV2(request, client::listAgents);

        List<ResourceModel> models = new ArrayList<>();

        // For each agent, add its resource model to the models list
        response.agents().forEach(a -> {
            ResourceModel model = ResourceModel.builder()
                    .agentArn(a.agentArn())
                    .build();
            models.add(model);
        });

        return models;
    }
}
