package software.amazon.datasync.task;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateTaskRequest;
import software.amazon.awssdk.services.datasync.model.CreateTaskResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private static final String AWS_TAG_PREFIX = "aws:";

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        if (callbackContext == null && (request.getDesiredResourceState().getTaskArn() != null)) {
            throw new CfnInvalidRequestException("TaskArn cannot be specified to create a task.");
        }

        final ResourceModel model = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        Map<String, String> tagList = request.getDesiredResourceTags();
        if (tagList == null) {
            tagList = new HashMap<String, String>();
        }

        // Check for invalid requested system tags.
        for (String key : tagList.keySet()) {
            if (key.trim().toLowerCase().startsWith(AWS_TAG_PREFIX)) {
                throw new CfnInvalidRequestException("LocationArn cannot be specified to create a location.");
            }
        }

        //  Retrieve default stack-level tags with aws:cloudformation prefix.
        Map<String, String> systemTagList = request.getSystemTags();
        if (systemTagList != null) {
            tagList.putAll(systemTagList);
        }

        CreateTaskRequest createTaskRequest = Translator.translateToCreateRequest(model, tagList);

        CreateTaskResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createTaskRequest, client::createTask);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        ResourceModel modelWithArn = ResourceModel.builder()
                .taskArn(response.taskArn())
                .build();
        ResourceHandlerRequest<ResourceModel> requestWithArn = request.toBuilder()
                .desiredResourceState(modelWithArn)
                .build();

        return new ReadHandler().handleRequest(proxy, requestWithArn, callbackContext, logger);
    }

}
