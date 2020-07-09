package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.*;
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

        CreateLocationEfsRequest createLocationEfsRequest = Translator.translateToCreateRequest(model);

        logger.log(String.format("\n\nRequest: %s\n\n", createLocationEfsRequest.toString()));

        CreateLocationEfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationEfsRequest, client::createLocationEfs);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createLocationEfsRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        final ResourceModel modelNoUri = ResourceModel.builder()
                .locationArn(response.locationArn())
                .eC2Config(model.getEC2Config())
                .efsFilesystemArn(model.getEfsFilesystemArn())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        final ResourceModel returnModel = retrieveUpdatedModel(modelNoUri, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private static ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                               final AmazonWebServicesClientProxy proxy,
                                               final DataSyncClient client) {

        DescribeLocationEfsRequest describeLocationEfsRequest = Translator.translateToReadRequest(model.getLocationArn());
        DescribeLocationEfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationEfsRequest, client::describeLocationEfs);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(model.getLocationArn())
                .locationUri(response.locationUri())
//                .eC2Config(model.getEC2Config())
//                .efsFilesystemArn(model.getEfsFilesystemArn())
//                .subdirectory(model.getSubdirectory())
//                .tags(model.getTags())
                .build();

        return returnModel;
    }

}
