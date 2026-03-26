package sk.iway.iwcm.doc;

import sk.iway.iwcm.Logger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Konvertuje databazovy typ int groupId na GroupDetails objekt a naopak, priklad pouzitia:
 *
 * @Column(name = "group_id")
 * @Convert(converter = GroupDetailsConverter.class)
 * private GroupDetails parentGroup;
 *
 * v databaze sa ulozi hodnota groupId ale na FE bude dostupny GroupDetails objekt
 */
@Converter
public class GroupDetailsConverter implements AttributeConverter<GroupDetails, Integer> {

    @Override
    public Integer convertToDatabaseColumn(GroupDetails group) {
        //podmienka >0 je aby fungovala anotacia @NotNull na entite
        if (group != null && group.getGroupId()>0) return Integer.valueOf(group.getGroupId());
        return null;
    }

    @Override
    public GroupDetails convertToEntityAttribute(Integer groupId) {
        if (groupId != null) {
            GroupsDB groupsDB = GroupsDB.getInstance();
            GroupDetails group = groupsDB.getGroup(groupId.intValue());
            if (group != null) {
                try {
                    GroupDetails cloned = (GroupDetails)group.clone();
                    return cloned;
                } catch (CloneNotSupportedException e) {
                    Logger.error(GroupDetailsConverter.class, e);
                }
            }
        }
        return null;
    }

}
