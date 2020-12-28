package software.amazon.datasync.task;
import software.amazon.awssdk.services.datasync.model.CreateTaskRequest;
import software.amazon.awssdk.services.datasync.model.DeleteTaskRequest;
import software.amazon.awssdk.services.datasync.model.DescribeTaskRequest;
import software.amazon.awssdk.services.datasync.model.ListTasksRequest;
import software.amazon.awssdk.services.datasync.model.UpdateTaskRequest;

import java.util.*;
import java.util.stream.Collectors;

public class Translator {

    Translator() {}

    public static CreateTaskRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> tags) {
        // Schedule is not required for the customer, but this causes a null exception is it gets input as blank
        // Therefore, create a blank Task Schedule
        if (model.getSchedule() == null)
            model.setSchedule(TaskSchedule.builder().scheduleExpression("").build());
        return CreateTaskRequest.builder()
                .cloudWatchLogGroupArn(model.getCloudWatchLogGroupArn())
                .destinationLocationArn(model.getDestinationLocationArn())
                .excludes(translateToDataSyncExcludes(model.getExcludes()))
                .name(model.getName())
                .options(translateToDataSyncOptions(model.getOptions()))
                .schedule(translateToDataSyncTaskSchedule(model.getSchedule()))
                .sourceLocationArn(model.getSourceLocationArn())
                .tags(TagTranslator.translateMapToTagListEntries(tags))
                .build();
    }

    public static DeleteTaskRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteTaskRequest.builder()
                .taskArn(model.getTaskArn())
                .build();
    }

    public static ListTasksRequest translateToListRequest(final String nextToken) {
        return ListTasksRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    public static DescribeTaskRequest translateToReadRequest(final String taskArn) {
        return DescribeTaskRequest.builder()
                .taskArn(taskArn)
                .build();
    }

    public static UpdateTaskRequest translateToUpdateRequest(final ResourceModel model) {
        // Create a blank Task Schedule if it doesn't exist
        if (model.getSchedule() == null)
            model.setSchedule(TaskSchedule.builder().scheduleExpression("").build());
        return UpdateTaskRequest.builder()
                .cloudWatchLogGroupArn(model.getCloudWatchLogGroupArn())
                .excludes(translateToDataSyncExcludes(model.getExcludes()))
                .name(model.getName())
                .options(translateToDataSyncOptions(model.getOptions()))
                .schedule(translateToDataSyncTaskSchedule(model.getSchedule()))
                .taskArn(model.getTaskArn())
                .build();
    }

    public static List<FilterRule> translateToResourceModelExcludes(
            final List<software.amazon.awssdk.services.datasync.model.FilterRule> filterRules) {
        if (filterRules == null)
            return Collections.emptyList();
        Set<FilterRule> excludesAsSet = filterRules.stream()
                .map(filterRule -> software.amazon.datasync.task.FilterRule.builder()
                        .value(filterRule.value())
                        .filterType(filterRule.filterTypeAsString())
                        .build())
                .collect(Collectors.toSet());
        return new ArrayList<>(excludesAsSet);
    }

    private static Set<software.amazon.awssdk.services.datasync.model.FilterRule> translateToDataSyncExcludes(
            final List<software.amazon.datasync.task.FilterRule> filterRules) {
        if (filterRules == null)
            return Collections.emptySet();
        return filterRules.stream()
                .map(filterRule -> software.amazon.awssdk.services.datasync.model.FilterRule.builder()
                        .filterType(filterRule.getFilterType())
                        .value(filterRule.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    public static software.amazon.datasync.task.Options translateToResourceModelOptions(
            final software.amazon.awssdk.services.datasync.model.Options options) {
        if (options == null)
            return software.amazon.datasync.task.Options.builder().build();
        return software.amazon.datasync.task.Options.builder()
                .atime(options.atimeAsString())
                .bytesPerSecond(options.bytesPerSecond())
                .gid(options.gidAsString())
                .logLevel(options.logLevelAsString())
                .mtime(options.mtimeAsString())
                .overwriteMode(options.overwriteModeAsString())
                .posixPermissions(options.posixPermissionsAsString())
                .preserveDeletedFiles(options.preserveDeletedFilesAsString())
                .preserveDevices(options.preserveDevicesAsString())
                .taskQueueing(options.taskQueueingAsString())
                .uid(options.uidAsString())
                .verifyMode(options.verifyModeAsString())
                .build();

    }

    private static software.amazon.awssdk.services.datasync.model.Options translateToDataSyncOptions(
            final software.amazon.datasync.task.Options options) {
        if (options == null)
            return software.amazon.awssdk.services.datasync.model.Options.builder().build();
        return software.amazon.awssdk.services.datasync.model.Options.builder()
                .atime(options.getAtime())
                .bytesPerSecond(options.getBytesPerSecond())
                .gid(options.getGid())
                .logLevel(options.getLogLevel())
                .mtime(options.getMtime())
                .overwriteMode(options.getOverwriteMode())
                .posixPermissions(options.getPosixPermissions())
                .preserveDeletedFiles(options.getPreserveDeletedFiles())
                .preserveDevices(options.getPreserveDevices())
                .taskQueueing(options.getTaskQueueing())
                .uid(options.getUid())
                .verifyMode(options.getVerifyMode())
                .build();
    }

    public static software.amazon.datasync.task.TaskSchedule translateToResourceModelTaskSchedule(
            final software.amazon.awssdk.services.datasync.model.TaskSchedule schedule) {
        if (schedule == null)
            return software.amazon.datasync.task.TaskSchedule.builder().build();
        return software.amazon.datasync.task.TaskSchedule.builder()
                .scheduleExpression(schedule.scheduleExpression())
                .build();
    }

    private static software.amazon.awssdk.services.datasync.model.TaskSchedule translateToDataSyncTaskSchedule(
            final software.amazon.datasync.task.TaskSchedule schedule) {
        if (schedule == null)
            return software.amazon.awssdk.services.datasync.model.TaskSchedule.builder().build();
        return software.amazon.awssdk.services.datasync.model.TaskSchedule.builder()
                .scheduleExpression(schedule.getScheduleExpression())
                .build();
    }

}
