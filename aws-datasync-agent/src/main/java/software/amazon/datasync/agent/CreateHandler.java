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
import software.amazon.cloudformation.proxy.*;

public class CreateHandler extends BaseHandlerStd {

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DataSyncClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = proxyClient.client();

        if (model.getAgentArn() != null) {
            throw new CfnInvalidRequestException("AgentArn cannot be specified to create an Agent.");
        }

        CreateAgentRequest createAgentRequest = Translator.translateToCreateRequest(model);

        // Get combined resource- and stack-level tags, since CFN stack stags are not in the model.
        Map<String, String> tagList = request.getDesiredResourceTags();
        if (tagList == null) {
            tagList = new HashMap<String, String>();
        }

        CreateAgentRequest createAgentRequest = Translator.translateToCreateRequest(model, tagList);
        CreateAgentResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createAgentRequest, client::createAgent);
            logger.log(String.format("%s created successfully", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .agentArn(response.agentArn())
                .agentName(model.getAgentName())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .tags(Translator.translateMapToTags(tagList))
                .build();

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }
}
