package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/**
 * Praca s mediami vo web stranke
 */
@Service
@RequestScope
public class MediaService {

    private MediaRepository mediaRepo;
    private HttpServletRequest request;
    private Identity user;

    public MediaService(MediaRepository mediaRepo, HttpServletRequest request) {
        this.mediaRepo = mediaRepo;
        this.request = request;
        this.user = UsersDB.getCurrentUser(request);
    }

    /**
     * V novo vytvorenej stranke sa media ukladaju s hodnotou -user_id, po ulozeni je potrebne im nastavit doc_id hodnotu
     * @param docId
     */
	public void assignDocIdToMedia(int docId) {
		if (docId > 0) {
			//find media with media_fk_table_name = "documents_temp" and media_fk_id = userId
			MediaDB mediaDB = new MediaDB();
			List<Media> mediaList = MediaDB.getMedia(request.getSession(), "documents_temp", user.getUserId(), null, 0, false);
			//ak som nasiel take media, tak im priradim spravne media_fk_id a media_fk_table_name
			if (mediaList != null && mediaList.isEmpty()==false) {
				for (Media media : mediaList) {
					try {
						media.setMediaFkId(Integer.valueOf(docId));
						media.setMediaFkTableName("documents");
						mediaDB.save(media);
					} catch (Exception e) {
						Logger.println(MediaService.class, "editor.save.mediaError");
					}
				}
			}
		}
	}

    /**
     * Zmaze v databaze docasne media (ulozene ako -user_id) ak novo vytvorena stranka nebola ulozena
     */
	public void deleteTempMedia(){
        try {
            MediaDB mediaDB = new MediaDB();
            List<Media> mediaList = MediaDB.getMedia(request.getSession(), "documents_temp", user.getUserId(), null, 0, false);

            // ak som nasiel take media, tak im priradim spravne media_fk_id a
            // media_fk_table_name
            if (mediaList != null && mediaList.isEmpty()==false) {
                for (Media media : mediaList) {
                    mediaDB.delete(media);
                }
            }
        } catch (Exception e) {
            Logger.println(MediaService.class, "ERROR: Nastal problem pri mazani docasne ulozenych medii");
        }
	}

    /**
     * Pri premenovani URL adresy aktualizuje zaznamy v databaze na novu hodnotu
     * @param oldLinkURL
     * @param newLinkURL
     * @param domain
     */
    public void updateMediaLink(String oldLinkURL, String newLinkURL, String domain) {
        DocDB docDB = DocDB.getInstance();
        if(domain != null) {
            List<Long> mediasIdsAll = mediaRepo.findMediaIds(oldLinkURL, "documents");
            List<Long> mediasIdsFiltered = new ArrayList<>();

            //ponechaj len tie, v aktualnej domene
            for(Long id : mediasIdsAll) {
                String docDomain = docDB.getDomain(id.intValue());
                if (docDomain.equals(domain)) {
                    mediasIdsFiltered.add(id);
                }
            }

            if(mediasIdsFiltered.isEmpty() == false) {
                mediaRepo.updateMedia(newLinkURL, oldLinkURL, mediasIdsFiltered);
            }
        } else {
            mediaRepo.updateMedia(newLinkURL, oldLinkURL);
        }
    }

    public List<Media> getAllMedia(Integer mediaFkId, String mediaFkTableName) {
        return mediaRepo.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(mediaFkId, mediaFkTableName, CloudToolsForCore.getDomainId());
    }
}
