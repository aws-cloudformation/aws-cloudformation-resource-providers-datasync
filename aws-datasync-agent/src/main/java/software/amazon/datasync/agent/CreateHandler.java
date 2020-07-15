package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateAgentRequest;
import software.amazon.awssdk.services.datasync.model.CreateAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        CreateAgentRequest createAgentRequest = Translator.translateToCreateRequest(model);

        CreateAgentResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createAgentRequest, client::createAgent);
            logger.log(String.format("%s created successfully", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createAgentRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .agentArn(response.agentArn())
                .agentName(model.getAgentName())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .tags(model.getTags())
                .build();

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }
