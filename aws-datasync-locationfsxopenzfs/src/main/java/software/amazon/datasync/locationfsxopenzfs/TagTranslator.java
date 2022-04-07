package software.amazon.datasync.locationfsxopenzfs;

import software.amazon.awssdk.services.datasync.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;
import software.amazon.awssdk.services.datasync.model.TagResourceRequest;
import software.amazon.awssdk.services.datasync.model.UntagResourceRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TagTranslator {

    public static ListTagsForResourceRequest translateToListTagsRequest(final String resourceArn) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .build();
    }

    public static TagResourceRequest translateToTagResourceRequest(final Set<Tag> tagsToAdd, final String resourceArn) {
        return TagResourceRequest.builder()
                .resourceArn(resourceArn)
                .tags(translateTags(tagsToAdd))
                .build();
    }

    public static UntagResourceRequest translateToUntagResourceRequest(final Set<String> tagsToRemove, final String resourceArn) {
        return UntagResourceRequest.builder()
                .resourceArn(resourceArn)
                .keys(tagsToRemove)
                .build();
    }

    static Set<TagListEntry> translateTags(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> TagListEntry.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    static Set<Tag> translateTagListEntries(final List<TagListEntry> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toSet());
    }

    static Map<String, String> translateTagsToMap(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptyMap();
        return tags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }

    static Set<TagListEntry> translateMapToTagListEntries(final Map<String, String> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.entrySet().stream().map(entry -> {
            return TagListEntry.builder().key(entry.getKey()).value(entry.getValue()).build();
        }).collect(Collectors.toSet());
    }

    static Set<Tag> translateMapToTags(final Map<String, String> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.entrySet().stream().map(entry -> {
            return Tag.builder().key(entry.getKey()).value(entry.getValue()).build();
        }).collect(Collectors.toSet());
    }

}
