package software.amazon.datasync.storagesystem;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.AddStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.AddStorageSystemResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private static final String AWS_TAG_PREFIX = "aws:";
    private static final String RESOURCE_ALREADY_EXISTS = "Request references a resource which already exist";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        if (callbackContext == null && (request.getDesiredResourceState().getStorageSystemArn() != null)) {
            throw new CfnInvalidRequestException("StorageSystemArn cannot be specified to create a StorageSystem.");
        }

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        validateInput(model);

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

        final AddStorageSystemRequest addStorageSystemRequest = Translator.translateToCreateRequest(model, tagList);

        final AddStorageSystemResponse addStorageSystemResponse;
        try {
            addStorageSystemResponse = proxy.injectCredentialsAndInvokeV2(addStorageSystemRequest, client::addStorageSystem);
        } catch (InvalidRequestException e) {
            //To-do: Can be replaced with DescribeCall with ARN created from request. Check via SAM test.
            if ( e.getMessage() != null && e.getMessage().equals(RESOURCE_ALREADY_EXISTS)) {
                throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, request.getClientRequestToken());
            }
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        final ResourceModel modelWithArn = ResourceModel.builder()
                .storageSystemArn(addStorageSystemResponse.storageSystemArn())
                .agentArns(model.getAgentArns())
                .serverConfiguration(model.getServerConfiguration())
                .serverCredentials(model.getServerCredentials())
                .systemType(model.getSystemType())
                .build();

        ResourceHandlerRequest<ResourceModel> requestWithArn = request.toBuilder()
                .desiredResourceState(modelWithArn)
                .build();

        return new ReadHandler().handleRequest(proxy, requestWithArn, callbackContext, logger);
    }

    private void validateInput(final ResourceModel model) {
        if(model.getSystemType() == null || model.getSystemType().isEmpty()) {
            throw new CfnInvalidRequestException("SystemType is a required property");
        }

        if(model.getServerCredentials() == null || model.getServerCredentials().getUsername() == null ||
                model.getServerCredentials().getUsername().isEmpty() || model.getServerCredentials().getPassword() == null ||
                model.getServerCredentials().getPassword().isEmpty()) {
            throw new CfnInvalidRequestException("ServerCredentials is a required property");
        }

        if(model.getAgentArns() == null || model.getAgentArns().size() == 0) {
            throw new CfnInvalidRequestException("AgentArns is a required property");
        }

        if(model.getServerConfiguration() == null) {
            throw new CfnInvalidRequestException("ServerConfiguration is a required property");
        }

        if(model.getServerConfiguration().getServerHostname() == null || model.getServerConfiguration().getServerHostname().isEmpty()) {
            throw new CfnInvalidRequestException("ServerHostname is a required property");
        }
    }
}