package software.amazon.datasync.locationsmb;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.CreateLocationSmbResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationSmbRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationSmbResponse;
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

        CreateLocationSmbRequest createLocationSmbRequest = Translator.translateToCreateRequest(model);

        CreateLocationSmbResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createLocationSmbRequest, client::createLocationSmb);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createLocationSmbRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(createLocationSmbRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(createLocationSmbRequest.toString(), e.getCause());
        }

        ResourceModel modelNoUri = ResourceModel.builder()
                .locationArn(response.locationArn())
                .agentArns(model.getAgentArns())
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .user(model.getUser())
                .build();

        final ResourceModel returnModel = retrieveUpdatedModel(modelNoUri, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    private static ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                                      final AmazonWebServicesClientProxy proxy,
                                                      final DataSyncClient client) {

        DescribeLocationSmbRequest describeLocationSmbRequest = Translator.translateToReadRequest(model.getLocationArn());

        DescribeLocationSmbResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationSmbRequest, client::describeLocationSmb);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        return ResourceModel.builder()
                .mountOptions(Translator.translateToResourceModelMountOptions(response.mountOptions()))
                .locationArn(response.locationArn())
                .domain(response.domain())
                .agentArns(model.getAgentArns())
                .serverHostname(model.getServerHostname())
                .subdirectory(model.getSubdirectory())
                .tags(model.getTags())
                .locationUri(response.locationUri())
                .user(model.getUser())
                .build();
    }
}
