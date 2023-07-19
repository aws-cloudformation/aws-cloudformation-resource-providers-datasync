package software.amazon.datasync.storagesystem;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.UpdateStorageSystemRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private static final String RESOURCE_NOT_FOUND = "Request references a resource which does not exist";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        final UpdateStorageSystemRequest updateStorageSystemRequest = Translator.translateToUpdateRequest(model);

        try {
            proxy.injectCredentialsAndInvokeV2(updateStorageSystemRequest, client::updateStorageSystem);
            logger.log(String.format("%s %s updated successfully.", ResourceModel.TYPE_NAME, model.getStorageSystemArn()));
        } catch (InvalidRequestException e) {
            //To-do: Can make a describe call to verify the not found scenario.
            if (e.getMessage()!=null && e.getMessage().equals(RESOURCE_NOT_FOUND)) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getStorageSystemArn());
            }
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        // Tags are not handled by the Update call and must be updated separately
        TagRequestMaker.updateTagsForResource(proxy, client, model.getStorageSystemArn(), request, logger);

        return new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
    }
}