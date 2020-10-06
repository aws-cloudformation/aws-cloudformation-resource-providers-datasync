package software.amazon.datasync.task;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.CreateTaskRequest;
import software.amazon.awssdk.services.datasync.model.CreateTaskResponse;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.DescribeTaskRequest;
import software.amazon.awssdk.services.datasync.model.DescribeTaskResponse;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandler<CallbackContext> {

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

        CreateTaskRequest createTaskRequest = Translator.translateToCreateRequest(model);

        CreateTaskResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(createTaskRequest, client::createTask);
            logger.log(String.format("%s created successfully.", ResourceModel.TYPE_NAME));
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(createTaskRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(createTaskRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(createTaskRequest.toString(), e.getCause());
        }

        ResourceModel modelTagsAndArn = ResourceModel.builder()
                .taskArn(response.taskArn())
                .tags(model.getTags())
                .build();

        ResourceModel returnModel = retrieveUpdatedModel(modelTagsAndArn, proxy, client);

        return ProgressEvent.defaultSuccessHandler(returnModel);
    }


    private ResourceModel retrieveUpdatedModel(final ResourceModel model,
                                               final AmazonWebServicesClientProxy proxy,
                                               final DataSyncClient client) {

        DescribeTaskRequest describeTaskRequest = Translator.translateToReadRequest(model.getTaskArn());
        DescribeTaskResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(describeTaskRequest, client::describeTask);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getCause());
        }

        return ResourceModel.builder()
                .cloudWatchLogGroupArn(response.cloudWatchLogGroupArn())
                .taskArn(response.taskArn())
                .destinationLocationArn(response.destinationLocationArn())
                .errorCode(response.errorCode())
                .errorDetail(response.errorDetail())
                .status(response.statusAsString())
                .excludes(Translator.translateToResourceModelExcludes(response.excludes()))
                .name(response.name())
                .options(Translator.translateToResourceModelOptions(response.options()))
                .schedule(Translator.translateToResourceModelTaskSchedule(response.schedule()))
                .sourceLocationArn(response.sourceLocationArn())
                .tags(model.getTags())
                .build();
    }
}
