package sk.iway.iwcm.components.perex_groups;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import sk.iway.iwcm.doc.PerexGroupBean;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-03-25T15:55:09+0100",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.41.0.z20250213-2037, environment: Java 21.0.6 (Eclipse Adoptium)"
)
public class PerexGroupBeanToPerexGroupsEntityMapperImpl implements PerexGroupBeanToPerexGroupsEntityMapper {

    @Override
    public PerexGroupsEntity perexGroupBeanToPerexGroupsEntity(PerexGroupBean perexGroupBean) {
        if ( perexGroupBean == null ) {
            return null;
        }

        PerexGroupsEntity perexGroupsEntity = new PerexGroupsEntity();

        perexGroupsEntity.setId( intToLong( perexGroupBean.getPerexGroupId() ) );
        perexGroupsEntity.setRelatedPages( tokensToString( perexGroupBean.getRelatedPages() ) );
        perexGroupsEntity.setAvailableGroups( perexGroupBean.getAvailableGroups() );
        perexGroupsEntity.setFieldA( perexGroupBean.getFieldA() );
        perexGroupsEntity.setFieldB( perexGroupBean.getFieldB() );
        perexGroupsEntity.setFieldC( perexGroupBean.getFieldC() );
        perexGroupsEntity.setFieldD( perexGroupBean.getFieldD() );
        perexGroupsEntity.setFieldE( perexGroupBean.getFieldE() );
        perexGroupsEntity.setFieldF( perexGroupBean.getFieldF() );
        perexGroupsEntity.setPerexGroupName( perexGroupBean.getPerexGroupName() );
        perexGroupsEntity.setPerexGroupNameCho( perexGroupBean.getPerexGroupNameCho() );
        perexGroupsEntity.setPerexGroupNameCz( perexGroupBean.getPerexGroupNameCz() );
        perexGroupsEntity.setPerexGroupNameDe( perexGroupBean.getPerexGroupNameDe() );
        perexGroupsEntity.setPerexGroupNameEn( perexGroupBean.getPerexGroupNameEn() );
        perexGroupsEntity.setPerexGroupNameEsp( perexGroupBean.getPerexGroupNameEsp() );
        perexGroupsEntity.setPerexGroupNameHu( perexGroupBean.getPerexGroupNameHu() );
        perexGroupsEntity.setPerexGroupNamePl( perexGroupBean.getPerexGroupNamePl() );
        perexGroupsEntity.setPerexGroupNameRu( perexGroupBean.getPerexGroupNameRu() );
        perexGroupsEntity.setPerexGroupNameSk( perexGroupBean.getPerexGroupNameSk() );

        return perexGroupsEntity;
    }

    @Override
    public List<PerexGroupsEntity> perexGroupBeanListToPerexGroupsEntityList(List<PerexGroupBean> perexGroupBeanList) {
        if ( perexGroupBeanList == null ) {
            return null;
        }

        List<PerexGroupsEntity> list = new ArrayList<PerexGroupsEntity>( perexGroupBeanList.size() );
        for ( PerexGroupBean perexGroupBean : perexGroupBeanList ) {
            list.add( perexGroupBeanToPerexGroupsEntity( perexGroupBean ) );
        }

        return list;
    }

    @Override
    public PerexGroupBean perexGroupsEntityToPerexGroupBean(PerexGroupsEntity perexGroupsEntity) {
        if ( perexGroupsEntity == null ) {
            return null;
        }

        PerexGroupBean perexGroupBean = new PerexGroupBean();

        perexGroupBean.setPerexGroupId( longToInt( perexGroupsEntity.getId() ) );
        perexGroupBean.setPerexGroupName( perexGroupsEntity.getPerexGroupName() );
        perexGroupBean.setRelatedPages( perexGroupsEntity.getRelatedPages() );
        perexGroupBean.setAvailableGroups( perexGroupsEntity.getAvailableGroups() );
        perexGroupBean.setPerexGroupNameSk( perexGroupsEntity.getPerexGroupNameSk() );
        perexGroupBean.setPerexGroupNameCz( perexGroupsEntity.getPerexGroupNameCz() );
        perexGroupBean.setPerexGroupNameEn( perexGroupsEntity.getPerexGroupNameEn() );
        perexGroupBean.setPerexGroupNameDe( perexGroupsEntity.getPerexGroupNameDe() );
        perexGroupBean.setPerexGroupNamePl( perexGroupsEntity.getPerexGroupNamePl() );
        perexGroupBean.setPerexGroupNameRu( perexGroupsEntity.getPerexGroupNameRu() );
        perexGroupBean.setPerexGroupNameHu( perexGroupsEntity.getPerexGroupNameHu() );
        perexGroupBean.setPerexGroupNameCho( perexGroupsEntity.getPerexGroupNameCho() );
        perexGroupBean.setPerexGroupNameEsp( perexGroupsEntity.getPerexGroupNameEsp() );
        perexGroupBean.setFieldA( perexGroupsEntity.getFieldA() );
        perexGroupBean.setFieldB( perexGroupsEntity.getFieldB() );
        perexGroupBean.setFieldC( perexGroupsEntity.getFieldC() );
        perexGroupBean.setFieldD( perexGroupsEntity.getFieldD() );
        perexGroupBean.setFieldE( perexGroupsEntity.getFieldE() );
        perexGroupBean.setFieldF( perexGroupsEntity.getFieldF() );

        return perexGroupBean;
    }

    @Override
    public List<PerexGroupBean> perexGroupsEntityListToPerexGroupBeanList(List<PerexGroupsEntity> perexGroupsEntityList) {
        if ( perexGroupsEntityList == null ) {
            return null;
        }

        List<PerexGroupBean> list = new ArrayList<PerexGroupBean>( perexGroupsEntityList.size() );
        for ( PerexGroupsEntity perexGroupsEntity : perexGroupsEntityList ) {
            list.add( perexGroupsEntityToPerexGroupBean( perexGroupsEntity ) );
        }

        return list;
    }
}
