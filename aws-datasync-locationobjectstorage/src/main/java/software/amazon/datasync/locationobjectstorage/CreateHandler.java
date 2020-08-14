package software.amazon.datasync.locationobjectstorage;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.CreateLocationObjectStorageResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageResponse;
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

        CreateLocationObjectStorageRequest createLocationObjectStorageRequest =
                Translator.translateToCreateRequest(model);

        CreateLocationObjectStorageResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationObjectStorageRequest, client::createLocationObjectStorage);
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createLocationObjectStorageRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(createLocationObjectStorageRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(createLocationObjectStorageRequest.toString(), e.getCause());
        }

        final ResourceModel modelNoUri = ResourceModel.builder()
                .bucketName(model.getBucketName())
                .secretKey(model.getSecretKey())
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .serverPort(model.getServerPort())
                .tags(model.getTags())
                .locationArn(response.locationArn())
                .build();

        ResourceModel returnModel = retrieveUpdatedModel(modelNoUri, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private static ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                                      final AmazonWebServicesClientProxy proxy,
                                                      final DataSyncClient client) {

        DescribeLocationObjectStorageRequest describeLocationObjectStorageRequest =
                Translator.translateToReadRequest(model.getLocationArn());
        DescribeLocationObjectStorageResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationObjectStorageRequest, client::describeLocationObjectStorage);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        Double serverPort = response.serverPort() == null ? null : response.serverPort().doubleValue();
        return ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .accessKey(response.accessKey())
                .agentArns(response.agentArns())
                .bucketName(model.getBucketName())
                .secretKey(model.getSecretKey())
                .serverHostname(model.getServerHostname())
                .serverPort(serverPort)
                .serverProtocol(response.serverProtocolAsString())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();
    }
}
