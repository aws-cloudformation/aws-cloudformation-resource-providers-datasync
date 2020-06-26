package software.amazon.datasync.agent;

import com.amazonaws.util.CollectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-agent.json");
    }

    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
<<<<<<< HEAD
        if (CollectionUtils.isNullOrEmpty(resourceModel.getTags()))
=======
        if(CollectionUtils.isNullOrEmpty(resourceModel.getTags()))
>>>>>>> 0f786d131e5b5f8eb01e8a57add2f5da65d32708
            return null;

        return resourceModel.getTags()
                .stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }
}
