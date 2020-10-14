package software.amazon.datasync.locationobjectstorage;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsResponse;
import software.amazon.awssdk.services.datasync.model.LocationListEntry;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
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

        final ListLocationsRequest listLocationsRequest = Translator.translateToListRequest(nextToken);

        final ListLocationsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(listLocationsRequest, client::listLocations);
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        final List<ResourceModel> models = new ArrayList<>();
        for (LocationListEntry loc : response.locations()) {
            // Add only if it is an Object Storage location
            if (loc.locationUri().startsWith("object-storage://")) {
                ResourceModel model = ResourceModel.builder()
                        .locationArn(loc.locationArn())
                        .locationUri(loc.locationUri())
                        .build();
                models.add(model);
            }
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(response.nextToken())
                .build();
    }
}
