package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

public class FolderPropertiesService {
    
    private FolderPropertiesService() {
        // private constructor to hide the implicit public one
    }
    
    public static FolderPropertiesEntity getOneItem(HttpServletRequest request, Identity user, FolderPropertiesRepository repository) {
        FolderPropertiesEntity fpe;

        //We need DIR - required parameter
        String dir = request.getParameter("dir");
        if(Tools.isEmpty(dir) == true)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        dir = dir.replaceAll("\\s+","");

        //Check perms
        if(user.isFolderWritable(dir) == false)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        fpe = repository.findByDirUrl(dir).orElse(null);
        if(fpe == null) {
            //This URL is not in DB
            fpe = new FolderPropertiesEntity();
            fpe.setDirUrl(dir);
        }

        FolderPropertiesEditorFields editorFields = new FolderPropertiesEditorFields();
        editorFields.prepareFolderProperties(fpe, request);
        return fpe;
    }

    public static void indexFolder(String dir, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //If user is not logged, redirect him to loggon
        Identity user = UsersDB.getCurrentUser(request);
        List<ResultBean> indexedFiles = new ArrayList<>();
        response.setContentType("text/html; charset=" + SetCharacterEncodingFilter.getEncoding());
                    
        PrintWriter out = response.getWriter();
        Prop prop = Prop.getInstance(request);
        out.println("<html><body>");
        if (user.isFolderWritable(dir)) {
            if (Tools.isNotEmpty(dir) && dir != null && dir.contains("WEB-INF") == false) {
                //budeme rovno vypisovat ak sa nejedna o hromadne indexovanie
                sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
                for (int i = 0; i < 10; i++) {
                    out.println("                                                                             ");
                }
                out.flush();
                FileIndexer.indexDir(dir, indexedFiles, request, out);
            }
        }
                    
        out.println(prop.getText("findex.done"));
        out.println("</body></html>");
    }

    public static void copyFolderProperties(String srcPath, String newPath, HttpServletRequest request) {
        if(Tools.isEmpty(srcPath) == true || Tools.isEmpty(newPath) == true)
            return;

        Identity loggedUser = UsersDB.getCurrentUser(request);    
        if(loggedUser.isFolderWritable(srcPath) == false) 
            return;

        FolderPropertiesRepository folderPropertiesRepository = Tools.getSpringBean("folderPropertiesRepository", FolderPropertiesRepository.class);
        FolderPropertiesEntity fromEntity = folderPropertiesRepository.findByDirUrl(srcPath).orElse(null);
        FolderPropertiesEntity toEntity = folderPropertiesRepository.findByDirUrl(newPath).orElse(null);

        if(fromEntity != null && toEntity != null) {
            //SWAP values
            toEntity.setLogonDocId(fromEntity.getLogonDocId());
            toEntity.setIndexFullText(fromEntity.isIndexFullText());
            toEntity.setPasswordProtected(fromEntity.getPasswordProtected());

            folderPropertiesRepository.save(toEntity);
        } else if(fromEntity != null && toEntity == null) {
            //Create new entity for new path
            fromEntity.setId(null);
            fromEntity.setDirUrl(newPath);

            folderPropertiesRepository.save(fromEntity);
        } else if(fromEntity == null && toEntity == null) {
            //BOTH null, create empty entity for new path
            toEntity = new FolderPropertiesEntity();
            toEntity.setDirUrl(newPath);
            toEntity.setLogonDocId(-1);

            folderPropertiesRepository.save(toEntity);
        }
    }

    public static void deleteFolderProperties(String path, HttpServletRequest request) {
        if(Tools.isEmpty(path) == true) 
            return;

        Identity loggedUser = UsersDB.getCurrentUser(request);    
        if(loggedUser.isFolderWritable(path) == false) 
            return;

        FolderPropertiesRepository folderPropertiesRepository = Tools.getSpringBean("folderPropertiesRepository", FolderPropertiesRepository.class);
        folderPropertiesRepository.deleteByPathLike(path + "%");
    }
}