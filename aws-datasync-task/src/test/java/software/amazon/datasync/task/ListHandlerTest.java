package software.amazon.datasync.task;

import software.amazon.awssdk.services.datasync.model.ListTasksRequest;
import software.amazon.awssdk.services.datasync.model.ListTasksResponse;
import software.amazon.awssdk.services.datasync.model.TaskListEntry;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        final List<TaskListEntry> tasks = buildDefaultList();

        ListTasksResponse listTasksResponse = ListTasksResponse.builder()
                .tasks(tasks)
                .nextToken(null)
                .build();

        doReturn(listTasksResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                        any(ListTasksRequest.class),
                        any()
                );

        final ResourceModel model1 = buildDefaultModel1();
        final ResourceModel model2 = buildDefaultModel2();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).containsAll(Arrays.asList(model1, model2));
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    private static List<TaskListEntry> buildDefaultList() {
        final String taskArn1 = "arn:aws:datasync:us-east-2:123456789012:task/task-01234567890123456";
        final String taskArn2 = "arn:aws:datasync:us-east-2:123456789012:task/task-12345678901234567";
        TaskListEntry task1 = TaskListEntry.builder()
                .taskArn(taskArn1)
                .status("AVAILABLE")
                .name("Task1")
                .build();
        TaskListEntry task2 = TaskListEntry.builder()
                .taskArn(taskArn2)
                .status("AVAILABLE")
                .name("Task2")
                .build();
        return Arrays.asList(task1, task2);
    }

    private static ResourceModel buildDefaultModel1() {
        final String taskArn = "arn:aws:datasync:us-east-2:123456789012:task/task-01234567890123456";
        return ResourceModel.builder()
                .taskArn(taskArn)
                .status("AVAILABLE")
                .name("Task1")
                .build();
    }

    private static ResourceModel buildDefaultModel2() {
        final String taskArn = "arn:aws:datasync:us-east-2:123456789012:task/task-12345678901234567";
        return ResourceModel.builder()
                .taskArn(taskArn)
                .status("AVAILABLE")
                .name("Task2")
                .build();
    }
}
