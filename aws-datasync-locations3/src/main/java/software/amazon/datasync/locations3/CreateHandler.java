package software.amazon.datasync.locations3;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationS3Request;
import software.amazon.awssdk.services.datasync.model.CreateLocationS3Response;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Request;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Response;
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

        if (callbackContext == null && (request.getDesiredResourceState().getLocationArn() != null)) {
            throw new CfnInvalidRequestException("LocationArn cannot be specified to create a location.");
        }

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        CreateLocationS3Request createLocationS3Request = Translator.translateToCreateRequest(model);

        CreateLocationS3Response response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationS3Request, client::createLocationS3);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        final ResourceModel modelNoUri = ResourceModel.builder()
                .locationArn(response.locationArn())
                .s3BucketArn(model.getS3BucketArn())
                .s3Config(model.getS3Config())
                .s3StorageClass(model.getS3StorageClass())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        final ResourceModel returnModel = retrieveUpdatedModel(modelNoUri, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private static ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                                      final AmazonWebServicesClientProxy proxy,
                                                      final DataSyncClient client) {

        DescribeLocationS3Request describeLocationS3Request = Translator.translateToReadRequest(model.getLocationArn());
        DescribeLocationS3Response response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationS3Request, client::describeLocationS3);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        return ResourceModel.builder()
                .locationArn(model.getLocationArn())
                .locationUri(response.locationUri())
                .s3BucketArn(model.getS3BucketArn())
                .s3Config(model.getS3Config())
                .s3StorageClass(model.getS3StorageClass())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();
    }
}
