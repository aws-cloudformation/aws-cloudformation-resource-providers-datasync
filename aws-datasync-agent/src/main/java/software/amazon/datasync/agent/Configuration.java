package software.amazon.datasync.agent;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-agent.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return Translator.translateTagsToMap(resourceModel.getTags());
    }
}
