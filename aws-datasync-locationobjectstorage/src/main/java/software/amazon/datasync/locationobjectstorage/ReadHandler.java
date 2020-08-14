package software.amazon.datasync.locationobjectstorage;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        final DescribeLocationObjectStorageRequest describeLocationObjectStorageRequest =
                Translator.translateToReadRequest(model.getLocationArn());

        DescribeLocationObjectStorageResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationObjectStorageRequest, client::describeLocationObjectStorage);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getLocationArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(describeLocationObjectStorageRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(describeLocationObjectStorageRequest.toString(), e.getCause());
        }

        Double serverPort = response.serverPort() == null ? null : response.serverPort().doubleValue();
        ResourceModel returnModel = ResourceModel.builder()
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

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(returnModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
