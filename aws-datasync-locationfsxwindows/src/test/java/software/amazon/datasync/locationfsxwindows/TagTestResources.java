package software.amazon.datasync.locationfsxwindows;


import software.amazon.awssdk.services.datasync.model.ListTagsForResourceResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TagTestResources {

    final static Set<Tag> defaultTags = new HashSet<Tag>(Arrays.asList(
            Tag.builder().key("Constant").value("Should remain").build(),
            Tag.builder().key("Update").value("Should be updated").build(),
            Tag.builder().key("Delete").value("Should be deleted").build()
    ));

    final static Set<Tag> updatedTags = new HashSet<Tag>(Arrays.asList(
            Tag.builder().key("Constant").value("Should remain").build(),
            Tag.builder().key("Update").value("Has been updated").build(),
            Tag.builder().key("Add").value("Should be added").build()
    ));

    final static Set<Tag> TagsWithSystemTag = new HashSet<Tag>(Arrays.asList(
            Tag.builder().key("Constant").value("Should remain").build(),
            Tag.builder().key("Update").value("Should be updated").build(),
            Tag.builder().key("Delete").value("Should be deleted").build(),
            Tag.builder().key("aws:cloudformation:stackid").value("123").build()
    ));

    static ListTagsForResourceResponse buildDefaultTagsResponse() {
        return ListTagsForResourceResponse.builder()
                .tags(TagTranslator.translateTags(defaultTags))
                .build();
    }

    static ListTagsForResourceResponse buildUpdatedTagsResponse() {
        return ListTagsForResourceResponse.builder()
                .tags(TagTranslator.translateTags(updatedTags))
                .build();
    }

    static ListTagsForResourceResponse buildTagsWithSystemTagResponse() {
        return ListTagsForResourceResponse.builder()
                .tags(TagTranslator.translateTags(TagsWithSystemTag))
                .build();
    }
}
