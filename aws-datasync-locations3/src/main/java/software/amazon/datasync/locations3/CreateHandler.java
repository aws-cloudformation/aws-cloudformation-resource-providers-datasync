package software.amazon.datasync.locations3;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationS3Request;
import software.amazon.awssdk.services.datasync.model.CreateLocationS3Response;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Request;
import software.amazon.awssdk.services.datasync.model.DescribeLocationS3Response;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private static final String AWS_TAG_PREFIX = "aws:";

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

        Map<String, String> tagList = request.getDesiredResourceTags();
        if (tagList == null) {
            tagList = new HashMap<String, String>();
        }

        // Check for invalid requested system tags.
        for (String key : tagList.keySet()) {
            if (key.trim().toLowerCase().startsWith(AWS_TAG_PREFIX)) {
                throw new CfnInvalidRequestException(key + " is an invalid key. aws: prefixed tag key names cannot be requested.");
            }
        }

        //  Retrieve default stack-level tags with aws:cloudformation prefix.
        Map<String, String> systemTagList = request.getSystemTags();
        if (systemTagList != null) {
            tagList.putAll(systemTagList);
        }

        CreateLocationS3Request createLocationS3Request = Translator.translateToCreateRequest(model, tagList);

        CreateLocationS3Response response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationS3Request, client::createLocationS3);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        final ResourceModel modelNoUri = ResourceModel.builder()
                .locationArn(response.locationArn())
                .s3BucketArn(model.getS3BucketArn())
                .s3Config(model.getS3Config())
                .s3StorageClass(model.getS3StorageClass())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        ResourceHandlerRequest<ResourceModel> requestWithArn = request.toBuilder()
                .desiredResourceState(modelNoUri)
                .build();

        return new ReadHandler().handleRequest(proxy, requestWithArn, callbackContext, logger);
    }

}
