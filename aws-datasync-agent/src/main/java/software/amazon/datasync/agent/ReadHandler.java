package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeAgentRequest;
import software.amazon.awssdk.services.datasync.model.DescribeAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class ReadHandler extends BaseHandlerStd {
    private static final String AWS_CFN_TAG_PREFIX = "aws:cloudformation:";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DataSyncClient> proxyClient,
            final Logger logger) {

        final DataSyncClient client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();

        final DescribeAgentRequest describeAgentRequest = Translator.translateToReadRequest(model);

        DescribeAgentResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeAgentRequest, client::describeAgent);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getAgentArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        // Since tags are not returned by the DescribeAgent call but can be modified,
        // we must separately retrieve and return them to ensure we return an up-to-date model.
        final ListTagsForResourceRequest listTagsForResourceRequest = Translator.translateToListTagsRequest(model);

        ListTagsForResourceResponse tagsResponse;
        try {
            tagsResponse = proxy.injectCredentialsAndInvokeV2(listTagsForResourceRequest, client::listTagsForResource);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getAgentArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        Set<Tag> allTags = new HashSet<Tag>();
        if (tagsResponse.tags() != null) {
            allTags = Translator.translateTagListEntries(tagsResponse.tags());
        }
        final Set<Tag> userTags = allTags.stream()
                .filter(tag -> !tag.getKey().startsWith(AWS_CFN_TAG_PREFIX))
                .collect(Collectors.toSet());

        ResourceModel returnModel = ResourceModel.builder()
                .agentArn(response.agentArn())
                .agentName(response.name())
                .securityGroupArns(response.privateLinkConfig() == null ? null : response.privateLinkConfig().securityGroupArns())
                .subnetArns(response.privateLinkConfig() == null ? null : response.privateLinkConfig().subnetArns())
                .vpcEndpointId(response.privateLinkConfig() == null ? null : response.privateLinkConfig().vpcEndpointId())
                .endpointType(response.endpointType() == null ? null : response.endpointType().toString())
                .tags(userTags)
                .build();

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

}
