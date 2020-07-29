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
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        final String region = request.getRegion();

        // Assert that both Activation Key and Agent Address are not given together:
        if (model.getActivationKey() != null && model.getAgentAddress() != null)
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .errorCode(HandlerErrorCode.InvalidRequest)
                    .status(OperationStatus.FAILED)
                    .build();

        // If an Activation Key is NOT input, then use the input Agent Address to retrieve one and add it to the model:
        if (model.getActivationKey() == null) {
            try {
                String activationKey = obtainCorrectActivationKey(model, proxy, region);
                model.setActivationKey(activationKey);
            } catch (IOException e) {
                throw new CfnInternalFailureException(e.getCause());
            }
        }
        // If the Activation Key is null, this indicates an invalid input
        if (model.getActivationKey() == null)
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .errorCode(HandlerErrorCode.InvalidRequest)
                    .status(OperationStatus.FAILED)
                    .build();

        // If the Activation Key is empty, we had a problem with the HTTP GET
        if (model.getActivationKey().equals(""))
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
                .endpointType(model.getEndpointType())
                .tags(model.getTags())
                .build();

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }

    String obtainCorrectActivationKey(ResourceModel model,
                                             AmazonWebServicesClientProxy proxy,
                                             String region) throws IOException {
        if (model.getEndpointType() == null || model.getAgentAddress() == null)
            return null; // will indicate invalid input
        switch (model.getEndpointType()) {
            case "FIPS":
                return getActivationKeyFipsEndpoint(model.getAgentAddress(), region);
            case "PRIVATE_LINK":
                return getActivationKeyVpcEndpoint(model.getAgentAddress(), model.getVpcEndpointId(),
                        model.getSubnetArns().get(0), proxy, region);
            case "PUBLIC":
                return getActivationKeyPublicEndpoint(model.getAgentAddress(), region);
            default: // Also return null in the case of invalid endpoint type
                return null;
        }
    }

    /**
     * Gets an Activation Key given the desired Agent Address for a Public Endpoint
     * Assumes that the Address is reachable on Port 80
     * @throws IOException if HTTP GET execution fails and leads to a resource creation failure
     */
    private String getActivationKeyPublicEndpoint(String IpAddress, String region) throws IOException {
        CloseableHttpClient httpClient = generateHttpClient();
        HttpGet httpGet = new HttpGet("http://" + IpAddress + "/?gatewayType=SYNC&activationRegion=" + region + "&no_redirect");
        CloseableHttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (Exception e) {
            throw new CfnInternalFailureException(e.getCause());
        }
        Scanner sc = new Scanner(httpResponse.getEntity().getContent(), "UTF-8");
        return sc.nextLine();
    }

    /**
     * Gets an Activation Key given the desired Agent Address for a FIPS Endpoint
     * Assumes that the Address is reachable on Port 80
     * @throws IOException if HTTP GET execution fails and leads to a resource creation failure
     */
    private String getActivationKeyFipsEndpoint(String IpAddress, String region) throws IOException {
        CloseableHttpClient httpClient = generateHttpClient();
        HttpGet httpGet = new HttpGet("http://" + IpAddress + "/?gatewayType=SYNC&activationRegion=" + region + "&endpointType=FIPS&no_redirect");
        CloseableHttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (Exception e) {
            throw new CfnInternalFailureException(e.getCause());
        }
        Scanner sc = new Scanner(httpResponse.getEntity().getContent(), "UTF-8");
        return sc.nextLine();
    }

    /**
     * Gets an Activation Key given the desired Agent Address for VPC endpoints using Private Link
     * Assumes that the Address is reachable on Port 80
     * Assumes that the Vpc Endpoint is available
     * @throws IOException if HTTP GET execution fails and leads to a resource creation failure
     */
    private String getActivationKeyVpcEndpoint(String IpAddress,
                                               String vpcEndpointId,
                                               String subnetArn,
                                               AmazonWebServicesClientProxy proxy,
                                               String region) throws IOException {
        // Initialize EC2 Client:
        Ec2Client ec2Client = Ec2Client.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

        // Get the Network Interface IDs associated with the VPC Endpoint
        DescribeVpcEndpointsRequest describeVpcEndpointsRequest = DescribeVpcEndpointsRequest.builder()
                .vpcEndpointIds(vpcEndpointId)
                .build();
        DescribeVpcEndpointsResponse describeVpcEndpointsResponse =
                proxy.injectCredentialsAndInvokeV2(describeVpcEndpointsRequest, ec2Client::describeVpcEndpoints);
        List<String> endpointNetworkInterfaceIds = describeVpcEndpointsResponse
                .vpcEndpoints()
                .get(0)
                .networkInterfaceIds();

        // Get the Network Interfaces associated with the Subnet:
        String subnetId = getSubnetId(subnetArn, proxy, ec2Client);
        Filter subnetFilter = Filter.builder()
                .name("subnet-id")
                .values(subnetId)
                .build();
        DescribeNetworkInterfacesRequest describeSubnetNetworkInterfacesRequest = DescribeNetworkInterfacesRequest.builder()
                .filters(subnetFilter)
                .build();
        DescribeNetworkInterfacesResponse describeSubnetNetworkInterfacesResponse =
                proxy.injectCredentialsAndInvokeV2(describeSubnetNetworkInterfacesRequest, ec2Client::describeNetworkInterfaces);
        List<String> subnetNetworkInterfaceIds =
                getNetworkInterfaceIds(describeSubnetNetworkInterfacesResponse.networkInterfaces());

        // Find the network interface which exists in the subnet:
        String desiredNetworkInterfaceId = "";
        for (String endpointNetworkInterfaceId : endpointNetworkInterfaceIds) {
            // If the Endpoint Network Interface ID matches the one in the subnet, that is our desired ID
            if (subnetNetworkInterfaceIds.contains(endpointNetworkInterfaceId)) {
                desiredNetworkInterfaceId = endpointNetworkInterfaceId;
                break;
            }
        }
        if (desiredNetworkInterfaceId.equals("")) // If ID doesn't exist, the subnet given is incorrect, so return null and fail with Invalid Request
            return null;

        // Finally, get the Elastic IP Address of the obtained Network Interface:
        DescribeNetworkInterfacesRequest describeDesiredNetworkInterfaceRequest = DescribeNetworkInterfacesRequest
                .builder()
                .networkInterfaceIds(desiredNetworkInterfaceId)
                .build();
        DescribeNetworkInterfacesResponse describeDesiredNetworkInterfaceResponse =
                proxy.injectCredentialsAndInvokeV2(describeDesiredNetworkInterfaceRequest, ec2Client::describeNetworkInterfaces);
        String elasticIpAddress = describeDesiredNetworkInterfaceResponse
                .networkInterfaces()
                .get(0)
                .privateIpAddress();

        // Make the HTTP GET request:
        final String url = "http://" + IpAddress +
                "/?gatewayType=SYNC&activationRegion=" + region + "&privateLinkEndpoint="
                + elasticIpAddress + "&endpointType=PRIVATE_LINK&no_redirect";
        CloseableHttpClient httpClient = generateHttpClient();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (Exception e) {
            throw new CfnInternalFailureException(e.getCause());
        }
        Scanner sc = new Scanner(httpResponse.getEntity().getContent(), "UTF-8");
        return sc.nextLine();
    }

    private CloseableHttpClient generateHttpClient() {
        // Enforce 15 second timeout:
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(15 * 1000)
                .setConnectionRequestTimeout(15 * 1000)
                .setSocketTimeout(15 * 1000)
                .build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    private String getSubnetId(String subnetArn, AmazonWebServicesClientProxy proxy, Ec2Client ec2Client) {
        Filter subnetFilter = Filter.builder()
                .name("subnet-arn")
                .values(subnetArn)
                .build();
        DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder()
                .filters(subnetFilter)
                .build();
        DescribeSubnetsResponse describeSubnetsResponse =
                proxy.injectCredentialsAndInvokeV2(describeSubnetsRequest, ec2Client::describeSubnets);
        return describeSubnetsResponse.subnets().get(0).subnetId();
    }

    private List<String> getNetworkInterfaceIds(List<NetworkInterface> networkInterfaces) {
        List<String> networkInterfaceIds = new ArrayList<>();
        for (NetworkInterface networkInterface : networkInterfaces)
            networkInterfaceIds.add(networkInterface.networkInterfaceId());
        return networkInterfaceIds;
    }
}