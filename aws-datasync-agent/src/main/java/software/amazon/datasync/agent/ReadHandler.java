package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeAgentRequest;
import software.amazon.awssdk.services.datasync.model.DescribeAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.datasync.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.*;

import java.util.HashSet;
import java.util.Set;


public class ReadHandler extends BaseHandlerStd {

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
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
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
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        Set<Tag> tags = new HashSet<Tag>();
        if (tagsResponse.tags() != null) {
            tags = Translator.translateTagListEntries(tagsResponse.tags());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .agentArn(response.agentArn())
                .agentName(response.name())
                .agentAddress(model.getAgentAddress())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .endpointType(model.getEndpointType())
                .tags(tags)
                .build();

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

}
