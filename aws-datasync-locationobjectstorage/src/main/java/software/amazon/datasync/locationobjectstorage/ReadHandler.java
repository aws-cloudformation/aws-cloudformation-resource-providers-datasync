package software.amazon.datasync.locationobjectstorage;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationObjectStorageResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Set;
import java.util.stream.Collectors;

public class ReadHandler extends BaseHandler<CallbackContext> {
    private static final String AWS_CFN_TAG_PREFIX = "aws:cloudformation:";

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
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        // Current tags are not supplied by the Describe call and must be retrieved separately
        final Set<Tag> allTags = TagRequestMaker.listTagsForResource(proxy, client, model.getLocationArn());
        final Set<Tag> userTags = allTags.stream()
                .filter(tag -> !tag.getKey().startsWith(AWS_CFN_TAG_PREFIX)) // Filter our system tags on the user tags
                .collect(Collectors.toSet());

        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .accessKey(response.accessKey())
                .agentArns(response.agentArns())
                .serverPort(response.serverPort())
                .serverProtocol(response.serverProtocolAsString())
                .tags(userTags)
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(returnModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
