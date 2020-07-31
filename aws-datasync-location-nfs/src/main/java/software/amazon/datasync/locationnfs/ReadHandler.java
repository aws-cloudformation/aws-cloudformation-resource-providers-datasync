package software.amazon.datasync.locationnfs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationNfsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationNfsResponse;
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

        final DescribeLocationNfsRequest describeLocationNfsRequest =
                Translator.translateToReadRequest(model.getLocationArn());

        DescribeLocationNfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationNfsRequest, client::describeLocationNfs);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getLocationArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(describeLocationNfsRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(describeLocationNfsRequest.toString(), e.getCause());
        }


        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .mountOptions(Translator.translateToResourceModelMountOptions(response.mountOptions()))
                .onPremConfig(Translator.translateToResourceModelOnPremConfig(response.onPremConfig()))
                .subdirectory(model.getSubdirectory())
                .serverHostname(model.getServerHostname())
                .tags(model.getTags())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(returnModel)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
