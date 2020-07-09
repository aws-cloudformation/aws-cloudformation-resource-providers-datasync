package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.*;
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

        final DescribeLocationEfsRequest describeLocationEfsRequest = Translator
                .translateToReadRequest(model.getLocationArn());

        DescribeLocationEfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationEfsRequest, client::describeLocationEfs);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getLocationArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .eC2Config(model.getEC2Config())
                .efsFilesystemArn(model.getEfsFilesystemArn())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(returnModel)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
