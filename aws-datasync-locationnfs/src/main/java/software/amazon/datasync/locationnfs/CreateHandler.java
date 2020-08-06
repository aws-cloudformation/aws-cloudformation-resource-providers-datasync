package software.amazon.datasync.locationnfs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.CreateLocationNfsResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationNfsResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
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

        CreateLocationNfsRequest createLocationNfsRequest = Translator.translateToCreateRequest(model);

        CreateLocationNfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationNfsRequest, client::createLocationNfs);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createLocationNfsRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(createLocationNfsRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(createLocationNfsRequest.toString(), e.getCause());
        }

        ResourceModel modelNoUri = ResourceModel.builder()
                .mountOptions(model.getMountOptions())
                .locationArn(response.locationArn())
                .onPremConfig(model.getOnPremConfig())
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        final ResourceModel returnModel = retrieveUpdatedModel(modelNoUri, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private static ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                                      final AmazonWebServicesClientProxy proxy,
                                                      final DataSyncClient client) {

        DescribeLocationNfsRequest describeLocationNfsRequest = Translator.translateToReadRequest(model.getLocationArn());
        DescribeLocationNfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationNfsRequest, client::describeLocationNfs);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        return ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .mountOptions(Translator.translateToResourceModelMountOptions(response.mountOptions()))
                .onPremConfig(Translator.translateToResourceModelOnPremConfig(response.onPremConfig()))
                .subdirectory(model.getSubdirectory())
                .serverHostname(model.getServerHostname())
                .tags(model.getTags())
                .build();
    }
}
