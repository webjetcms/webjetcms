package sk.iway.iwcm.system.datatable.editorlocking;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.UserDto;
import sk.iway.iwcm.users.UsersDB;

/**
 * RestCotroller pre prácu s editorLocking záznamami v Chache pamäti
 *
 * Pri editovaní záznamu sa uloží do Chache informácia o tom, kto upravuje aky záznam a v akej tabuľke.
 * Súčasne sa vráti zoznam všetkých ostatných používateľov, ktorý práve edituju ten istý záznam.
 * Po tom čo uživateľ uzavrie editor záznamu, jeho akcia sa vymaže z Cache pamäte.
 */
@RestController
@RequestMapping("/admin/rest/editorlocking")
@ResponseBody
public class EditorLockingRestController {

    //nazov cachce objektu v pamäti
    private static final String CACHE_PREFIX = "editor.locking";
    //dlžka exspiracie v minutach, pre dany cache objekt
    private static final int CACHE_EXPIRE_MINUTES = 2;

    /**
	 * Ulozi novy zaznam (EditorLockingBean) o tom kto prave edituje aky zazanm a v akej tabulke
     * do Chache objektu v pamäti.
	 *
	 * @param entityId
	 * @param tableUniqueId
     * @param request
	 * @return zoznam všetkych použivateľov (List<UserDto>), ktory prave edituju ten isty zaznam
	 */
    @GetMapping({ "/open/{entityId}/{tableUniqueId}" })
    public List<UserDto> addEdit(
        @PathVariable("entityId") int entityId,
        @PathVariable("tableUniqueId") String tableUniqueId,
        HttpServletRequest request) {

            Identity user = UsersDB.getCurrentUser(request);
            if (user == null) return new ArrayList<>();
            int userId = user.getUserId();

            List<EditorLockingBean> editorLockingBeanList = getCacheList(tableUniqueId);

            boolean actionExist = false;
            //Zisti, či už existuje zaznam o tom, že aktualny použivateľ upravuje tento zaznam v tejto tabuľke
            for(EditorLockingBean editAction : editorLockingBeanList) {
                if(editAction.getEntityId() == entityId &&
                    editAction.getUserId() == userId) {
                        actionExist = true;
                        //Aktualizuj poslednu zmenu (musi byť aktualizovana PRED odstranenim editActions, ktore exspirovali)
                        editAction.setLastChange(new Date());
                        break;
                    }
            }

            //Tento editActions zaznam nie je v liste, takže ho pridaj
            if(!actionExist) {
                //Pridaj novo vytvoreny zaznam (EditorLockingBean) do listu
                editorLockingBeanList.add(getNewEditRecord(entityId, userId));
            }

            //Odstran editActions, ktore exspirovali
            removeExpiredEditActions(editorLockingBeanList);

            //Vrať list ostatnych použivateľov, ktory upravuju rovnaky zaznam v rovnakej tabuľke (ako aktualny poživateľ)
            return getListOfOtherUsers(editorLockingBeanList, entityId, userId);
    }

    /**
	 * Vymaže z Cache objektu v pamäti zaznam o tom, že daný použivateľ edituje
     * konkretny zaznam v konkretnej tabuľke.
	 *
	 * @param entityId
	 * @param tableUniqueId
     * @param request
	 * @return
	 */
    @GetMapping({ "/close/{entityId}/{tableUniqueId}" })
    public void removeEdit(
        @PathVariable("entityId") int entityId,
        @PathVariable("tableUniqueId") String tableUniqueId,
        HttpServletRequest request) {

        List<EditorLockingBean> editorLockingBeanList = getCacheList(tableUniqueId);

        Identity user = UsersDB.getCurrentUser(request);
        if (user == null) return;
        int userId = user.getUserId();

        Iterator<EditorLockingBean> i = editorLockingBeanList.iterator();
        while (i.hasNext()) {
            EditorLockingBean next = i.next(); //Musi to byt zavolane pre akciou i.remove()

            if(next.getEntityId() == entityId && next.getUserId()==userId) {
                i.remove();
                break;
            }
        }
    }

    /**
	 * Vytvor a nastav novy EditorLockingBean objekt podla vstupnych parametrov.
	 *
	 * @param entityId
     * @param request
	 * @return vytvoreny a nastaveny objekt (EditorLockingBean)
	 */
    private EditorLockingBean getNewEditRecord(int entityId, int userId) {
        EditorLockingBean newEdit = new EditorLockingBean();
        newEdit.setEntityId(entityId);
        newEdit.setLastChange(new Date());
        newEdit.setUserId(userId);

        return newEdit;
    }

    /**
	 * Prejdi zadany list (List<EditorLockingBean>), a dostran z neho EditorLockingBean objekty, ktore už exspirovali.
	 *
	 * @param entityId
	 * @param tableUniqueId
     * @param request
	 * @return list (List<EditorLockingBean>) s objektami, ktore ešte neexspirovali
	 */
    private void removeExpiredEditActions(List<EditorLockingBean> editorLockingBeanList) {
        //Aktualny datum a čas v millisekundach
        long now = new Date().getTime();

        Iterator<EditorLockingBean> i = editorLockingBeanList.iterator();
        while (i.hasNext()) {
            //Musi to byt zavolane pre akciou i.remove()
            EditorLockingBean editAction = i.next();

            //Dokedy validne
            long validUntil = editAction.getLastChange().getTime() + (60000 * CACHE_EXPIRE_MINUTES);

            //Ak editAction exspirovala, odstraň ju
            if(now >= validUntil) {
                i.remove();
            }
        }
    }

    /**
	 * Prejdi zadany list (List<EditorLockingBean>), a zisti kto každy upravuje ten isty zaznam v tej istej tabulke
	 *
	 * @param entityId
     * @param userId
	 * @return list (List<UserDto>) s ostatnymi pouzivatelmi ktory tento zaznam prave upravuju
	 */
    private List<UserDto> getListOfOtherUsers(List<EditorLockingBean> editorLockingBeanList, int entityId, int userId) {

        List<UserDto> otherUsers = new ArrayList<>();

        for(EditorLockingBean editAction : editorLockingBeanList) {
            if(editAction.getEntityId() == entityId && editAction.getUserId() != userId) {
                UserDto otherUser = new UserDto(UsersDB.getUser(editAction.getUserId()));
                otherUsers.add(otherUser);
            }
        }

        return otherUsers;
    }

    /**
     * Vrati z cache list EditorLockingBean pre zadane meno tabulky
     * @param tableUniqueId
     * @return
     */
    private List<EditorLockingBean> getCacheList(String tableUniqueId) {
        Cache cache = Cache.getInstance();
        String cacheKey = CACHE_PREFIX+"-"+tableUniqueId;

        //Ak editorlocking cacheBean už existuje, ziskaj lit zaznamov uprav (List<EditorLockingBean>)
        @SuppressWarnings("unchecked")
        List<EditorLockingBean> editorLockingBeanList = (List<EditorLockingBean>) cache.getObject(cacheKey);
        if (editorLockingBeanList == null) {
            editorLockingBeanList = new ArrayList<>();
            cache.setObject(cacheKey, editorLockingBeanList, CACHE_EXPIRE_MINUTES+5);
        } else {
            //musime ho v cache obnovit, aby neexspiroval
            cache.setObjectExpiryTime(cacheKey, Tools.getNow() + ((CACHE_EXPIRE_MINUTES+5)*60*1000));
        }
        return editorLockingBeanList;
    }
}
