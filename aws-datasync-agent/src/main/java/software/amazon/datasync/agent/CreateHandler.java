package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateAgentRequest;
import software.amazon.awssdk.services.datasync.model.CreateAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.*;

import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandlerStd {
    private static final String AWS_TAG_PREFIX = "aws:";

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

        Map<String, String> tagList = request.getDesiredResourceTags();
        if (tagList == null) {
            tagList = new HashMap<String, String>();
        }

        // Check for invalid requested system tags.
        for (String key : tagList.keySet()) {
            if (key.trim().toLowerCase().startsWith(AWS_TAG_PREFIX)) {
                throw new CfnInvalidRequestException(key + " is an invalid key. aws: prefixed tag key names cannot be requested.");
            }
        }

        //  Retrieve default stack-level tags with aws:cloudformation prefix.
        Map<String, String> systemTagList = request.getSystemTags();
        if (systemTagList != null) {
            tagList.putAll(systemTagList);
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
            throw Translator.translateDataSyncExceptionToCfnException(e);
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
