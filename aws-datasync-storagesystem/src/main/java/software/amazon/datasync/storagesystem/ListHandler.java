package software.amazon.datasync.storagesystem;

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListStorageSystemsRequest;
import software.amazon.awssdk.services.datasync.model.ListStorageSystemsResponse;
import software.amazon.awssdk.services.datasync.model.StorageSystemListEntry;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final String nextToken = request.getNextToken();
        final DataSyncClient client = ClientBuilder.getClient();

        final ListStorageSystemsRequest listStorageSystemsRequest = Translator.translateToListRequest(nextToken);

        final ListStorageSystemsResponse listStorageSystemsResponse;
        try {
            listStorageSystemsResponse = proxy.injectCredentialsAndInvokeV2(listStorageSystemsRequest, client::listStorageSystems);
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        final List<ResourceModel> models = new ArrayList<>();
        for (StorageSystemListEntry storageSystemListEntry: listStorageSystemsResponse.storageSystems()) {
            ResourceModel model = ResourceModel.builder()
                    .storageSystemArn(storageSystemListEntry.storageSystemArn())
                    .name(storageSystemListEntry.name())
                    .build();
            models.add(model);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(listStorageSystemsResponse.nextToken())
                .build();
    }
}