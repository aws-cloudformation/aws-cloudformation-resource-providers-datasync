package software.amazon.datasync.locationfsxwindows;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.CreateLocationFsxWindowsResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationFsxWindowsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationFsxWindowsResponse;
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

        if (callbackContext == null && (request.getDesiredResourceState().getLocationArn() != null)) {
            throw new CfnInvalidRequestException("LocationArn cannot be specified to create a location.");
        }

        CreateLocationFsxWindowsRequest createLocationFsxWindowsRequest =
                Translator.translateToCreateRequest(model);

        CreateLocationFsxWindowsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationFsxWindowsRequest, client::createLocationFsxWindows);
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        final ResourceModel modelNoUri = ResourceModel.builder()
                .locationArn(response.locationArn())
                .domain(model.getDomain())
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .securityGroupArns(model.getSecurityGroupArns())
                .subdirectory(model.getSubdirectory())
                .user(model.getUser())
                .tags(model.getTags())
                .build();

        ResourceModel returnModel = retrieveUpdatedModel(modelNoUri, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private static ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                                      final AmazonWebServicesClientProxy proxy,
                                                      final DataSyncClient client) {

        DescribeLocationFsxWindowsRequest describeLocationFsxWindowsRequest =
                Translator.translateToReadRequest(model.getLocationArn());
        DescribeLocationFsxWindowsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationFsxWindowsRequest, client::describeLocationFsxWindows);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        return ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .domain(response.domain())
                .fsxFilesystemArn(model.getFsxFilesystemArn())
                .securityGroupArns(response.securityGroupArns())
                .subdirectory(model.getSubdirectory())
                .user(response.user())
                .tags(model.getTags())
                .build();

    }
}
