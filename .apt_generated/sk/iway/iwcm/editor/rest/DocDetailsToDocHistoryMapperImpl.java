package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.Generated;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.PerexGroupBean;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-01T15:38:04+0200",
    comments = "version: 1.6.1, compiler: Eclipse JDT (IDE) 3.42.0.v20250325-2231, environment: Java 21.0.6 (Eclipse Adoptium)"
)
public class DocDetailsToDocHistoryMapperImpl implements DocDetailsToDocHistoryMapper {

    @Override
    public DocDetails docHistoryToDocDetails(DocHistory history) {
        if ( history == null ) {
            return null;
        }

        DocDetails docDetails = new DocDetails();

        docDetails.setPublishStart( history.getPublishStart() );
        docDetails.setPublishEnd( history.getPublishEnd() );
        docDetails.setPasswordProtected( history.getPasswordProtected() );
        docDetails.setPerexGroupString( history.getPerexGroupString() );
        docDetails.setEventDate( history.getEventDate() );
        docDetails.setSyncId( history.getSyncId() );
        docDetails.setSyncStatus( history.getSyncStatus() );
        docDetails.setLogonPageDocId( history.getLogonPageDocId() );
        docDetails.setForumCount( history.getForumCount() );
        docDetails.setViewsTotal( history.getViewsTotal() );
        docDetails.setRequireSsl( history.isRequireSsl() );
        docDetails.setEditorFields( history.getEditorFields() );
        docDetails.setFullPath( history.getFullPath() );
        String[] perexGroup = history.getPerexGroup();
        if ( perexGroup != null ) {
            docDetails.setPerexGroup( Arrays.copyOf( perexGroup, perexGroup.length ) );
        }
        Integer[] perexGroups = history.getPerexGroups();
        if ( perexGroups != null ) {
            docDetails.setPerexGroups( Arrays.copyOf( perexGroups, perexGroups.length ) );
        }
        docDetails.setGroupId( history.getGroupId() );
        docDetails.setAuthorId( history.getAuthorId() );
        docDetails.setTempId( history.getTempId() );
        docDetails.setSortPriority( history.getSortPriority() );
        docDetails.setHeaderDocId( history.getHeaderDocId() );
        docDetails.setFooterDocId( history.getFooterDocId() );
        docDetails.setMenuDocId( history.getMenuDocId() );
        docDetails.setRightMenuDocId( history.getRightMenuDocId() );
        docDetails.setTitle( history.getTitle() );
        docDetails.setNavbar( history.getNavbar() );
        docDetails.setVirtualPath( history.getVirtualPath() );
        docDetails.setExternalLink( history.getExternalLink() );
        docDetails.setAvailable( history.isAvailable() );
        docDetails.setSearchable( history.isSearchable() );
        docDetails.setCacheable( history.isCacheable() );
        docDetails.setDisableAfterEnd( history.isDisableAfterEnd() );
        docDetails.setPublishAfterStart( history.isPublishAfterStart() );
        docDetails.setLazyLoaded( history.isLazyLoaded() );
        docDetails.setFieldA( history.getFieldA() );
        docDetails.setFieldB( history.getFieldB() );
        docDetails.setFieldC( history.getFieldC() );
        docDetails.setFieldD( history.getFieldD() );
        docDetails.setFieldE( history.getFieldE() );
        docDetails.setFieldF( history.getFieldF() );
        docDetails.setFieldG( history.getFieldG() );
        docDetails.setFieldH( history.getFieldH() );
        docDetails.setFieldI( history.getFieldI() );
        docDetails.setFieldJ( history.getFieldJ() );
        docDetails.setFieldK( history.getFieldK() );
        docDetails.setFieldL( history.getFieldL() );
        docDetails.setFieldM( history.getFieldM() );
        docDetails.setFieldN( history.getFieldN() );
        docDetails.setFieldO( history.getFieldO() );
        docDetails.setFieldP( history.getFieldP() );
        docDetails.setFieldQ( history.getFieldQ() );
        docDetails.setFieldR( history.getFieldR() );
        docDetails.setFieldS( history.getFieldS() );
        docDetails.setFieldT( history.getFieldT() );
        docDetails.setHtmlHead( history.getHtmlHead() );
        docDetails.setHtmlData( history.getHtmlData() );
        docDetails.setPerexPlace( history.getPerexPlace() );
        docDetails.setPerexImage( history.getPerexImage() );
        docDetails.setAuthorName( history.getAuthorName() );
        docDetails.setDocLink( history.getDocLink() );
        docDetails.setData( history.getData() );
        docDetails.setEventDateString( history.getEventDateString() );
        docDetails.setEventTimeString( history.getEventTimeString() );
        docDetails.setTempName( history.getTempName() );
        docDetails.setPublishEndString( history.getPublishEndString() );
        docDetails.setPublishEndTimeString( history.getPublishEndTimeString() );
        docDetails.setPublishStartString( history.getPublishStartString() );
        docDetails.setPublishStartStringExtra( history.getPublishStartStringExtra() );
        docDetails.setPublishStartTimeString( history.getPublishStartTimeString() );
        docDetails.setSyncDefaultForGroupId( history.getSyncDefaultForGroupId() );
        docDetails.setSyncRemotePath( history.getSyncRemotePath() );
        docDetails.setFileName( history.getFileName() );
        docDetails.setAuthorEmail( history.getAuthorEmail() );
        docDetails.setAuthorPhoto( history.getAuthorPhoto() );
        docDetails.setDateCreated( history.getDateCreated() );
        docDetails.setPublishStartDate( history.getPublishStartDate() );
        docDetails.setPublishEndDate( history.getPublishEndDate() );
        docDetails.setEventDateDate( history.getEventDateDate() );
        docDetails.setDataAsc( history.getDataAsc() );
        docDetails.setUrlInheritGroup( history.getUrlInheritGroup() );
        docDetails.setGenerateUrlFromTitle( history.getGenerateUrlFromTitle() );
        docDetails.setEditorVirtualPath( history.getEditorVirtualPath() );
        docDetails.setTempFieldADocId( history.getTempFieldADocId() );
        docDetails.setTempFieldBDocId( history.getTempFieldBDocId() );
        docDetails.setTempFieldCDocId( history.getTempFieldCDocId() );
        docDetails.setTempFieldDDocId( history.getTempFieldDDocId() );
        docDetails.setShowInMenu( history.isShowInMenu() );
        docDetails.setShowInNavbar( history.getShowInNavbar() );
        docDetails.setShowInSitemap( history.getShowInSitemap() );
        docDetails.setLoggedShowInMenu( history.getLoggedShowInMenu() );
        docDetails.setLoggedShowInNavbar( history.getLoggedShowInNavbar() );
        docDetails.setLoggedShowInSitemap( history.getLoggedShowInSitemap() );
        docDetails.setId( history.getId() );
        docDetails.setDocId( history.getDocId() );
        docDetails.setHistoryActual( history.isHistoryActual() );
        docDetails.setHistoryApproveDate( history.getHistoryApproveDate() );
        docDetails.setHistoryApprovedByName( history.getHistoryApprovedByName() );
        docDetails.setHistoryDisapprovedByName( history.getHistoryDisapprovedByName() );
        docDetails.setHistorySaveDate( history.getHistorySaveDate() );
        docDetails.setHistoryId( history.getHistoryId() );
        docDetails.setHistoryApprovedBy( history.getHistoryApprovedBy() );
        docDetails.setHistoryDisapprovedBy( history.getHistoryDisapprovedBy() );
        if ( docDetails.getPerexGroupsList() != null ) {
            List<PerexGroupBean> list = history.getPerexGroupsList();
            if ( list != null ) {
                docDetails.getPerexGroupsList().addAll( list );
            }
        }

        return docDetails;
    }

    @Override
    public List<DocDetails> docHistoryListToDocDetailsList(List<DocHistory> historyList) {
        if ( historyList == null ) {
            return null;
        }

        List<DocDetails> list = new ArrayList<DocDetails>( historyList.size() );
        for ( DocHistory docHistory : historyList ) {
            list.add( docHistoryToDocDetails( docHistory ) );
        }

        return list;
    }

    @Override
    public DocHistory docDetailsToDocHistory(DocDetails detail) {
        if ( detail == null ) {
            return null;
        }

        DocHistory docHistory = new DocHistory();

        docHistory.setPublishStart( detail.getPublishStart() );
        docHistory.setPublishEnd( detail.getPublishEnd() );
        docHistory.setPasswordProtected( detail.getPasswordProtected() );
        docHistory.setPerexGroupString( detail.getPerexGroupString() );
        docHistory.setEventDate( detail.getEventDate() );
        docHistory.setSyncId( detail.getSyncId() );
        docHistory.setSyncStatus( detail.getSyncStatus() );
        docHistory.setLogonPageDocId( detail.getLogonPageDocId() );
        docHistory.setForumCount( detail.getForumCount() );
        docHistory.setViewsTotal( detail.getViewsTotal() );
        docHistory.setRequireSsl( detail.isRequireSsl() );
        docHistory.setEditorFields( detail.getEditorFields() );
        docHistory.setFullPath( detail.getFullPath() );
        String[] perexGroup = detail.getPerexGroup();
        if ( perexGroup != null ) {
            docHistory.setPerexGroup( Arrays.copyOf( perexGroup, perexGroup.length ) );
        }
        Integer[] perexGroups = detail.getPerexGroups();
        if ( perexGroups != null ) {
            docHistory.setPerexGroups( Arrays.copyOf( perexGroups, perexGroups.length ) );
        }
        docHistory.setGroupId( detail.getGroupId() );
        docHistory.setAuthorId( detail.getAuthorId() );
        docHistory.setTempId( detail.getTempId() );
        docHistory.setSortPriority( detail.getSortPriority() );
        docHistory.setHeaderDocId( detail.getHeaderDocId() );
        docHistory.setFooterDocId( detail.getFooterDocId() );
        docHistory.setMenuDocId( detail.getMenuDocId() );
        docHistory.setRightMenuDocId( detail.getRightMenuDocId() );
        docHistory.setTitle( detail.getTitle() );
        docHistory.setNavbar( detail.getNavbar() );
        docHistory.setVirtualPath( detail.getVirtualPath() );
        docHistory.setExternalLink( detail.getExternalLink() );
        docHistory.setAvailable( detail.isAvailable() );
        docHistory.setSearchable( detail.isSearchable() );
        docHistory.setCacheable( detail.isCacheable() );
        docHistory.setDisableAfterEnd( detail.isDisableAfterEnd() );
        docHistory.setPublishAfterStart( detail.isPublishAfterStart() );
        docHistory.setLazyLoaded( detail.isLazyLoaded() );
        docHistory.setFieldA( detail.getFieldA() );
        docHistory.setFieldB( detail.getFieldB() );
        docHistory.setFieldC( detail.getFieldC() );
        docHistory.setFieldD( detail.getFieldD() );
        docHistory.setFieldE( detail.getFieldE() );
        docHistory.setFieldF( detail.getFieldF() );
        docHistory.setFieldG( detail.getFieldG() );
        docHistory.setFieldH( detail.getFieldH() );
        docHistory.setFieldI( detail.getFieldI() );
        docHistory.setFieldJ( detail.getFieldJ() );
        docHistory.setFieldK( detail.getFieldK() );
        docHistory.setFieldL( detail.getFieldL() );
        docHistory.setFieldM( detail.getFieldM() );
        docHistory.setFieldN( detail.getFieldN() );
        docHistory.setFieldO( detail.getFieldO() );
        docHistory.setFieldP( detail.getFieldP() );
        docHistory.setFieldQ( detail.getFieldQ() );
        docHistory.setFieldR( detail.getFieldR() );
        docHistory.setFieldS( detail.getFieldS() );
        docHistory.setFieldT( detail.getFieldT() );
        docHistory.setHtmlHead( detail.getHtmlHead() );
        docHistory.setHtmlData( detail.getHtmlData() );
        docHistory.setPerexPlace( detail.getPerexPlace() );
        docHistory.setPerexImage( detail.getPerexImage() );
        docHistory.setAuthorName( detail.getAuthorName() );
        docHistory.setDocLink( detail.getDocLink() );
        docHistory.setData( detail.getData() );
        docHistory.setEventDateString( detail.getEventDateString() );
        docHistory.setEventTimeString( detail.getEventTimeString() );
        docHistory.setTempName( detail.getTempName() );
        docHistory.setPublishEndString( detail.getPublishEndString() );
        docHistory.setPublishEndTimeString( detail.getPublishEndTimeString() );
        docHistory.setPublishStartString( detail.getPublishStartString() );
        docHistory.setPublishStartStringExtra( detail.getPublishStartStringExtra() );
        docHistory.setPublishStartTimeString( detail.getPublishStartTimeString() );
        docHistory.setSyncDefaultForGroupId( detail.getSyncDefaultForGroupId() );
        docHistory.setSyncRemotePath( detail.getSyncRemotePath() );
        docHistory.setFileName( detail.getFileName() );
        docHistory.setAuthorEmail( detail.getAuthorEmail() );
        docHistory.setAuthorPhoto( detail.getAuthorPhoto() );
        docHistory.setDateCreated( detail.getDateCreated() );
        docHistory.setPublishStartDate( detail.getPublishStartDate() );
        docHistory.setPublishEndDate( detail.getPublishEndDate() );
        docHistory.setEventDateDate( detail.getEventDateDate() );
        docHistory.setDataAsc( detail.getDataAsc() );
        docHistory.setUrlInheritGroup( detail.getUrlInheritGroup() );
        docHistory.setGenerateUrlFromTitle( detail.getGenerateUrlFromTitle() );
        docHistory.setEditorVirtualPath( detail.getEditorVirtualPath() );
        docHistory.setTempFieldADocId( detail.getTempFieldADocId() );
        docHistory.setTempFieldBDocId( detail.getTempFieldBDocId() );
        docHistory.setTempFieldCDocId( detail.getTempFieldCDocId() );
        docHistory.setTempFieldDDocId( detail.getTempFieldDDocId() );
        docHistory.setShowInMenu( detail.isShowInMenu() );
        docHistory.setShowInNavbar( detail.getShowInNavbar() );
        docHistory.setShowInSitemap( detail.getShowInSitemap() );
        docHistory.setLoggedShowInMenu( detail.getLoggedShowInMenu() );
        docHistory.setLoggedShowInNavbar( detail.getLoggedShowInNavbar() );
        docHistory.setLoggedShowInSitemap( detail.getLoggedShowInSitemap() );
        docHistory.setDocId( detail.getDocId() );
        docHistory.setHistoryId( detail.getHistoryId() );
        docHistory.setHistoryApprovedBy( detail.getHistoryApprovedBy() );
        docHistory.setHistoryDisapprovedBy( detail.getHistoryDisapprovedBy() );
        docHistory.setHistorySaveDate( detail.getHistorySaveDate() );
        docHistory.setHistoryApproveDate( detail.getHistoryApproveDate() );
        docHistory.setHistoryApprovedByName( detail.getHistoryApprovedByName() );
        docHistory.setHistoryDisapprovedByName( detail.getHistoryDisapprovedByName() );
        docHistory.setHistoryActual( detail.isHistoryActual() );
        if ( docHistory.getPerexGroupsList() != null ) {
            List<PerexGroupBean> list = detail.getPerexGroupsList();
            if ( list != null ) {
                docHistory.getPerexGroupsList().addAll( list );
            }
        }

        return docHistory;
    }

    @Override
    public List<DocHistory> docDetailsListToDocHistoryList(List<DocDetails> detailsList) {
        if ( detailsList == null ) {
            return null;
        }

        List<DocHistory> list = new ArrayList<DocHistory>( detailsList.size() );
        for ( DocDetails docDetails : detailsList ) {
            list.add( docDetailsToDocHistory( docDetails ) );
        }

        return list;
    }
}
