package software.amazon.datasync.locationhdfs;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DescribeLocationHdfsRequest;
import software.amazon.awssdk.services.datasync.model.DescribeLocationHdfsResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
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

        final DescribeLocationHdfsRequest describeLocationHdfsRequest =
                Translator.translateToReadRequest(model.getLocationArn());

        DescribeLocationHdfsResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeLocationHdfsRequest, client::describeLocationHdfs);
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
                .filter(tag -> !tag.getKey().startsWith(AWS_CFN_TAG_PREFIX))
                .collect(Collectors.toSet());

        ResourceModel returnModel = ResourceModel.builder()
                .locationArn(response.locationArn())
                .locationUri(response.locationUri())
                .agentArns(response.agentArns())
                .blockSize(response.blockSize() != null ? response.blockSize().longValue() : null)
                .replicationFactor(response.replicationFactor() != null ? response.replicationFactor().longValue() : null)
                .authenticationType(response.authenticationTypeAsString())
                .nameNodes(Translator.translateToResourceModelNameNodes(response.nameNodes()))
                .simpleUser(response.simpleUser())
                .kmsKeyProviderUri(response.kmsKeyProviderUri())
                .kerberosPrincipal(response.kerberosPrincipal())
                .qopConfiguration(Translator.translateToResourceModelQopConfiguration(response.qopConfiguration()))
                .tags(userTags)
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(returnModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
