package software.amazon.datasync.locationfsxwindows;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationFsxWindowsResponse;
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

        final DescribeLocationFsxWindowsRequest describeLocationFsxWindowsRequest =
                Translator.translateToReadRequest(model.getLocationArn());

        DescribeLocationFsxWindowsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationFsxWindowsRequest, client::describeLocationFsxWindows);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getLocationArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .domain(response.domain())
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .securityGroupArns(response.securityGroupArns())
                .subdirectory(model.getSubdirectory())
                .user(response.user())
                .tags(model.getTags())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(returnModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
