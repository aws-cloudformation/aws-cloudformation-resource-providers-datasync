package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.*;
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
            throw new CfnInvalidRequestException(listLocationsRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        List<ResourceModel> models = new ArrayList<>();
        for (LocationListEntry loc : response.locations()) {
            ResourceModel model = ResourceModel.builder()
                    .locationArn(loc.locationArn())
                    .locationUri(loc.locationUri())
                    .build();
            models.add(model);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(response.nextToken())
                .build();
    }
}
