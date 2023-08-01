package software.amazon.datasync.storagesystem;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemRequest;
import software.amazon.awssdk.services.datasync.model.DescribeStorageSystemResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
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

        final DescribeStorageSystemRequest describeStorageSystemRequest =
                Translator.translateToReadRequest(model.getStorageSystemArn());

        DescribeStorageSystemResponse describeStorageSystemResponse;
        try {
            describeStorageSystemResponse =
                    proxy.injectCredentialsAndInvokeV2(describeStorageSystemRequest,client::describeStorageSystem);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getStorageSystemArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        // Current tags are not supplied by the Describe call and must be retrieved separately
        final Set<Tag> allTags = TagRequestMaker.listTagsForResource(proxy, client, model.getStorageSystemArn());
        final Set<Tag> userTags = allTags.stream()
                .filter(tag -> !tag.getKey().startsWith(AWS_CFN_TAG_PREFIX)) // Filter our system tags on the user tags
                .collect(Collectors.toSet());

        final ResourceModel returnModel = ResourceModel.builder()
                .storageSystemArn(describeStorageSystemResponse.storageSystemArn())
                .serverConfiguration(Translator.translateToResourceModelServerConfiguration(describeStorageSystemResponse.serverConfiguration()))
                .secretsManagerArn(describeStorageSystemResponse.secretsManagerArn())
                .systemType(describeStorageSystemResponse.systemType().toString())
                .agentArns(describeStorageSystemResponse.agentArns())
                .name(describeStorageSystemResponse.name())
                .connectivityStatus(describeStorageSystemResponse.connectivityStatus().toString())
                .cloudWatchLogGroupArn(describeStorageSystemResponse.cloudWatchLogGroupArn())
                .tags(userTags)
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(returnModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}