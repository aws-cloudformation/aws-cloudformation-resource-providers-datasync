package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DeleteAgentRequest;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final DataSyncClient client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();

        DeleteAgentRequest deleteAgentRequest = Translator.translateToDeleteRequest(model);

        try {
            proxy.injectCredentialsAndInvokeV2(deleteAgentRequest, client::deleteAgent);
            logger.log(String.format("%s %s deleted successfully", ResourceModel.TYPE_NAME,
                    model.getAgentArn()));
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getAgentArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(deleteAgentRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(deleteAgentRequest.toString(), e.getCause());
        }
        return ProgressEvent.defaultSuccessHandler(null);

    }
}
