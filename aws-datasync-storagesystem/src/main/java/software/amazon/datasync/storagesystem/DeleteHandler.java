package software.amazon.datasync.storagesystem;


import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.RemoveStorageSystemRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {

                    //Handle the case where StorageSystem does not exist
                    final DescribeStorageSystemRequest describeStorageSystemRequest =
                            Translator.translateToReadRequest(model.getStorageSystemArn());
                    try {
                        proxy.injectCredentialsAndInvokeV2(describeStorageSystemRequest, client::describeStorageSystem);
                        logger.log(String.format("Verified %s %s exits before deletion", ResourceModel.TYPE_NAME, model.getStorageSystemArn()));
                    } catch (InvalidRequestException e) {
                        // We don't want to fail remove in the case that a secret does not exist.
                        // We also know that if the describeStorageSystemRequest throws an error related to the secret it means the storage system does exist.
                        if(e.getMessage() == null || (e.getMessage() != null && !e.getMessage().contains("Unable to describe secret associated with StorageSystem"))){
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getStorageSystemArn());
                        }
                    } catch (InternalException e) {
                        throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
                    } catch (DataSyncException e) {
                        throw Translator.translateDataSyncExceptionToCfnException(e);
                    }

                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.IN_PROGRESS)
                            .build();

                }).then(progress -> {
                    final RemoveStorageSystemRequest removeStorageSystemRequest =
                            Translator.translateToDeleteRequest(model.getStorageSystemArn());
                    try {
                        proxy.injectCredentialsAndInvokeV2(removeStorageSystemRequest, client::removeStorageSystem);
                        logger.log(String.format("%s %s deleted successfully", ResourceModel.TYPE_NAME, model.getStorageSystemArn()));
                    } catch (InvalidRequestException e) {
                        //Covers only the case of StorageSystem delete failing due to active discovery job(s)
                        throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
                    } catch (InternalException e) {
                        throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
                    } catch (DataSyncException e) {
                        throw Translator.translateDataSyncExceptionToCfnException(e);
                    }

                    return ProgressEvent.defaultSuccessHandler(null);
                });
    }
}
