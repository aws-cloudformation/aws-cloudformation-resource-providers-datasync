package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DeleteHandler extends BaseHandlerStd {

    /*
    Once an Agent resource is deleted, the user's deployed agent begins deactivation.
    It may take several minutes until a deactivated agent is ready for re-activation.
    By adding stabilization delay, we give the agent a chance to reboot and be ready for
    subsequent activation. Deletion-then-creation is common in CloudFormation stacks,
    and providing stabilization delay prevents failed stacks by giving deployed agents
    time to deactivate and reboot.
     */
    private static final Constant STABILIZATION_DELAY = Constant.of()
            .timeout(Duration.ofDays(365L))  // Nonsense value; we already enforce timeout in the resource schema
            .delay(Duration.ofMinutes(3L))   // How long to delay to allow the host to stabilize
            .build();

    private Logger logger;

    private Delay delay;

    public DeleteHandler() {
        super();
        this.delay = STABILIZATION_DELAY;
    }

    public DeleteHandler(Delay delay) {
        super();
        this.delay = delay;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DataSyncClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-DataSync-Agent::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteAgent)
                                .progress())
                .then(progress ->
                        proxy.initiate("AWS-DataSync-Agent::PostDeleteStabilize", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Function.identity())
                                .backoffDelay(delay)
                                .makeServiceCall(EMPTY_CALL)
                                .stabilize((req, resp, prox, mod, ctx) -> isStabilized(ctx))
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private static final BiFunction<ResourceModel, ProxyClient<DataSyncClient>, ResourceModel> EMPTY_CALL =
            (model, proxyClient) -> model;

    private DeleteAgentResponse deleteAgent(
            final DeleteAgentRequest deleteAgentRequest,
            final ProxyClient<DataSyncClient> proxyClient) {
        DeleteAgentResponse deleteAgentResponse;
        try {
            deleteAgentResponse = proxyClient.injectCredentialsAndInvokeV2(
                    deleteAgentRequest,
                    proxyClient.client()::deleteAgent);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, deleteAgentRequest.agentArn(), e);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return deleteAgentResponse;
    }

    private boolean isStabilized(final CallbackContext callbackContext) {
        if (callbackContext.isDeleteStabilizationStarted()) {
            return true;
        } else {
            callbackContext.setDeleteStabilizationStarted(true);
            return false;
        }
    }

}
