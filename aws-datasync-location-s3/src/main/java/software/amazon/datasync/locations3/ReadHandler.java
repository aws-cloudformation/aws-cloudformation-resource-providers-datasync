package software.amazon.datasync.locations3;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Request;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Response;
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

        final DescribeLocationS3Request describeLocationS3Request = Translator.translateToReadRequest(model.getLocationArn());

        DescribeLocationS3Response response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationS3Request, client::describeLocationS3);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getLocationArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(describeLocationS3Request.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(describeLocationS3Request.toString(), e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .s3BucketArn(model.getS3BucketArn())
                .s3Config(model.getS3Config())
                .s3StorageClass(response.s3StorageClassAsString())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(returnModel)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
