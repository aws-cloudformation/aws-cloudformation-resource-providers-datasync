package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DescribeAgentRequest;
import software.amazon.awssdk.services.datasync.model.DescribeAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
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

        final DataSyncClient client = ClientBuilder.getClient();
        final ResourceModel model = request.getDesiredResourceState();

        final DescribeAgentRequest describeAgentRequest = Translator.translateToReadRequest(model);

        DescribeAgentResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeAgentRequest, client::describeAgent);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getAgentArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(describeAgentRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(describeAgentRequest.toString(), e.getCause());
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
                .tags(model.getTags())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(returnModel)
                .status(OperationStatus.SUCCESS)
                .build();
    }

}