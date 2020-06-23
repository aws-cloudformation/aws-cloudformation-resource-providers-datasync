package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DescribeAgentRequest;
import software.amazon.awssdk.services.datasync.model.DescribeAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.UpdateAgentRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        // If anything except for Name or AgentArn is not null, throw a NotUpdatable exception
        if (!checkAttributes(model)) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, model.getAgentArn());
        }

        UpdateAgentRequest updateAgentRequest = Translator.translateToUpdateRequest(model);

        try {
            proxy.injectCredentialsAndInvokeV2(updateAgentRequest, client::updateAgent);
            logger.log(String.format("%s %s updated successfully", ResourceModel.TYPE_NAME,
                    model.getAgentArn().toString()));
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getAgentArn().toString());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        ResourceModel returnModel = retrieveUpdatedModel(model, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                               final AmazonWebServicesClientProxy proxy,
                                               final DataSyncClient client) {
        DescribeAgentRequest describeAgentRequest= Translator.translateToReadRequest(model);
        DescribeAgentResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeAgentRequest, client::describeAgent);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .agentArn(response.agentArn())
                .agentName(response.name())
                .build();

        return returnModel;
    }

    private boolean checkAttributes(ResourceModel model) {
        return (model.getSubnetArns() == null && model.getSecurityGroupArns() == null
                && model.getActivationKey() == null && model.getVpcEndpointId() == null
                && model.getTags() == null);
    }

}
