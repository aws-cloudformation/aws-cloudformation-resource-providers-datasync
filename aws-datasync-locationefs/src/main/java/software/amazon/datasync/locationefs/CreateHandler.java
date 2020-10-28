package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.CreateLocationEfsResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationEfsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationEfsResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        if (callbackContext == null && (request.getDesiredResourceState().getLocationArn() != null)) {
            throw new CfnInvalidRequestException("LocationArn cannot be specified to create a location.");
        }

        // In order to include stack-level tags, they must be retrieved separately from the model
        Map<String, String> tagList = request.getDesiredResourceTags();
        if (tagList == null) {
            tagList = new HashMap<String, String>();
        }

        CreateLocationEfsRequest createLocationEfsRequest = Translator.translateToCreateRequest(model, tagList);

        CreateLocationEfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationEfsRequest, client::createLocationEfs);
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
                .eC2Config(model.getEC2Config())
                .efsFilesystemArn(model.getEfsFilesystemArn())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        ResourceHandlerRequest<ResourceModel> requestWithArn = request.toBuilder()
                .desiredResourceState(modelNoUri)
                .build();

        return new ReadHandler().handleRequest(proxy, requestWithArn, callbackContext, logger);
    }

}
