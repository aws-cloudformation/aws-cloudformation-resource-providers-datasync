package software.amazon.datasync.agent;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateAgentRequest;
import software.amazon.awssdk.services.datasync.model.CreateAgentResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointsResponse;
import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.IOException;
import java.util.Scanner;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        // If an Activation Key is NOT input, then use the input Agent Address to retrieve one and add it to the model:
        if (model.getActivationKey() == null) {
            try {
                String activationKey = obtainCorrectActivationKey(model, proxy);
                model.setActivationKey(activationKey);
            } catch (IOException e) {
                return ProgressEvent.defaultFailureHandler(e, null);
            }
        }
        // If the Activation Key is null again, then the HTTP GET failed:
        if (model.getActivationKey() == null)
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .errorCode(HandlerErrorCode.InternalFailure)
                    .status(OperationStatus.FAILED)
                    .build();

        CreateAgentRequest createAgentRequest = Translator.translateToCreateRequest(model);
        CreateAgentResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createAgentRequest, client::createAgent);
            logger.log(String.format("%s created successfully", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createAgentRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(createAgentRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(createAgentRequest.toString(), e.getCause());
        }

        ResourceModel returnModel = ResourceModel.builder()
                .agentArn(response.agentArn())
                .agentName(model.getAgentName())
                .agentAddress(model.getAgentAddress())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .tags(model.getTags())
                .build();

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    public String obtainCorrectActivationKey(ResourceModel model,
                                             AmazonWebServicesClientProxy proxy) throws IOException {
        // If there is no VPC endpoint ID, obtain Activation Key for Agent w/ Public service endpoints
        if (model.getVpcEndpointId() == null) {
            return getActivationKeyPublicEndpoint(model.getAgentAddress());
        }
        // Otherwise obtain an Activation Key for the Agent w/ Vpc Endpoints
        return getActivationKeyVpcEndpoint(model.getAgentAddress(), model.getVpcEndpointId(), proxy);
    }

    /**
     * Gets an Activation Key given the desired Agent Address
     * Assumes that the Address is reachable on Port 80
     * @throws IOException if HTTP GET execution fails and leads to a resource creation failure
     */
    public String getActivationKeyPublicEndpoint(String IpAddress) throws IOException {
        // Enforce 15 second timeout:
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(15 * 1000)
                .setConnectionRequestTimeout(15 * 1000)
                .setSocketTimeout(15 * 1000)
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        HttpGet httpGet = new HttpGet("http://" + IpAddress + "/?gatewayType=SYNC&activationRegion=us-west-2&no_redirect");
        CloseableHttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (Exception e) {
            return null;
        }
        Scanner sc = new Scanner(httpResponse.getEntity().getContent(), "UTF-8");
        return sc.nextLine();
    }

    /**
     * Gets an Activation Key given the desired Agent Address
     * Assumes that the Address is reachable on Port 80
     * Assumes that the Vpc Endpoint is available
     * @throws IOException if HTTP GET execution fails and leads to a resource creation failure
     */
    private String getActivationKeyVpcEndpoint(String IpAddress,
                                               String vpcEndpointId,
                                               AmazonWebServicesClientProxy proxy) throws IOException {
        Ec2Client ec2Client = Ec2Client.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
        // Obtain the Vpc Endpoint Elastic Ip Address:
        DescribeVpcEndpointsRequest describeVpcEndpointsRequest = DescribeVpcEndpointsRequest.builder()
                .vpcEndpointIds(vpcEndpointId)
                .build();
        DescribeVpcEndpointsResponse describeVpcEndpointsResponse =
                proxy.injectCredentialsAndInvokeV2(describeVpcEndpointsRequest, ec2Client::describeVpcEndpoints);
        String networkInterfaceId = describeVpcEndpointsResponse
                .vpcEndpoints()
                .get(0)
                .networkInterfaceIds()
                .get(0);
        DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest.builder()
                .networkInterfaceIds(networkInterfaceId)
                .build();
        DescribeNetworkInterfacesResponse describeNetworkInterfacesResponse =
                proxy.injectCredentialsAndInvokeV2(describeNetworkInterfacesRequest, ec2Client::describeNetworkInterfaces);
        String elasticIpAddress = describeNetworkInterfacesResponse
                .networkInterfaces()
                .get(0)
                .privateIpAddress();
        final String url = "http://" + IpAddress +
                "/?gatewayType=SYNC&activationRegion=us-west-2&privateLinkEndpoint="
                + elasticIpAddress + "&endpointType=PRIVATE_LINK&no_redirect";
        // Make HTTP GET Request; enforce 15 second timeout
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(15 * 1000)
                .setConnectionRequestTimeout(15 * 1000)
                .setSocketTimeout(15 * 1000)
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        HttpGet httpGet = new HttpGet(url);

        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (Exception e) {
            return null;
        }
        Scanner sc = new Scanner(httpResponse.getEntity().getContent(), "UTF-8");
        return sc.nextLine();
    }
}