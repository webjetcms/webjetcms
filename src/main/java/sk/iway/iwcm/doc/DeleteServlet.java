package sk.iway.iwcm.doc;


import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.system.spring.SpringUrlMapping;

@WebServlet(name = "DelDoc",
        urlPatterns = {"/admin/docdel.do"}
)
public class DeleteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.println(DeleteServlet.class,"DeleteServlet  CALLED - GET");
        execute(request,response); //NOSONAR
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Logger.println(DeleteServlet.class,"DeleteServlet  CALLED - POST");
        execute(request,response); //NOSONAR
    }

    public void execute(
                                 HttpServletRequest request,
                                 HttpServletResponse response)
            throws IOException, ServletException
    {

        try
        {
            String result = deleteDoc(request, -1);
            if("logon_admin".equals(result)){
                SpringUrlMapping.redirectToLogon(response);
                return ;
            }
            else if("success".equals(result))
            {
                String redirect = Tools.sanitizeHttpHeaderParam(request.getParameter("returl"));
                if(Tools.isNotEmpty(redirect))
                {
                    response.sendRedirect(redirect);
                    return;
                }
                //request.getRequestDispatcher("success").forward(request,response);
                response.sendRedirect("/admin/webpages/");
                return;
            }
            else if("error_admin".equals(result)){
                request.getRequestDispatcher("/admin/error.jsp").forward(request,response);

                return ;
            }

        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }


        response.sendRedirect("/admin/webpages/");
    }

    /**
     * Zmaze / presunie web stranku do kosa
     * @param request - moze byt NULL pre API volanie (vyzaduje ale korektne nastaveny RequestBean, neda sa teda volat z CRON ulohy)
     * @param delDocId -> ak je rovne -1 tak sa pouzije docId z requestu, v opacnom pripade pouzije prislusnu hodnotu delDocId
     * @return
     * @throws IOException
     * @throws ServletException
     */
    public static String deleteDoc(HttpServletRequest request, int delDocId)
    {
        return deleteDoc(request, delDocId, true);
    }

    /**
     * Zmaze / presunie web stranku do kosa
     * @param request - moze byt NULL pre API volanie (vyzaduje ale korektne nastaveny RequestBean, neda sa teda volat z CRON ulohy)
     * @param delDocId -> ak je rovne -1 tak sa pouzije docId z requestu, v opacnom pripade pouzije prislusnu hodnotu delDocId
     * @param publishEvents - ak je true, su vyvolane udalosti (false potrebne ak napr. reagujeme na udalost a potrebujeme znova upravit adresar a nechceme aby doslo k zacykleniu)
     * @return
     * @throws IOException
     * @throws ServletException
     */
    public static String deleteDoc(HttpServletRequest request, int delDocId, boolean publishEvents)
    {
        DocDetails doc = DocDB.getInstance().getDoc(delDocId);
        if (doc != null) {
            EditorService editorService = Tools.getSpringBean("editorService", EditorService.class);
            boolean deleted = editorService.deleteWebpage(doc, publishEvents);
            if (deleted) return "success";
        }
        return "error_admin";
    }

}
