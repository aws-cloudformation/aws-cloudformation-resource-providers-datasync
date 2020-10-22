package software.amazon.datasync.locationfsxwindows;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-locationfsxwindows.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return TagTranslator.translateTagsToMap(resourceModel.getTags());
    }

}
